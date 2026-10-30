package ar.edu.um.ingenieria.limitador.controllers;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ar.edu.um.ingenieria.limitador.config.SecurityConfig;
import ar.edu.um.ingenieria.limitador.dto.GeoLocationResponse;
import ar.edu.um.ingenieria.limitador.exception.InvalidIpException;
import ar.edu.um.ingenieria.limitador.exception.IpNotFoundException;
import ar.edu.um.ingenieria.limitador.exception.PrivateIpAddressException;
import ar.edu.um.ingenieria.limitador.services.GeoIpService;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(GeoIpController.class)
@Import(GlobalExceptionHandler.class)
@DisplayName("GeoIpController Specification")
class GeoIpControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GeoIpService geoIpService;

    @Test
    @DisplayName("GET /api/v1/geoip/{ip} returns 200 OK with geolocation payload")
    void shouldReturnGeoLocationForValidIp() throws Exception {
        String ip = "8.8.8.8";
        GeoLocationResponse response = new GeoLocationResponse(
                ip,
                "United States",
                "US",
                "Mountain View",
                "94043",
                new GeoLocationResponse.CoordinatesDTO(37.4223, -122.0847),
                new GeoLocationResponse.AsnDTO(15169L, "GOOGLE")
        );

        when(geoIpService.getIpLocation(ip)).thenReturn(response);

        mockMvc.perform(get("/api/v1/geoip/{ip}", ip)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ip").value(ip))
                .andExpect(jsonPath("$.country").value("United States"))
                .andExpect(jsonPath("$.isoCode").value("US"))
                .andExpect(jsonPath("$.city").value("Mountain View"))
                .andExpect(jsonPath("$.postalCode").value("94043"))
                .andExpect(jsonPath("$.coordinates.latitude").value(37.4223))
                .andExpect(jsonPath("$.coordinates.longitude").value(-122.0847))
                .andExpect(jsonPath("$.asn.autonomousSystemNumber").value(15169))
                .andExpect(jsonPath("$.asn.autonomousSystemOrganization").value("GOOGLE"));
    }

    @Test
    @DisplayName("GET /api/v1/geoip/{ip} returns 400 Bad Request when IP is private or local")
    void shouldReturn400WhenIpIsPrivate() throws Exception {
        String privateIp = "127.0.0.1";
        when(geoIpService.getIpLocation(privateIp))
                .thenThrow(new PrivateIpAddressException("La dirección IP '127.0.0.1' es privada o local y no posee geolocalización pública."));

        mockMvc.perform(get("/api/v1/geoip/{ip}", privateIp)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Private IP Address"))
                .andExpect(jsonPath("$.detail").value("La dirección IP '127.0.0.1' es privada o local y no posee geolocalización pública."));
    }

    @Test
    @DisplayName("GET /api/v1/geoip/{ip} returns 400 Bad Request when IP format is invalid")
    void shouldReturn400WhenIpIsInvalid() throws Exception {
        String invalidIp = "invalid-ip";
        when(geoIpService.getIpLocation(invalidIp))
                .thenThrow(new InvalidIpException("La dirección IP proporcionada es inválida: invalid-ip"));

        mockMvc.perform(get("/api/v1/geoip/{ip}", invalidIp)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid IP Address"))
                .andExpect(jsonPath("$.detail").value("La dirección IP proporcionada es inválida: invalid-ip"));
    }

    @Test
    @DisplayName("GET /api/v1/geoip/{ip} returns 404 Not Found when IP does not exist in databases")
    void shouldReturn404WhenIpNotFound() throws Exception {
        String unknownIp = "198.51.100.1";
        when(geoIpService.getIpLocation(unknownIp))
                .thenThrow(new IpNotFoundException("No se encontró información geográfica para la dirección IP: 198.51.100.1"));

        mockMvc.perform(get("/api/v1/geoip/{ip}", unknownIp)
                        .accept(MediaType.APPLICATION_PROBLEM_JSON, MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("IP Address Not Found"))
                .andExpect(jsonPath("$.detail").value("No se encontró información geográfica para la dirección IP: 198.51.100.1"));
    }
}
