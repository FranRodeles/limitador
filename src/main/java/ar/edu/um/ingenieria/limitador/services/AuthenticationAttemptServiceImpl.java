package ar.edu.um.ingenieria.limitador.services;

import java.time.Instant;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttempt;
import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttemptStatus;
import ar.edu.um.ingenieria.limitador.domain.IpAddress;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.AuthenticationAttemptDTO;
import ar.edu.um.ingenieria.limitador.dto.GeoLocationResponse;
import ar.edu.um.ingenieria.limitador.repository.AuthenticationAttemptRepository;
import ar.edu.um.ingenieria.limitador.repository.IpAddressRepository;
import ar.edu.um.ingenieria.limitador.validator.CidrMatcher;

@Service
public class AuthenticationAttemptServiceImpl implements AuthenticationAttemptService {

    private static final Logger log = LoggerFactory.getLogger(AuthenticationAttemptServiceImpl.class);
    private static final String FALLBACK_IP = "127.0.0.1";

    private final AuthenticationAttemptRepository authenticationAttemptRepository;
    private final IpAddressRepository ipAddressRepository;
    private final GeoIpService geoIpService;
    private final CidrMatcher cidrMatcher;

    public AuthenticationAttemptServiceImpl(
            AuthenticationAttemptRepository authenticationAttemptRepository,
            IpAddressRepository ipAddressRepository,
            GeoIpService geoIpService,
            CidrMatcher cidrMatcher) {
        this.authenticationAttemptRepository = authenticationAttemptRepository;
        this.ipAddressRepository = ipAddressRepository;
        this.geoIpService = geoIpService;
        this.cidrMatcher = cidrMatcher;
    }

    @Override
    public IpAddress resolveOrCreateIpAddress(String rawClientIp) {
        String normalizedIp = normalizeSafeIp(rawClientIp);
        Instant now = Instant.now();

        return ipAddressRepository.findByIp(normalizedIp)
            .map(existing -> {
                existing.setLastSeenAt(now);
                return ipAddressRepository.save(existing);
            })
            .orElseGet(() -> createNewIpAddress(normalizedIp, now));
    }

    @Override
    public AuthenticationAttempt recordAttempt(
            String clientIp,
            String attemptedUsername,
            User identifiedUser,
            boolean successful,
            AuthenticationAttemptStatus status,
            String rejectionReason) {
        IpAddress ipAddress = resolveOrCreateIpAddress(clientIp);
        String safeUsername = (attemptedUsername != null && !attemptedUsername.isBlank())
            ? attemptedUsername.trim()
            : "<unknown>";

        AuthenticationAttempt attempt = AuthenticationAttempt.builder()
            .user(identifiedUser)
            .attemptedUsername(safeUsername)
            .userIdentified(identifiedUser != null)
            .ipAddress(ipAddress)
            .attemptedAt(Instant.now())
            .successful(successful)
            .status(status)
            .rejectionReason(rejectionReason)
            .build();

        return authenticationAttemptRepository.save(attempt);
    }

    @Override
    public List<AuthenticationAttempt> findAll() {
        return authenticationAttemptRepository.findAll();
    }

    @Override
    public List<AuthenticationAttempt> findByUsername(String username) {
        if (username == null || username.isBlank()) {
            return List.of();
        }
        return authenticationAttemptRepository.findByAttemptedUsernameOrderByAttemptedAtDesc(username.trim());
    }

    @Override
    public List<AuthenticationAttempt> findByIp(String ip) {
        if (ip == null || ip.isBlank()) {
            return List.of();
        }
        String normalized = cidrMatcher.isValidIp(ip) ? cidrMatcher.normalizeIp(ip) : ip.trim();
        return authenticationAttemptRepository.findByIpAddress_IpOrderByAttemptedAtDesc(normalized);
    }

    @Override
    public List<IpAddress> findAllRegisteredIps() {
        return ipAddressRepository.findAll();
    }

    @Override
    public AuthenticationAttemptDTO toDTO(AuthenticationAttempt attempt) {
        if (attempt == null) {
            return null;
        }
        IpAddress ipAddr = attempt.getIpAddress();
        return AuthenticationAttemptDTO.builder()
            .id(attempt.getId())
            .userId(attempt.getUser() != null ? attempt.getUser().getId() : null)
            .attemptedUsername(attempt.getAttemptedUsername())
            .userIdentified(attempt.getUserIdentified())
            .ip(ipAddr != null ? ipAddr.getIp() : null)
            .city(ipAddr != null ? ipAddr.getCity() : null)
            .country(ipAddr != null ? ipAddr.getCountry() : null)
            .countryIsoCode(ipAddr != null ? ipAddr.getCountryIsoCode() : null)
            .asn(ipAddr != null ? ipAddr.getAsn() : null)
            .asnOrganization(ipAddr != null ? ipAddr.getAsnOrganization() : null)
            .attemptedAt(attempt.getAttemptedAt())
            .successful(attempt.getSuccessful())
            .status(attempt.getStatus())
            .rejectionReason(attempt.getRejectionReason())
            .build();
    }

    private IpAddress createNewIpAddress(String normalizedIp, Instant now) {
        IpAddress ipEntity = IpAddress.builder()
            .ip(normalizedIp)
            .firstSeenAt(now)
            .lastSeenAt(now)
            .build();

        enrichWithGeoLocation(ipEntity, normalizedIp);

        try {
            return ipAddressRepository.save(ipEntity);
        } catch (DataIntegrityViolationException ex) {
            // Manejo de concurrencia: si otro hilo insertó la misma IP simultáneamente,
            // recuperamos el registro recién confirmado.
            return ipAddressRepository.findByIp(normalizedIp)
                .orElseThrow(() -> ex);
        }
    }

    private void enrichWithGeoLocation(IpAddress ipEntity, String normalizedIp) {
        if (geoIpService == null) {
            return;
        }
        try {
            GeoLocationResponse geo = geoIpService.getIpLocation(normalizedIp);
            if (geo != null) {
                ipEntity.setCountry(geo.country());
                ipEntity.setCountryIsoCode(geo.isoCode());
                ipEntity.setCity(geo.city());
                if (geo.asn() != null) {
                    ipEntity.setAsn(geo.asn().autonomousSystemNumber());
                    ipEntity.setAsnOrganization(geo.asn().autonomousSystemOrganization());
                }
            }
        } catch (RuntimeException ex) {
            log.debug("No se pudo obtener geolocalización pública para la IP '{}': {}", normalizedIp, ex.getMessage());
        }
    }

    private String normalizeSafeIp(String rawClientIp) {
        if (rawClientIp != null && cidrMatcher.isValidIp(rawClientIp)) {
            return cidrMatcher.normalizeIp(rawClientIp);
        }
        return FALLBACK_IP;
    }
}
