package ar.edu.um.ingenieria.limitador.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * DTO inmutable que representa la información geográfica y de red (ASN)
 * asociada a una dirección IP pública.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record GeoLocationResponse(
    String ip,
    String country,
    String isoCode,
    String city,
    String postalCode,
    CoordinatesDTO coordinates,
    AsnDTO asn
) {
    public record CoordinatesDTO(
        Double latitude,
        Double longitude
    ) {}

    public record AsnDTO(
        Long autonomousSystemNumber,
        String autonomousSystemOrganization
    ) {}
}
