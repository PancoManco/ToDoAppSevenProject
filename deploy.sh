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

  local service
  for service in "$@"; do
    if [[ "$service" == "$expected" ]]; then
      return 0
    fi
  done

  return 1
}

validate_service() {
  local requested="$1"
  local valid

  for valid in "${ALL_APP_SERVICES[@]}"; do
    if [[ "$requested" == "$valid" ]]; then
      return 0
    fi
  done

  fail "Неизвестный сервис: $requested"
}

wait_for_healthy() {
  local service="$1"
  local max_attempts="${2:-30}"
  local delay_seconds="${3:-5}"

  local container_id
  container_id="$(
    docker compose \
      --env-file "$ENV_FILE" \
      -f "$COMPOSE_FILE" \
      ps -q "$service"
  )"

  if [[ -z "$container_id" ]]; then
    fail "Контейнер сервиса $service не найден"
  fi

  local attempt
  for ((attempt = 1; attempt <= max_attempts; attempt++)); do
    local state
    local health

    state="$(
      docker inspect \
        --format '{{.State.Status}}' \
        "$container_id"
    )"

    health="$(
      docker inspect \
        --format '{{if .State.Health}}{{.State.Health.Status}}{{else}}none{{end}}' \
        "$container_id"
    )"

    if [[ "$state" == "running" ]] \
      && [[ "$health" == "healthy" || "$health" == "none" ]]; then

      log "✅ $service запущен: state=$state, health=$health"
      return 0
    fi

    if [[ "$state" == "exited" || "$state" == "dead" ]]; then
      log "❌ $service завершился: state=$state"

      docker compose \
        --env-file "$ENV_FILE" \
        -f "$COMPOSE_FILE" \
        logs --tail=100 "$service" || true

      return 1
    fi

    log "⏳ Ожидание $service: state=$state, health=$health ($attempt/$max_attempts)"
    sleep "$delay_seconds"

    container_id="$(
      docker compose \
        --env-file "$ENV_FILE" \
        -f "$COMPOSE_FILE" \
        ps -q "$service"
    )"
  done

  log "❌ $service не стал healthy вовремя"

  docker compose \
    --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" \
    logs --tail=100 "$service" || true

  return 1
}

trap 'log "❌ Ошибка деплоя на строке $LINENO"' ERR

cd "$PROJECT_DIR"

[[ -f "$COMPOSE_FILE" ]] \
  || fail "Не найден $PROJECT_DIR/$COMPOSE_FILE"

[[ -f "$ENV_FILE" ]] \
  || fail "Не найден $PROJECT_DIR/$ENV_FILE"

set -a
source "$ENV_FILE"
set +a

command -v docker >/dev/null 2>&1 \
  || fail "Docker не установлен"

docker compose version >/dev/null 2>&1 \
  || fail "Docker Compose plugin не установлен"

: "${DOCKERHUB_USERNAME:?DOCKERHUB_USERNAME не задан}"

export DOCKERHUB_USERNAME

REQUESTED_SERVICES=("$@")

if [[ ${#REQUESTED_SERVICES[@]} -eq 0 ]]; then
  log "ℹ️ Список сервисов не передан."
  log "ℹ️ Обновляем compose-конфигурацию и запускаем весь стек."

  docker compose \
    --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" \
    config --quiet

  docker compose \
    --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" \
    pull

  docker compose \
    --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" \
    up -d --remove-orphans

  SERVICES_TO_CHECK=("${ALL_APP_SERVICES[@]}")
else
  for service in "${REQUESTED_SERVICES[@]}"; do
    validate_service "$service"
  done

  log "🚀 Деплоим: ${REQUESTED_SERVICES[*]}"

  docker compose \
    --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" \
    config --quiet

  log "📥 Скачиваем новые образы"

  docker compose \
    --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" \
    pull "${REQUESTED_SERVICES[@]}"

  log "♻️ Пересоздаём изменённые контейнеры"

  docker compose \
    --env-file "$ENV_FILE" \
    -f "$COMPOSE_FILE" \
    up -d \
    --no-deps \
    --force-recreate \
    "${REQUESTED_SERVICES[@]}"

  SERVICES_TO_CHECK=("${REQUESTED_SERVICES[@]}")

  if contains_service "frontend" "${REQUESTED_SERVICES[@]}" \
    || contains_service "api-gateway" "${REQUESTED_SERVICES[@]}"; then

    log "♻️ Перезапускаем nginx"

    docker compose \
      --env-file "$ENV_FILE" \
      -f "$COMPOSE_FILE" \
      up -d \
      --no-deps \
      --force-recreate \
      nginx
  fi

  if contains_service "task-service" "${REQUESTED_SERVICES[@]}" \
    && ! contains_service "scheduler" "${REQUESTED_SERVICES[@]}"; then

    log "♻️ Перезапускаем scheduler после обновления task-service"

    docker compose \
      --env-file "$ENV_FILE" \
      -f "$COMPOSE_FILE" \
      up -d \
      --no-deps \
      --force-recreate \
      scheduler

    SERVICES_TO_CHECK+=("scheduler")
  fi
fi

log "🔎 Проверяем состояние контейнеров"

for service in "${SERVICES_TO_CHECK[@]}"; do
  wait_for_healthy "$service"
done

if contains_service "frontend" "${REQUESTED_SERVICES[@]}" \
  || contains_service "api-gateway" "${REQUESTED_SERVICES[@]}"; then

  wait_for_healthy "nginx"
fi

log "📋 Текущее состояние"

docker compose \
  --env-file "$ENV_FILE" \
  -f "$COMPOSE_FILE" \
  ps

log "🧹 Удаляем неиспользуемые Docker-образы"

docker image prune -f

log "✅ Деплой успешно завершён"