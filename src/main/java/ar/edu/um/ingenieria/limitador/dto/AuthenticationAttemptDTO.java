package ar.edu.um.ingenieria.limitador.dto;

import java.io.Serializable;
import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import ar.edu.um.ingenieria.limitador.domain.AuthenticationAttemptStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthenticationAttemptDTO implements Serializable {
    private Long id;
    private Long userId;
    private String attemptedUsername;
    private Boolean userIdentified;
    private String ip;
    private String city;
    private String country;
    private String countryIsoCode;
    private Long asn;
    private String asnOrganization;
    private Instant attemptedAt;
    private Boolean successful;
    private AuthenticationAttemptStatus status;
    private String rejectionReason;
}
