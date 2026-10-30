package ar.edu.um.ingenieria.limitador.config;

import java.io.File;
import java.io.IOException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import com.maxmind.geoip2.DatabaseReader;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeoIpConfig Specification")
class GeoIpConfigTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withUserConfiguration(GeoIpConfig.class)
            .withPropertyValues(
                    "geoip.database.city-path=docs/GeoLite2-City.mmdb",
                    "geoip.database.country-path=docs/GeoLite2-Country.mmdb",
                    "geoip.database.asn-path=docs/GeoLite2-ASN.mmdb",
                    "geoip.database.cache-size=1024"
            );

    @Test
    @DisplayName("should configure singleton DatabaseReader beans for City, Country and ASN with CHMCache")
    void shouldConfigureDatabaseReaderBeans() {
        contextRunner.run(context -> {
            assertThat(context).hasBean("cityDatabaseReader");
            assertThat(context).hasBean("countryDatabaseReader");
            assertThat(context).hasBean("asnDatabaseReader");

            DatabaseReader cityReader = context.getBean("cityDatabaseReader", DatabaseReader.class);
            DatabaseReader countryReader = context.getBean("countryDatabaseReader", DatabaseReader.class);
            DatabaseReader asnReader = context.getBean("asnDatabaseReader", DatabaseReader.class);

            assertThat(cityReader).isNotNull();
            assertThat(countryReader).isNotNull();
            assertThat(asnReader).isNotNull();

            // Verify they are distinct singleton instances
            assertThat(cityReader).isNotSameAs(countryReader);
            assertThat(cityReader).isNotSameAs(asnReader);
        });
    }
}
