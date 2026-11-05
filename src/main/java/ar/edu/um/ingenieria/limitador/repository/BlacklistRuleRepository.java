package ar.edu.um.ingenieria.limitador.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import ar.edu.um.ingenieria.limitador.domain.BlacklistRule;
import ar.edu.um.ingenieria.limitador.domain.BlacklistRuleType;

public interface BlacklistRuleRepository extends JpaRepository<BlacklistRule, Long> {
    List<BlacklistRule> findByRuleTypeAndActiveTrue(BlacklistRuleType ruleType);
    List<BlacklistRule> findByRuleTypeAndTargetValueIgnoreCaseAndActiveTrue(BlacklistRuleType ruleType, String targetValue);
    List<BlacklistRule> findByUser_IdAndActiveTrue(Long userId);
    List<BlacklistRule> findByRuleType(BlacklistRuleType ruleType);
}
