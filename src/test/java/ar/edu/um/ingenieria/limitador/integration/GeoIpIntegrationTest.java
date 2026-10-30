package ar.edu.um.ingenieria.limitador.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("GeoIP Real Database End-to-End Integration Test")
class GeoIpIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("should resolve real IP 8.8.8.8 from local MaxMind MMDB files")
    void shouldResolveGoogleDnsIp() throws Exception {
        mockMvc.perform(get("/api/v1/geoip/8.8.8.8")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ip").value("8.8.8.8"))
                .andExpect(jsonPath("$.country").value("United States"))
                .andExpect(jsonPath("$.isoCode").value("US"))
                .andExpect(jsonPath("$.coordinates.latitude").value(notNullValue()))
                .andExpect(jsonPath("$.coordinates.longitude").value(notNullValue()))
                .andExpect(jsonPath("$.asn.autonomousSystemNumber").value(15169))
                .andExpect(jsonPath("$.asn.autonomousSystemOrganization").value("Google LLC"));
    }

    @Test
    @DisplayName("should resolve real IP 1.1.1.1 (Cloudflare Anycast) with ASN data from local MaxMind MMDB files")
    void shouldResolveCloudflareDnsIp() throws Exception {
        mockMvc.perform(get("/api/v1/geoip/1.1.1.1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ip").value("1.1.1.1"))
                .andExpect(jsonPath("$.asn.autonomousSystemNumber").value(13335))
                .andExpect(jsonPath("$.asn.autonomousSystemOrganization").value("Cloudflare, Inc."));
    }

    @Test
    @DisplayName("should reject loopback IP 127.0.0.1 with 400 Bad Request ProblemDetail")
    void shouldRejectLoopbackAddress() throws Exception {
        mockMvc.perform(get("/api/v1/geoip/127.0.0.1")
                        .accept(MediaType.APPLICATION_PROBLEM_JSON, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Private IP Address"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("should reject malformed IP with 400 Bad Request ProblemDetail")
    void shouldRejectMalformedIpAddress() throws Exception {
        mockMvc.perform(get("/api/v1/geoip/not-a-valid-ip")
                        .accept(MediaType.APPLICATION_PROBLEM_JSON, MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid IP Address"))
                .andExpect(jsonPath("$.status").value(400));
    }
}
