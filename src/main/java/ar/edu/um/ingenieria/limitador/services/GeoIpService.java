package ar.edu.um.ingenieria.limitador.services;

import ar.edu.um.ingenieria.limitador.dto.GeoLocationResponse;
import ar.edu.um.ingenieria.limitador.exception.InvalidIpException;
import ar.edu.um.ingenieria.limitador.exception.IpNotFoundException;
import ar.edu.um.ingenieria.limitador.exception.PrivateIpAddressException;

/**
 * Contrato del servicio de consulta de geolocalización e información ASN para direcciones IP.
 */
public interface GeoIpService {

    /**
     * Obtiene la información geográfica y de ASN para la dirección IP provista.
     *
     * @param ipAddress Cadena con la dirección IP pública a consultar.
     * @return DTO {@link GeoLocationResponse} con los datos de país, ciudad, coordenadas y ASN.
     * @throws InvalidIpException si la IP tiene formato inválido.
     * @throws PrivateIpAddressException si la IP es local o privada.
     * @throws IpNotFoundException si la IP no se encuentra en las bases de datos de MaxMind.
     */
    GeoLocationResponse getIpLocation(String ipAddress);
}
