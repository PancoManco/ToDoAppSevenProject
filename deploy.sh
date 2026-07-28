#!/usr/bin/env bash

set -Eeuo pipefail

COMPOSE_FILE="${COMPOSE_FILE:-docker-compose-prod.yml}"
PROJECT_DIR="${PROJECT_DIR:-/opt/todo-app}"
ENV_FILE="${ENV_FILE:-.env}"

ALL_APP_SERVICES=(
  auth-service
  task-service
  email-sender
  scheduler
  api-gateway
  frontend
)

declare -A DEPENDENT_SERVICES=(
  ["auth-service"]="task-service api-gateway"
  ["task-service"]="scheduler api-gateway"
  ["frontend"]="nginx"
  ["api-gateway"]="nginx"
)

log() {
  printf '%s\n' "$*"
}

fail() {
  log "❌ $*"
  exit 1
}

contains_service() {
  local expected="$1"
  shift
  for service in "$@"; do
    [[ "$service" == "$expected" ]] && return 0
  done
  return 1
}

validate_service() {
  local requested="$1"
  for valid in "${ALL_APP_SERVICES[@]}"; do
    [[ "$requested" == "$valid" ]] && return 0
  done
  fail "Неизвестный сервис: $requested"
}

wait_for_healthy() {
  local service="$1"
  local max_attempts="${2:-30}"
  local delay_seconds="${3:-5}"

  local container_id
  container_id="$(docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps -q "$service" 2>/dev/null || echo "")"

  if [[ -z "$container_id" ]]; then
    log "⚠️  Контейнер $service не найден (возможно не настроен depends_on)"
    return 0
  fi

  local attempt
  for ((attempt = 1; attempt <= max_attempts; attempt++)); do
    local state health
    state="$(docker inspect --format '{{.State.Status}}' "$container_id" 2>/dev/null || echo "unknown")"
    health="$(docker inspect --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' "$container_id" 2>/dev/null || echo "none")"

    if [[ "$state" == "running" ]] && [[ "$health" == "healthy" || "$health" == "none" ]]; then
      log "✅ $service запущен: state=$state, health=$health"
      return 0
    fi

    if [[ "$state" == "exited" || "$state" == "dead" ]]; then
      log "❌ $service завершился: state=$state"
      docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --tail=50 "$service" || true
      return 1
    fi

    log "⏳ Ожидание $service: state=$state, health=$health ($attempt/$max_attempts)"
    sleep "$delay_seconds"
  done

  log "❌ $service не стал healthy за $((max_attempts * delay_seconds)) секунд"
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" logs --tail=50 "$service" || true
  return 1
}

rollback_service() {
  local service="$1"
  local previous_tag="${DOCKERHUB_USERNAME}/${service}:previous"

  if docker image inspect "$previous_tag" >/dev/null 2>&1; then
    log "🔄 Откатываем $service к previous..."
    docker tag "$previous_tag" "${DOCKERHUB_USERNAME}/${service}:latest"
    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --force-recreate "$service"
    wait_for_healthy "$service" 20 5
  else
    log "⚠️  Образ previous не найден, откат невозможен"
  fi
}


trap 'log "❌ Ошибка деплоя на строке $LINENO"; notify "error" "Script failed at line $LINENO"' ERR

cd "$PROJECT_DIR"

[[ -f "$COMPOSE_FILE" ]] || fail "Не найден $PROJECT_DIR/$COMPOSE_FILE"
[[ -f "$ENV_FILE" ]] || fail "Не найден $PROJECT_DIR/$ENV_FILE"

set -a
source "$ENV_FILE"
set +a

command -v docker >/dev/null 2>&1 || fail "Docker не установлен"
docker compose version >/dev/null 2>&1 || fail "Docker Compose plugin не установлен"
: "${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME не задан}"

export DOCKERHUB_USERNAME

REQUESTED_SERVICES=("$@")

if [[ ${#REQUESTED_SERVICES[@]} -eq 0 ]]; then
  log "ℹ️ Список сервисов не передан. Запускаем весь стек."

  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" config --quiet
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --remove-orphans

  SERVICES_TO_CHECK=("${ALL_APP_SERVICES[@]}")
else
  for service in "${REQUESTED_SERVICES[@]}"; do
    validate_service "$service"
  done

  log "🚀 Деплоим: ${REQUESTED_SERVICES[*]}"

  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" config --quiet


  for service in "${REQUESTED_SERVICES[@]}"; do
    current_image="${DOCKERHUB_USERNAME}/${service}:latest"
    if docker image inspect "$current_image" >/dev/null 2>&1; then
      docker tag "$current_image" "${DOCKERHUB_USERNAME}/${service}:previous"
      log "💾 Сохранён previous образ для $service"
    fi
  done

  log "📥 Скачиваем новые образы"
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" pull "${REQUESTED_SERVICES[@]}"

  log "♻️ Пересоздаём изменённые контейнеры"
  docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --force-recreate "${REQUESTED_SERVICES[@]}"

  SERVICES_TO_CHECK=("${REQUESTED_SERVICES[@]}")


  ALL_DEPENDENTS=()
  for service in "${REQUESTED_SERVICES[@]}"; do
    if [[ -n "${DEPENDENT_SERVICES[$service]:-}" ]]; then
      for dep in ${DEPENDENT_SERVICES[$service]}; do
        if ! contains_service "$dep" "${SERVICES_TO_CHECK[@]}" "${ALL_DEPENDENTS[@]}"; then
          ALL_DEPENDENTS+=("$dep")
        fi
      done
    fi
  done


  log "🔎 Проверяем основные сервисы"
  FAILED_SERVICES=()
  for service in "${SERVICES_TO_CHECK[@]}"; do
    if ! wait_for_healthy "$service"; then
      FAILED_SERVICES+=("$service")
    fi
  done


  if [[ ${#FAILED_SERVICES[@]} -gt 0 ]]; then
    log "❌ Сервисы упали: ${FAILED_SERVICES[*]}"
    notify "error" "Services failed: ${FAILED_SERVICES[*]}. Attempting rollback..."

    for service in "${FAILED_SERVICES[@]}"; do
      rollback_service "$service"
    done

    notify "warning" "Rollback performed for: ${FAILED_SERVICES[*]}"
    exit 1
  fi

  if [[ ${#ALL_DEPENDENTS[@]} -gt 0 ]]; then
    log "🔄 Перезапускаем зависимые сервисы: ${ALL_DEPENDENTS[*]}"

    docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up -d --force-recreate "${ALL_DEPENDENTS[@]}"

    for dep in "${ALL_DEPENDENTS[@]}"; do
      if ! wait_for_healthy "$dep" 20 5; then
        log "⚠️  Зависимый сервис $dep не стал healthy"
        notify "warning" "Dependent service $dep is not healthy after restart"
      fi
    done

    SERVICES_TO_CHECK+=("${ALL_DEPENDENTS[@]}")
  fi
fi

log "📋 Финальное состояние"
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps

log "🧹 Удаляем неиспользуемые Docker-образы"
docker image prune -f

log "✅ Деплой успешно завершён"
notify "success" "Deployment completed successfully"