package ar.edu.um.ingenieria.limitador.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;

import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;

/**
 * Configuración e inicialización de lectores singleton para bases de datos MaxMind GeoIP2.
 * Utiliza {@link CHMCache} (ConcurrentHashMap) para optimizar consultas repetidas
 * y gestiona el ciclo de vida cerrando los recursos con {@code destroyMethod = "close"}.
 */
@Configuration
@Log4j2 
@AllArgsConstructor 
public class GeoIpConfig {

    private final ResourceLoader resourceLoader;

    @Bean(name = "cityDatabaseReader", destroyMethod = "close")
    public DatabaseReader cityDatabaseReader(
            @Value("${geoip.database.city-path:docs/GeoLite2-City.mmdb}") String path,
            @Value("${geoip.database.cache-size:4096}") int cacheCapacity) throws IOException {
        log.info("Inicializando bean singleton GeoLite2-City desde '{}' con capacidad de caché {}", path, cacheCapacity);
        return buildDatabaseReader(path, cacheCapacity);
    }

    @Bean(name = "countryDatabaseReader", destroyMethod = "close")
    public DatabaseReader countryDatabaseReader(
            @Value("${geoip.database.country-path:docs/GeoLite2-Country.mmdb}") String path,
            @Value("${geoip.database.cache-size:4096}") int cacheCapacity) throws IOException {
        log.info("Inicializando bean singleton GeoLite2-Country desde '{}' con capacidad de caché {}", path, cacheCapacity);
        return buildDatabaseReader(path, cacheCapacity);
    }

    @Bean(name = "asnDatabaseReader", destroyMethod = "close")
    public DatabaseReader asnDatabaseReader(
            @Value("${geoip.database.asn-path:docs/GeoLite2-ASN.mmdb}") String path,
            @Value("${geoip.database.cache-size:4096}") int cacheCapacity) throws IOException {
        log.info("Inicializando bean singleton GeoLite2-ASN desde '{}' con capacidad de caché {}", path, cacheCapacity);
        return buildDatabaseReader(path, cacheCapacity);
    }

    private DatabaseReader buildDatabaseReader(String path, int cacheCapacity) throws IOException {
        File file = new File(path);
        if (file.exists() && file.isFile()) {
            return new DatabaseReader.Builder(file)
                    .withCache(new CHMCache(cacheCapacity))
                    .build();
        }

        Resource resource = resourceLoader.getResource(path);
        if (resource.exists()) {
            try {
                return new DatabaseReader.Builder(resource.getFile())
                        .withCache(new CHMCache(cacheCapacity))
                        .build();
            } catch (IOException ex) {
                return new DatabaseReader.Builder(resource.getInputStream())
                        .withCache(new CHMCache(cacheCapacity))
                        .build();
            }
        }

        throw new FileNotFoundException("No se encontró el archivo de base de datos GeoIP en la ruta configurada: " + path);
    }
}
