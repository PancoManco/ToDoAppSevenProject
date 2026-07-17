package ru.pancomanco.authservice.config;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class ClientIpResolver {
    public String resolve(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");

        if (StringUtils.hasText(forwardedFor)) {
            String ip = forwardedFor.split(",")[0].trim();
            log.debug("Resolved IP from X-Forwarded-For: {}", ip);
            return ip;
        }

        String realIp = request.getHeader("X-Real-IP");

        if (StringUtils.hasText(realIp)) {
            log.debug("Resolved IP from X-Real-IP: {}", realIp);
            return realIp.trim();
        }
        String remoteAddr = request.getRemoteAddr();
        log.debug("Fallback to RemoteAddr: {}", remoteAddr);
        return remoteAddr;
    }
}
