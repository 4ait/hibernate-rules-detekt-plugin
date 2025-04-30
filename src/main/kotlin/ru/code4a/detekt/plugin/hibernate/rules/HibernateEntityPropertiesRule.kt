package ru.code4a.detekt.plugin.hibernate.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter
import org.jetbrains.kotlin.psi.psiUtil.visibilityModifierTypeOrDefault

/**
 * Checks that all properties in classes with @Entity annotation are private and var.
 * This is important for proper Hibernate functionality.
 *
 * <noncompliant>
 * @Entity
 * class User {
 *     val id: Long = 0 // Violation: property should be var
 *     public var name: String = "" // Violation: property should be private
 * }
 * </noncompliant>
 *
 * <compliant>
 * @Entity
 * class User {
 *     private var id: Long = 0
 *     private var name: String = ""
 * }
 * </compliant>
 */
class HibernateEntityPropertiesRule(config: Config = Config.empty) : Rule(config) {
  override val issue = Issue(
    javaClass.simpleName,
    Severity.Style,
    "All properties in Hibernate Entity classes must be declared as 'private var'.",
    Debt.FIVE_MINS
  )

  override fun visitClass(klass: KtClass) {
    super.visitClass(klass)

    // Check if the class has @Entity annotation
    val isEntity = klass.annotationEntries.any { annotation ->
      annotation.shortName?.asString() == "Entity"
    }

    if (isEntity) {
      klass.body?.properties?.forEach { property ->
        checkProperty(property)
      }
      klass.primaryConstructor?.let { primaryConstructor ->
        primaryConstructor.valueParameters.forEach { parameter ->
          if (parameter.isPropertyParameter()) {
            checkValueParameter(parameter)
          }
        }
      }
    }
  }

  private fun checkProperty(property: KtProperty) {
    // Check if property is private
    val isPrivate = property.visibilityModifierTypeOrDefault().toString() == "private"

    // Check if property is var (mutable)
    val isVar = property.isVar

    if (!isPrivate || !isVar) {
      val message = buildString {
        append("Property '${property.name}' in Entity class ")
        if (!isPrivate) append("must be private. ")
        if (!isVar) append("must be var. ")
      }

      report(CodeSmell(issue, Entity.from(property), message))
    }
  }

  private fun checkValueParameter(parameter: KtParameter) {
    // Check if property is private
    val isPrivate = parameter.visibilityModifierTypeOrDefault().toString() == "private"

    // Check if property is var (mutable)
    val isVar = parameter.valOrVarKeyword?.text == "var"

    if (!isPrivate || !isVar) {
      val message = buildString {
        append("Constructor property '${parameter.name}' in Entity class ")
        if (!isPrivate) append("must be private. ")
        if (!isVar) append("must be var. ")
      }

      report(CodeSmell(issue, Entity.from(parameter), message))
    }
  }
}
