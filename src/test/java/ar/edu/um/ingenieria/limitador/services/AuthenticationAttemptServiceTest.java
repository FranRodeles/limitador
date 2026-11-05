package ar.edu.um.ingenieria.limitador.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttempt;
import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttemptStatus;
import ar.edu.um.ingenieria.limitador.domain.IpAddress;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.GeoLocationResponse;
import ar.edu.um.ingenieria.limitador.exception.PrivateIpAddressException;
import ar.edu.um.ingenieria.limitador.repository.AuthenticationAttemptRepository;
import ar.edu.um.ingenieria.limitador.repository.IpAddressRepository;
import ar.edu.um.ingenieria.limitador.validator.CidrMatcher;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthenticationAttemptService Specification")
class AuthenticationAttemptServiceTest {

    @Mock
    private AuthenticationAttemptRepository authenticationAttemptRepository;

    @Mock
    private IpAddressRepository ipAddressRepository;

    @Mock
    private GeoIpService geoIpService;

    private CidrMatcher cidrMatcher;
    private AuthenticationAttemptServiceImpl attemptService;

    @BeforeEach
    void setUp() {
        cidrMatcher = new CidrMatcher();
        attemptService = new AuthenticationAttemptServiceImpl(
            authenticationAttemptRepository,
            ipAddressRepository,
            geoIpService,
            cidrMatcher
        );
    }

    @Test
    @DisplayName("Should enrich new public IP with City, Country, and ASN from existing GeoIpService")
    void shouldEnrichPublicIpWithGeoIpServiceData() {
        String publicIp = "8.8.8.8";
        GeoLocationResponse geoResponse = new GeoLocationResponse(
            publicIp,
            "United States",
            "US",
            "Mountain View",
            "94043",
            new GeoLocationResponse.CoordinatesDTO(37.386, -122.0838),
            new GeoLocationResponse.AsnDTO(15169L, "GOOGLE")
        );

        when(ipAddressRepository.findByIp(publicIp)).thenReturn(Optional.empty());
        when(geoIpService.getIpLocation(publicIp)).thenReturn(geoResponse);
        when(ipAddressRepository.save(any(IpAddress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(authenticationAttemptRepository.save(any(AuthenticationAttempt.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        User user = new User();
        user.setId(5L);
        user.setUsername("johndoe");

        AuthenticationAttempt attempt = attemptService.recordAttempt(
            publicIp,
            "johndoe",
            user,
            true,
            AuthenticationAttemptStatus.SUCCESS,
            null
        );

        assertThat(attempt.getSuccessful()).isTrue();
        assertThat(attempt.getUserIdentified()).isTrue();
        assertThat(attempt.getUser()).isEqualTo(user);
        assertThat(attempt.getIpAddress().getIp()).isEqualTo("8.8.8.8");
        assertThat(attempt.getIpAddress().getCountry()).isEqualTo("United States");
        assertThat(attempt.getIpAddress().getCountryIsoCode()).isEqualTo("US");
        assertThat(attempt.getIpAddress().getCity()).isEqualTo("Mountain View");
        assertThat(attempt.getIpAddress().getAsn()).isEqualTo(15169L);
        assertThat(attempt.getIpAddress().getAsnOrganization()).isEqualTo("GOOGLE");
        verify(geoIpService).getIpLocation(publicIp);
    }

    @Test
    @DisplayName("Should record attempt for non-existent user with userIdentified=false and handle private IP gracefully")
    void shouldRecordUnidentifiedUserAndPrivateIpGracefully() {
        String privateIp = "192.168.1.50";
        when(ipAddressRepository.findByIp(privateIp)).thenReturn(Optional.empty());
        when(geoIpService.getIpLocation(privateIp))
            .thenThrow(new PrivateIpAddressException("Private IP"));
        when(ipAddressRepository.save(any(IpAddress.class))).thenAnswer(inv -> inv.getArgument(0));
        when(authenticationAttemptRepository.save(any(AuthenticationAttempt.class)))
            .thenAnswer(inv -> inv.getArgument(0));

        AuthenticationAttempt attempt = attemptService.recordAttempt(
            privateIp,
            "ghost_user",
            null,
            false,
            AuthenticationAttemptStatus.FAILURE_USER_NOT_FOUND,
            "El usuario no existe; intento no asociado a una cuenta registrada"
        );

        assertThat(attempt.getSuccessful()).isFalse();
        assertThat(attempt.getUser()).isNull();
        assertThat(attempt.getUserIdentified()).isFalse();
        assertThat(attempt.getAttemptedUsername()).isEqualTo("ghost_user");
        assertThat(attempt.getStatus()).isEqualTo(AuthenticationAttemptStatus.FAILURE_USER_NOT_FOUND);
        assertThat(attempt.getIpAddress().getIp()).isEqualTo("192.168.1.50");
        assertThat(attempt.getIpAddress().getCity()).isNull();
        assertThat(attempt.getIpAddress().getCountry()).isNull();
        assertThat(attempt.getIpAddress().getAsn()).isNull();
    }
}
