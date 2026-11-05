package ar.edu.um.ingenieria.limitador.services;

import java.util.List;
import java.util.Optional;

import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;
import ar.edu.um.ingenieria.limitador.domain.BlacklistRuleType;
import ar.edu.um.ingenieria.limitador.domain.User;
import ar.edu.um.ingenieria.limitador.dto.BlacklistEvaluationResult;
import ar.edu.um.ingenieria.limitador.dto.BlacklistRuleDTO;

public interface BlacklistService {
    BlacklistRule createRule(BlacklistRuleDTO ruleDTO);
    BlacklistRule updateRule(Long id, BlacklistRuleDTO ruleDTO);
    void deleteRule(Long id);
    Optional<BlacklistRule> findById(Long id);
    List<BlacklistRule> findAll();
    List<BlacklistRule> findByType(BlacklistRuleType ruleType);

    boolean isIpBlocked(String clientIp);
    boolean isUserBlocked(String username, User user);
    BlacklistEvaluationResult evaluate(String clientIp, String username, User user);

    BlacklistRuleDTO toDTO(BlacklistRule rule);
}
