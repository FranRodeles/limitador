package ar.edu.um.ingenieria.limitador.security;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;

import ar.edu.um.ingenieria.limitador.validator.CidrMatcher;

/**
 * Componente responsable de obtener y validar la dirección IP real del cliente.
 * Solo procesa cabeceras {@code X-Forwarded-For} y {@code X-Real-IP} cuando la petición
 * proviene de un proxy confiable configurado, evaluando la cadena de derecha a izquierda
 * para prevenir ataques de falsificación de cabeceras (IP Spoofing).
 */
@Component
public class ClientIpResolver {

    private static final String DEFAULT_LOCAL_IP = "127.0.0.1";
    private static final String X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String X_REAL_IP = "X-Real-IP";

    private final CidrMatcher cidrMatcher;
    private final List<String> trustedProxyCidrs;

    public ClientIpResolver(
            CidrMatcher cidrMatcher,
            @Value("${security.trusted-proxies:127.0.0.1/32,::1/128,10.0.0.0/8,172.16.0.0/12,192.168.0.0/16}")
            List<String> trustedProxies) {
        this.cidrMatcher = cidrMatcher;
        this.trustedProxyCidrs = trustedProxies != null
            ? trustedProxies.stream()
                .filter(p -> p != null && !p.isBlank())
                .map(String::trim)
                .toList()
            : List.of();
    }

    /**
     * Resuelve la IP del cliente a partir del contexto HTTP actual o devuelve {@code 127.0.0.1}
     * cuando se invoca fuera de un contexto web (por ejemplo, en pruebas unitarias de servicios).
     */
    public String resolveCurrentClientIp() {
        RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
        if (attributes instanceof ServletRequestAttributes servletAttributes) {
            return resolveClientIp(servletAttributes.getRequest());
        }
        return DEFAULT_LOCAL_IP;
    }

    /**
     * Extrae y valida la dirección IP real del cliente para el {@link HttpServletRequest} dado.
     */
    public String resolveClientIp(HttpServletRequest request) {
        if (request == null) {
            return DEFAULT_LOCAL_IP;
        }

        String remoteAddr = request.getRemoteAddr();
        if (!cidrMatcher.isValidIp(remoteAddr)) {
            return DEFAULT_LOCAL_IP;
        }

        String normalizedRemote = cidrMatcher.normalizeIp(remoteAddr);
        if (!isTrustedProxy(normalizedRemote)) {
            return normalizedRemote;
        }

        String xForwardedFor = request.getHeader(X_FORWARDED_FOR);
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            String resolvedFromXff = extractFromXForwardedFor(xForwardedFor);
            if (resolvedFromXff != null) {
                return resolvedFromXff;
            }
        }

        String xRealIp = request.getHeader(X_REAL_IP);
        if (xRealIp != null && !xRealIp.isBlank() && cidrMatcher.isValidIp(xRealIp)) {
            return cidrMatcher.normalizeIp(xRealIp);
        }

        return normalizedRemote;
    }

    private String extractFromXForwardedFor(String xForwardedFor) {
        String[] rawHops = xForwardedFor.split(",");
        List<String> validHops = new ArrayList<>();

        for (String hop : rawHops) {
            String candidate = hop.trim();
            if (!cidrMatcher.isValidIp(candidate)) {
                // Si hay una IP malformada o manipulada en la cabecera, descartamos ese salto
                continue;
            }
            validHops.add(cidrMatcher.normalizeIp(candidate));
        }

        if (validHops.isEmpty()) {
            return null;
        }

        // Recorrido de derecha a izquierda: el primer salto no perteneciente a un proxy confiable
        // es la dirección real que se conectó a nuestra infraestructura de borde.
        for (int i = validHops.size() - 1; i >= 0; i--) {
            String hopIp = validHops.get(i);
            if (!isTrustedProxy(hopIp)) {
                return hopIp;
            }
        }

        // Si todos los saltos pertenecen a redes internas confiables, el cliente es el primer salto.
        return validHops.get(0);
    }

    private boolean isTrustedProxy(String ipAddress) {
        for (String proxyCidr : trustedProxyCidrs) {
            if (cidrMatcher.matches(ipAddress, proxyCidr)) {
                return true;
            }
        }
        return false;
    }
}
