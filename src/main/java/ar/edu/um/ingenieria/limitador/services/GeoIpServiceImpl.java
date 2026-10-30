package ar.edu.um.ingenieria.limitador.services;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.exception.AddressNotFoundException;
import com.maxmind.geoip2.exception.GeoIp2Exception;
import com.maxmind.geoip2.model.AsnResponse;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.model.CountryResponse;

import ar.edu.um.ingenieria.limitador.dto.GeoLocationResponse;
import ar.edu.um.ingenieria.limitador.exception.IpNotFoundException;
import ar.edu.um.ingenieria.limitador.validator.IpValidator;

/**
 * Implementación eficiente del servicio de geolocalización utilizando
 * lectores Singleton de MaxMind GeoIP2 y validación estricta de direcciones IP.
 */
@Service
public class GeoIpServiceImpl implements GeoIpService {

    private static final Logger log = LoggerFactory.getLogger(GeoIpServiceImpl.class);

    private final DatabaseReader cityReader;
    private final DatabaseReader countryReader;
    private final DatabaseReader asnReader;
    private final IpValidator ipValidator;

    public GeoIpServiceImpl(
            @Qualifier("cityDatabaseReader") DatabaseReader cityReader,
            @Qualifier("countryDatabaseReader") DatabaseReader countryReader,
            @Qualifier("asnDatabaseReader") DatabaseReader asnReader,
            IpValidator ipValidator) {
        this.cityReader = cityReader;
        this.countryReader = countryReader;
        this.asnReader = asnReader;
        this.ipValidator = ipValidator;
    }

    @Override
    public GeoLocationResponse getIpLocation(String ipAddress) {
        InetAddress validatedAddress = ipValidator.validateAndParse(ipAddress);

        GeoData geoData = resolveGeoData(validatedAddress);
        GeoLocationResponse.AsnDTO asnData = resolveAsnData(validatedAddress);

        if (geoData.isEmpty() && asnData == null) {
            throw new IpNotFoundException("No se encontró información geográfica para la dirección IP: " + ipAddress);
        }

        return new GeoLocationResponse(
                ipAddress,
                geoData.country(),
                geoData.isoCode(),
                geoData.city(),
                geoData.postalCode(),
                geoData.coordinates(),
                asnData
        );
    }

    private GeoData resolveGeoData(InetAddress address) {
        return findInCityDatabase(address)
                .or(() -> findInCountryDatabase(address))
                .orElse(GeoData.empty());
    }

    private Optional<GeoData> findInCityDatabase(InetAddress address) {
        try {
            CityResponse response = cityReader.city(address);
            GeoLocationResponse.CoordinatesDTO coordinates = extractCoordinates(response);
            return Optional.of(new GeoData(
                    extractCountryName(response),
                    extractCountryIsoCode(response),
                    extractCityName(response),
                    extractPostalCode(response),
                    coordinates
            ));
        } catch (AddressNotFoundException e) {
            log.debug("IP {} no encontrada en GeoLite2-City, procediendo a respaldo con GeoLite2-Country", address);
            return Optional.empty();
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error al consultar GeoLite2-City para IP: {}", address, e);
            throw new IllegalStateException("Error al leer la base de datos de City", e);
        }
    }

    private Optional<GeoData> findInCountryDatabase(InetAddress address) {
        try {
            CountryResponse response = countryReader.country(address);
            String countryName = response.getCountry() != null ? response.getCountry().getName() : null;
            String isoCode = response.getCountry() != null ? response.getCountry().getIsoCode() : null;
            return Optional.of(new GeoData(countryName, isoCode, null, null, null));
        } catch (AddressNotFoundException e) {
            log.debug("IP {} no encontrada en GeoLite2-Country", address);
            return Optional.empty();
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error al consultar GeoLite2-Country para IP: {}", address, e);
            throw new IllegalStateException("Error al leer la base de datos de Country", e);
        }
    }

    private GeoLocationResponse.AsnDTO resolveAsnData(InetAddress address) {
        try {
            AsnResponse response = asnReader.asn(address);
            return new GeoLocationResponse.AsnDTO(
                    response.getAutonomousSystemNumber(),
                    response.getAutonomousSystemOrganization()
            );
        } catch (AddressNotFoundException e) {
            log.debug("IP {} no encontrada en GeoLite2-ASN", address);
            return null;
        } catch (IOException | GeoIp2Exception e) {
            log.error("Error al consultar GeoLite2-ASN para IP: {}", address, e);
            throw new IllegalStateException("Error al leer la base de datos de ASN", e);
        }
    }

    private GeoLocationResponse.CoordinatesDTO extractCoordinates(CityResponse response) {
        if (response.getLocation() != null 
                && response.getLocation().getLatitude() != null 
                && response.getLocation().getLongitude() != null) {
            return new GeoLocationResponse.CoordinatesDTO(
                    response.getLocation().getLatitude(),
                    response.getLocation().getLongitude()
            );
        }
        return null;
    }

    private String extractCountryName(CityResponse response) {
        return response.getCountry() != null ? response.getCountry().getName() : null;
    }

    private String extractCountryIsoCode(CityResponse response) {
        return response.getCountry() != null ? response.getCountry().getIsoCode() : null;
    }

    private String extractCityName(CityResponse response) {
        return response.getCity() != null ? response.getCity().getName() : null;
    }

    private String extractPostalCode(CityResponse response) {
        return response.getPostal() != null ? response.getPostal().getCode() : null;
    }

    private record GeoData(
            String country,
            String isoCode,
            String city,
            String postalCode,
            GeoLocationResponse.CoordinatesDTO coordinates
    ) {
        static GeoData empty() {
            return new GeoData(null, null, null, null, null);
        }

        boolean isEmpty() {
            return country == null && isoCode == null && city == null && postalCode == null && coordinates == null;
        }
    }
}
