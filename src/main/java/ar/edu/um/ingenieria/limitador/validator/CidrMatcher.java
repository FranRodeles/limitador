package ar.edu.um.ingenieria.limitador.validator;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.util.regex.Pattern;

import org.apache.commons.net.util.SubnetUtils;
import org.springframework.stereotype.Component;

import ar.edu.um.ingenieria.limitador.exception.InvalidIpException;

/**
 * Componente encargado de validar direcciones IPv4/IPv6 y evaluar pertenencia a rangos CIDR.
 * Utiliza {@link SubnetUtils} de Apache Commons Net para rangos IPv4 y máscaras de bits sobre
 * {@link Inet6Address} para rangos IPv6.
 */
@Component
public class CidrMatcher {

    private static final Pattern STRICT_IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$"
    );

    private static final int IPV4_MAX_PREFIX = 32;
    private static final int IPV6_MAX_PREFIX = 128;

    /**
     * Valida una cadena que representa una dirección IP individual (IPv4 o IPv6, pública o privada)
     * y devuelve su representación normalizada.
     */
    public String normalizeIp(String rawIp) {
        InetAddress parsed = parseStrictIpAddress(rawIp);
        if (parsed instanceof Inet4Address) {
            return rawIp.trim();
        }
        return parsed.getHostAddress();
    }

    /**
     * Indica si la cadena provista corresponde a una dirección IPv4 o IPv6 literal válida.
     */
    public boolean isValidIp(String rawIp) {
        if (rawIp == null || rawIp.isBlank()) {
            return false;
        }
        try {
            parseStrictIpAddress(rawIp);
            return true;
        } catch (InvalidIpException ex) {
            return false;
        }
    }

    /**
     * Valida y normaliza una dirección IP o rango CIDR.
     * Si se recibe una IP sin máscara, se normaliza a {@code /32} (IPv4) o {@code /128} (IPv6).
     */
    public String normalizeCidrOrIp(String rawCidrOrIp) {
        if (rawCidrOrIp == null || rawCidrOrIp.isBlank()) {
            throw new InvalidIpException("La regla IP/CIDR no puede estar vacía.");
        }

        String trimmed = rawCidrOrIp.trim();
        String[] parts = trimmed.split("/", -1);
        if (parts.length > 2) {
            throw new InvalidIpException("Formato CIDR inválido: " + rawCidrOrIp);
        }

        InetAddress address = parseStrictIpAddress(parts[0]);
        boolean isIpv4 = address instanceof Inet4Address;
        int maxPrefix = isIpv4 ? IPV4_MAX_PREFIX : IPV6_MAX_PREFIX;

        int prefixLength = maxPrefix;
        if (parts.length == 2) {
            prefixLength = parsePrefixLength(parts[1], maxPrefix, rawCidrOrIp);
        }

        String normalizedAddress = isIpv4 ? parts[0].trim() : trimmed.split("/")[0].trim();
        String normalizedCidr = normalizedAddress + "/" + prefixLength;

        if (isIpv4 && prefixLength > 0) {
            validateWithApacheCommonsSubnetUtils(normalizedCidr);
        }

        return normalizedCidr;
    }

    /**
     * Evalúa si {@code clientIp} pertenece a la regla {@code cidrOrIpRule} (IP individual o CIDR).
     */
    public boolean matches(String clientIp, String cidrOrIpRule) {
        if (!isValidIp(clientIp) || cidrOrIpRule == null || cidrOrIpRule.isBlank()) {
            return false;
        }

        String normalizedRule = normalizeCidrOrIp(cidrOrIpRule);
        String[] parts = normalizedRule.split("/");
        InetAddress clientAddr = parseStrictIpAddress(clientIp);
        InetAddress ruleAddr = parseStrictIpAddress(parts[0]);
        int prefixLength = Integer.parseInt(parts[1]);

        if (clientAddr instanceof Inet4Address && ruleAddr instanceof Inet4Address) {
            if (prefixLength == 0) {
                return true;
            }
            SubnetUtils subnetUtils = new SubnetUtils(normalizedRule);
            subnetUtils.setInclusiveHostCount(true);
            return subnetUtils.getInfo().isInRange(clientIp.trim());
        }

        if (clientAddr instanceof Inet6Address && ruleAddr instanceof Inet6Address) {
            return matchesIpv6Prefix(clientAddr.getAddress(), ruleAddr.getAddress(), prefixLength);
        }

        return false;
    }

    private void validateWithApacheCommonsSubnetUtils(String ipv4Cidr) {
        try {
            SubnetUtils subnetUtils = new SubnetUtils(ipv4Cidr);
            subnetUtils.setInclusiveHostCount(true);
        } catch (IllegalArgumentException ex) {
            throw new InvalidIpException("Rango CIDR IPv4 inválido: " + ipv4Cidr);
        }
    }

    private boolean matchesIpv6Prefix(byte[] clientBytes, byte[] ruleBytes, int prefixLength) {
        if (prefixLength == 0) {
            return true;
        }
        int fullBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;

        for (int i = 0; i < fullBytes; i++) {
            if (clientBytes[i] != ruleBytes[i]) {
                return false;
            }
        }

        if (remainingBits > 0) {
            int mask = (0xFF << (8 - remainingBits)) & 0xFF;
            if ((clientBytes[fullBytes] & mask) != (ruleBytes[fullBytes] & mask)) {
                return false;
            }
        }

        return true;
    }

    private InetAddress parseStrictIpAddress(String rawIp) {
        if (rawIp == null || rawIp.isBlank()) {
            throw new InvalidIpException("La dirección IP no puede estar vacía.");
        }

        String trimmed = rawIp.trim();
        if (trimmed.contains(".")) {
            if (!STRICT_IPV4_PATTERN.matcher(trimmed).matches()) {
                throw new InvalidIpException("La dirección IP proporcionada es inválida: " + rawIp);
            }
        }

        try {
            return InetAddress.ofLiteral(trimmed);
        } catch (IllegalArgumentException ex) {
            throw new InvalidIpException("La dirección IP proporcionada es inválida: " + rawIp);
        }
    }

    private int parsePrefixLength(String rawPrefix, int maxPrefix, String fullInput) {
        if (rawPrefix == null || rawPrefix.isBlank()) {
            throw new InvalidIpException("Prefijo CIDR vacío en: " + fullInput);
        }
        try {
            int prefix = Integer.parseInt(rawPrefix.trim());
            if (prefix < 0 || prefix > maxPrefix) {
                throw new InvalidIpException("Prefijo CIDR fuera de rango (0-" + maxPrefix + "): " + fullInput);
            }
            return prefix;
        } catch (NumberFormatException ex) {
            throw new InvalidIpException("Prefijo CIDR no numérico en: " + fullInput);
        }
    }
}
