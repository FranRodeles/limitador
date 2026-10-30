package ar.edu.um.ingenieria.limitador.services;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;
import com.maxmind.geoip2.record.City;
import com.maxmind.geoip2.record.Country;
import com.maxmind.geoip2.record.Location;
import com.maxmind.geoip2.record.Postal;

import ar.edu.um.ingenieria.limitador.dto.GeoLocationResponse;
import ar.edu.um.ingenieria.limitador.exception.InvalidIpException;
import ar.edu.um.ingenieria.limitador.exception.IpNotFoundException;
import ar.edu.um.ingenieria.limitador.validator.IpValidator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GeoIpService Specification")
class GeoIpServiceTest {

    @Mock
    private DatabaseReader cityReader;

    @Mock
    private DatabaseReader countryReader;

    @Mock
    private DatabaseReader asnReader;

    private IpValidator ipValidator;
    private GeoIpService geoIpService;

    @BeforeEach
    void setUp() {
        ipValidator = new IpValidator();
        geoIpService = new GeoIpServiceImpl(cityReader, countryReader, asnReader, ipValidator);
    }

    @Test
    @DisplayName("should return full geolocation and ASN data when IP exists in both readers")
    void shouldReturnFullGeolocationAndAsnData() throws IOException, GeoIp2Exception {
        String ip = "8.8.8.8";
        InetAddress inetAddress = InetAddress.ofLiteral(ip);

        // Mock City response
        CityResponse cityResponse = mock(CityResponse.class);
        Country country = mock(Country.class);
        City city = mock(City.class);
        Postal postal = mock(Postal.class);
        Location location = mock(Location.class);

        when(country.getName()).thenReturn("United States");
        when(country.getIsoCode()).thenReturn("US");
        when(city.getName()).thenReturn("Mountain View");
        when(postal.getCode()).thenReturn("94043");
        when(location.getLatitude()).thenReturn(37.4223);
        when(location.getLongitude()).thenReturn(-122.0847);

        when(cityResponse.getCountry()).thenReturn(country);
        when(cityResponse.getCity()).thenReturn(city);
        when(cityResponse.getPostal()).thenReturn(postal);
        when(cityResponse.getLocation()).thenReturn(location);

        when(cityReader.city(inetAddress)).thenReturn(cityResponse);

        // Mock ASN response
        AsnResponse asnResponse = mock(AsnResponse.class);
        when(asnResponse.getAutonomousSystemNumber()).thenReturn(15169L);
        when(asnResponse.getAutonomousSystemOrganization()).thenReturn("GOOGLE");
        when(asnReader.asn(inetAddress)).thenReturn(asnResponse);

        GeoLocationResponse response = geoIpService.getIpLocation(ip);

        assertThat(response).isNotNull();
        assertThat(response.ip()).isEqualTo(ip);
        assertThat(response.country()).isEqualTo("United States");
        assertThat(response.isoCode()).isEqualTo("US");
        assertThat(response.city()).isEqualTo("Mountain View");
        assertThat(response.postalCode()).isEqualTo("94043");
        assertThat(response.coordinates()).isNotNull();
        assertThat(response.coordinates().latitude()).isEqualTo(37.4223);
        assertThat(response.coordinates().longitude()).isEqualTo(-122.0847);
        assertThat(response.asn()).isNotNull();
        assertThat(response.asn().autonomousSystemNumber()).isEqualTo(15169L);
        assertThat(response.asn().autonomousSystemOrganization()).isEqualTo("GOOGLE");
    }

    @Test
    @DisplayName("should fallback to Country database when City database does not find the address")
    void shouldFallbackToCountryDatabaseWhenCityNotFound() throws IOException, GeoIp2Exception {
        String ip = "1.2.3.4";
        InetAddress inetAddress = InetAddress.ofLiteral(ip);

        when(cityReader.city(inetAddress)).thenThrow(new AddressNotFoundException("Not found in city DB"));

        CountryResponse countryResponse = mock(CountryResponse.class);
        Country country = mock(Country.class);
        when(country.getName()).thenReturn("Australia");
        when(country.getIsoCode()).thenReturn("AU");
        when(countryResponse.getCountry()).thenReturn(country);

        when(countryReader.country(inetAddress)).thenReturn(countryResponse);

        // ASN reader returns empty / throws AddressNotFoundException
        when(asnReader.asn(inetAddress)).thenThrow(new AddressNotFoundException("Not found in ASN DB"));

        GeoLocationResponse response = geoIpService.getIpLocation(ip);

        assertThat(response).isNotNull();
        assertThat(response.ip()).isEqualTo(ip);
        assertThat(response.country()).isEqualTo("Australia");
        assertThat(response.isoCode()).isEqualTo("AU");
        assertThat(response.city()).isNull();
        assertThat(response.postalCode()).isNull();
        assertThat(response.coordinates()).isNull();
        assertThat(response.asn()).isNull();
    }

    @Test
    @DisplayName("should throw IpNotFoundException when address is not found in any database")
    void shouldThrowIpNotFoundExceptionWhenNotInAnyDatabase() throws IOException, GeoIp2Exception {
        String ip = "198.51.100.1"; // TEST-NET-2 documentation IP (not private, but unassigned publicly)
        InetAddress inetAddress = InetAddress.ofLiteral(ip);

        when(cityReader.city(inetAddress)).thenThrow(new AddressNotFoundException("Not found in city DB"));
        when(countryReader.country(inetAddress)).thenThrow(new AddressNotFoundException("Not found in country DB"));
        when(asnReader.asn(inetAddress)).thenThrow(new AddressNotFoundException("Not found in ASN DB"));

        assertThatThrownBy(() -> geoIpService.getIpLocation(ip))
                .isInstanceOf(IpNotFoundException.class)
                .hasMessageContaining("No se encontró información geográfica");
    }

    @Test
    @DisplayName("should throw InvalidIpException and not call databases when IP is malformed")
    void shouldRejectMalformedIpBeforeDatabaseCall() {
        assertThatThrownBy(() -> geoIpService.getIpLocation("invalid-format"))
                .isInstanceOf(InvalidIpException.class);
    }
}
