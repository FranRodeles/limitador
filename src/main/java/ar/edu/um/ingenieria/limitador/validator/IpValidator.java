package ar.edu.um.ingenieria.limitador.validator;

import java.net.InetAddress;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import ar.edu.um.ingenieria.limitador.exception.InvalidIpException;
import ar.edu.um.ingenieria.limitador.exception.PrivateIpAddressException;

/**
 * Validador de direcciones IP responsable de garantizar el formato estricto
 * y filtrar direcciones de red privadas, locales o no enrutables públicamente.
 */
@Component
public class IpValidator {

    private static final Pattern STRICT_IPV4_PATTERN = Pattern.compile(
        "^((25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d\\d|[1-9]?\\d)$"
    );

    /**
     * Valida y analiza una cadena que representa una dirección IP pública.
     *
     * @param ipAddress Dirección IP a validar.
     * @return Instancia de {@link InetAddress} válida y enrutable públicamente.
     * @throws InvalidIpException si la cadena es nula, vacía o no tiene formato de IP válido.
     * @throws PrivateIpAddressException si la IP es privada, de loopback o de enlace local.
     */
    public InetAddress validateAndParse(String ipAddress) {
        if (ipAddress == null || ipAddress.isBlank()) {
            throw new InvalidIpException("La dirección IP no puede estar vacía.");
        }

        String trimmedIp = ipAddress.trim();
        InetAddress parsedAddress = parseStrictAddress(trimmedIp);
        ensurePublicRoutableAddress(parsedAddress, trimmedIp);

        return parsedAddress;
    }

    private InetAddress parseStrictAddress(String ipAddress) {
        if (ipAddress.contains(".")) {
            if (!STRICT_IPV4_PATTERN.matcher(ipAddress).matches()) {
                throw new InvalidIpException("La dirección IP proporcionada es inválida: " + ipAddress);
            }
        }

        try {
            return InetAddress.ofLiteral(ipAddress);
        } catch (IllegalArgumentException ex) {
            throw new InvalidIpException("La dirección IP proporcionada es inválida: " + ipAddress);
        }
    }

    private void ensurePublicRoutableAddress(InetAddress address, String rawIp) {
        if (isPrivateOrLocal(address)) {
            throw new PrivateIpAddressException(
                "La dirección IP '" + rawIp + "' es privada o local y no posee geolocalización pública."
            );
        }
    }

    private boolean isPrivateOrLocal(InetAddress address) {
        return address.isLoopbackAddress()
                || address.isSiteLocalAddress()
                || address.isLinkLocalAddress()
                || address.isAnyLocalAddress()
                || address.isMulticastAddress();
    }
}
