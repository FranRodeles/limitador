package ar.edu.um.ingenieria.limitador.domain;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidad que almacena de forma normalizada cada dirección IP única registrada
 * en el sistema junto con su información geográfica y de ASN provista por MaxMind.
 */
@Entity
@Table(
    name = "ip_addresses",
    indexes = {
        @Index(name = "idx_ip_addresses_ip", columnList = "ip", unique = true)
    }
)
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class IpAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ip", nullable = false, unique = true, length = 45)
    private String ip;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "country", length = 100)
    private String country;

    @Column(name = "country_iso_code", length = 10)
    private String countryIsoCode;

    @Column(name = "asn")
    private Long asn;

    @Column(name = "asn_organization", length = 255)
    private String asnOrganization;

    @Column(name = "first_seen_at", nullable = false)
    private Instant firstSeenAt;

    @Column(name = "last_seen_at", nullable = false)
    private Instant lastSeenAt;
}
