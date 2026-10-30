package ar.edu.um.ingenieria.limitador.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ar.edu.um.ingenieria.limitador.dto.GeoLocationResponse;
import ar.edu.um.ingenieria.limitador.services.GeoIpService;

/**
 * Controlador REST para consultas de geolocalización e información de red basadas en IP.
 */
@RestController
@RequestMapping("/api/v1/geoip")
public class GeoIpController {

    private final GeoIpService geoIpService;

    public GeoIpController(GeoIpService geoIpService) {
        this.geoIpService = geoIpService;
    }

    /**
     * Endpoint REST para consultar la ubicación geográfica y ASN de una dirección IP.
     *
     * @param ip Dirección IP a consultar (IPv4 o IPv6).
     * @return {@link ResponseEntity} con {@link GeoLocationResponse}.
     */
    @GetMapping("/{ip}")
    public ResponseEntity<GeoLocationResponse> getGeoLocation(@PathVariable("ip") String ip) {
        GeoLocationResponse response = geoIpService.getIpLocation(ip);
        return ResponseEntity.ok(response);
    }
}
