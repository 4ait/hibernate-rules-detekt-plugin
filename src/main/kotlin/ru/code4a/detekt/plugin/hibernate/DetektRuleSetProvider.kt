package ru.code4a.detekt.plugin.hibernate

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider
import ru.code4a.detekt.plugin.hibernate.rules.HibernateAssociationsRule
import ru.code4a.detekt.plugin.hibernate.rules.HibernateEntityPropertiesRule
import ru.code4a.detekt.plugin.hibernate.rules.RequireEntityRegistrationRule

class DetektRuleSetProvider : RuleSetProvider {
  override val ruleSetId: String = "foura_hibernate_rule_set"

  override fun instance(config: Config): RuleSet =
    RuleSet(
      ruleSetId,
      listOf(
        HibernateAssociationsRule(config),
        RequireEntityRegistrationRule(config),
        HibernateEntityPropertiesRule(config)
      )
    )
}
