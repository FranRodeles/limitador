package ar.edu.um.ingenieria.limitador.services;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;
import ar.edu.um.ingenieria.limitador.domain.BlacklistRuleType;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.BlacklistEvaluationResult;
import ar.edu.um.ingenieria.limitador.dto.BlacklistRuleDTO;
import ar.edu.um.ingenieria.limitador.repository.BlacklistRuleRepository;
import ar.edu.um.ingenieria.limitador.repository.UserRepository;
import ar.edu.um.ingenieria.limitador.validator.CidrMatcher;

@Service
public class BlacklistServiceImpl implements BlacklistService {

    private final BlacklistRuleRepository blacklistRuleRepository;
    private final UserRepository userRepository;
    private final CidrMatcher cidrMatcher;

    public BlacklistServiceImpl(
            BlacklistRuleRepository blacklistRuleRepository,
            UserRepository userRepository,
            CidrMatcher cidrMatcher) {
        this.blacklistRuleRepository = blacklistRuleRepository;
        this.userRepository = userRepository;
        this.cidrMatcher = cidrMatcher;
    }

    @Override
    public BlacklistRule createRule(BlacklistRuleDTO ruleDTO) {
        validateRuleDto(ruleDTO);
        Instant now = Instant.now();

        BlacklistRule rule = new BlacklistRule();
        applyDtoToEntity(ruleDTO, rule);
        rule.setCreatedAt(now);
        rule.setUpdatedAt(now);

        return blacklistRuleRepository.save(rule);
    }

    @Override
    public BlacklistRule updateRule(Long id, BlacklistRuleDTO ruleDTO) {
        validateRuleDto(ruleDTO);
        BlacklistRule existing = blacklistRuleRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Blacklist rule not found with id: " + id));

        applyDtoToEntity(ruleDTO, existing);
        existing.setUpdatedAt(Instant.now());

        return blacklistRuleRepository.save(existing);
    }

    @Override
    public void deleteRule(Long id) {
        if (!blacklistRuleRepository.findById(id).isPresent()) {
            throw new RuntimeException("Blacklist rule not found with id: " + id);
        }
        blacklistRuleRepository.deleteById(id);
    }

    @Override
    public Optional<BlacklistRule> findById(Long id) {
        return blacklistRuleRepository.findById(id);
    }

    @Override
    public List<BlacklistRule> findAll() {
        return blacklistRuleRepository.findAll();
    }

    @Override
    public List<BlacklistRule> findByType(BlacklistRuleType ruleType) {
        return blacklistRuleRepository.findByRuleType(ruleType);
    }

    @Override
    public boolean isIpBlocked(String clientIp) {
        return !findMatchingIpRules(clientIp, Instant.now()).isEmpty();
    }

    @Override
    public boolean isUserBlocked(String username, User user) {
        return !findMatchingUserRules(username, user, Instant.now()).isEmpty();
    }

    @Override
    public BlacklistEvaluationResult evaluate(String clientIp, String username, User user) {
        Instant now = Instant.now();
        List<BlacklistRule> matchedIpRules = findMatchingIpRules(clientIp, now);
        List<BlacklistRule> matchedUserRules = findMatchingUserRules(username, user, now);

        return new BlacklistEvaluationResult(
            !matchedIpRules.isEmpty(),
            !matchedUserRules.isEmpty(),
            matchedIpRules,
            matchedUserRules
        );
    }

    @Override
    public BlacklistRuleDTO toDTO(BlacklistRule rule) {
        if (rule == null) {
            return null;
        }
        return BlacklistRuleDTO.builder()
            .id(rule.getId())
            .ruleType(rule.getRuleType())
            .targetValue(rule.getTargetValue())
            .userId(rule.getUser() != null ? rule.getUser().getId() : null)
            .reason(rule.getReason())
            .active(rule.getActive())
            .createdAt(rule.getCreatedAt())
            .updatedAt(rule.getUpdatedAt())
            .expiresAt(rule.getExpiresAt())
            .build();
    }

    private List<BlacklistRule> findMatchingIpRules(String clientIp, Instant referenceTime) {
        if (clientIp == null || clientIp.isBlank() || !cidrMatcher.isValidIp(clientIp)) {
            return List.of();
        }
        List<BlacklistRule> activeIpRules = blacklistRuleRepository.findByRuleTypeAndActiveTrue(BlacklistRuleType.IP_CIDR);
        return activeIpRules.stream()
            .filter(rule -> rule.isCurrentlyActive(referenceTime))
            .filter(rule -> cidrMatcher.matches(clientIp, rule.getTargetValue()))
            .toList();
    }

    private List<BlacklistRule> findMatchingUserRules(String username, User user, Instant referenceTime) {
        Map<Long, BlacklistRule> matchedMap = new LinkedHashMap<>();
        List<BlacklistRule> unindexedMatches = new ArrayList<>();

        if (username != null && !username.isBlank()) {
            List<BlacklistRule> byUsername = blacklistRuleRepository
                .findByRuleTypeAndTargetValueIgnoreCaseAndActiveTrue(BlacklistRuleType.USER, username.trim());
            for (BlacklistRule rule : byUsername) {
                if (rule.isCurrentlyActive(referenceTime)) {
                    if (rule.getId() != null) {
                        matchedMap.put(rule.getId(), rule);
                    } else {
                        unindexedMatches.add(rule);
                    }
                }
            }
        }

        if (user != null && user.getId() != null) {
            List<BlacklistRule> byUserId = blacklistRuleRepository.findByUser_IdAndActiveTrue(user.getId());
            for (BlacklistRule rule : byUserId) {
                if (rule.isCurrentlyActive(referenceTime)) {
                    if (rule.getId() != null) {
                        matchedMap.put(rule.getId(), rule);
                    } else {
                        unindexedMatches.add(rule);
                    }
                }
            }
        }

        List<BlacklistRule> result = new ArrayList<>(matchedMap.values());
        result.addAll(unindexedMatches);
        return result;
    }

    private void validateRuleDto(BlacklistRuleDTO ruleDTO) {
        if (ruleDTO == null) {
            throw new IllegalArgumentException("Blacklist rule data cannot be null");
        }
        if (ruleDTO.getRuleType() == null) {
            throw new IllegalArgumentException("Blacklist rule type is required");
        }
        if ((ruleDTO.getTargetValue() == null || ruleDTO.getTargetValue().isBlank()) && ruleDTO.getUserId() == null) {
            throw new IllegalArgumentException("Blacklist target value is required");
        }
    }

    private void applyDtoToEntity(BlacklistRuleDTO dto, BlacklistRule entity) {
        entity.setRuleType(dto.getRuleType());
        entity.setReason(dto.getReason());
        entity.setActive(dto.getActive() != null ? dto.getActive() : Boolean.TRUE);
        entity.setExpiresAt(dto.getExpiresAt());

        if (dto.getRuleType() == BlacklistRuleType.IP_CIDR) {
            String normalizedCidr = cidrMatcher.normalizeCidrOrIp(dto.getTargetValue());
            entity.setTargetValue(normalizedCidr);
            entity.setUser(null);
        } else if (dto.getRuleType() == BlacklistRuleType.USER) {
            User resolvedUser = resolveUser(dto);
            entity.setUser(resolvedUser);
            if (dto.getTargetValue() != null && !dto.getTargetValue().isBlank()) {
                entity.setTargetValue(dto.getTargetValue().trim());
            } else if (resolvedUser != null) {
                entity.setTargetValue(resolvedUser.getUsername());
            } else {
                throw new IllegalArgumentException("Username or valid userId is required for USER blacklist rule");
            }
        }
    }

    private User resolveUser(BlacklistRuleDTO dto) {
        if (userRepository == null) {
            return null;
        }
        if (dto.getUserId() != null) {
            return userRepository.findById(dto.getUserId()).orElse(null);
        }
        if (dto.getTargetValue() != null && !dto.getTargetValue().isBlank()) {
            return userRepository.findByUsername(dto.getTargetValue().trim()).orElse(null);
        }
        return null;
    }
}
