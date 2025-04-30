package ru.code4a.detekt.plugin.hibernate.rules

import io.gitlab.arturbosch.detekt.api.*
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameOrNull
import org.jetbrains.kotlin.types.KotlinType
import java.util.concurrent.ConcurrentHashMap

/**
 * Detekt rule that requires calling a specified initialization method immediately after creating an Entity.
 * The rule checks that after each object creation of a class marked with @Entity,
 * the specified method is called on this entity or passed as a parameter to it.
 */
@RequiresTypeResolution
class RequireEntityRegistrationRule(config: Config) : Rule(config) {
  override val issue = Issue(
    javaClass.simpleName,
    Severity.Defect,
    "After creating an Entity, an initialization method must be called immediately",
    Debt.TWENTY_MINS
  )

  /**
   * Full name of the method that should be called after entity creation.
   */
  private val requiredStaticMethod: String = valueOrDefault(
    "requiredStaticMethod",
    "com.example.EntityTracker.Companion.register"
  )

  /**
   * Annotations that define an entity.
   */
  private val entityAnnotations = setOf(
    "jakarta.persistence.Entity",
    "javax.persistence.Entity"
  )

  // Thread-safe set to track processed expressions
  private val processedExpressions = ConcurrentHashMap.newKeySet<KtCallExpression>()

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    // Skip if we've already processed this expression
    if (!processedExpressions.add(expression)) return

    // Check if this is constructor call for an entity
    val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
    val descriptor = resolvedCall.resultingDescriptor

    // Ensure this is a constructor call
    if (descriptor !is ClassConstructorDescriptor) return

    // Check if the constructed class is an entity
    val containingClass = descriptor.containingDeclaration
    val isEntity = containingClass.annotations.any { annotation ->
      entityAnnotations.contains(annotation.fqName?.asString())
    }

    if (!isEntity) return

    // Check if the entity creation is properly initialized
    if (!isEntityInitialized(expression, bindingContext)) {
      report(
        CodeSmell(
          issue,
          Entity.from(expression),
          "After entity creation, you must call the method $requiredStaticMethod immediately"
        )
      )
    }
  }

  /**
   * Checks if the entity creation expression is properly initialized
   */
  private fun isEntityInitialized(expression: KtCallExpression, bindingContext: BindingContext): Boolean {
    val parent = expression.parent

    // Case 1: Direct method call with entity as argument - EntityTracker.register(Entity())
    if (parent is KtValueArgument) {
      val argumentList = parent.parent as? KtValueArgumentList ?: return false
      val callExpression = argumentList.parent as? KtCallExpression ?: return false

      // Check if it's our target method through bindingContext
      val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return false
      val fqName = resolvedCall.resultingDescriptor.fqNameOrNull()?.asString() ?: return false

      return fqName == requiredStaticMethod
    }

    // Case 2: Method chaining - Entity().register()
    if (parent is KtDotQualifiedExpression && parent.receiverExpression == expression) {
      val selectorExpression = parent.selectorExpression as? KtCallExpression ?: return false

      // Check if it's our target method through bindingContext
      val resolvedCall = selectorExpression.getResolvedCall(bindingContext) ?: return false
      val fqName = resolvedCall.resultingDescriptor.fqNameOrNull()?.asString() ?: return false

      return fqName == requiredStaticMethod
    }

    // Case 3: Assigned to variable and then initialized - val entity = Entity(); EntityTracker.register(entity)
    if (parent is KtProperty) {
      val variableName = parent.nameIdentifier?.text ?: return false
      val block = parent.parent as? KtBlockExpression ?: return false
      val statements = block.statements
      val thisIndex = statements.indexOf(parent)

      // Check if the next statement is initialization
      if (thisIndex < statements.size - 1) {
        val nextStatement = statements[thisIndex + 1]
        return isVariableInitialized(nextStatement, variableName, bindingContext)
      }
    }

    return false
  }

  /**
   * Checks if a statement is initializing the specified variable
   */
  private fun isVariableInitialized(
    statement: KtExpression,
    variableName: String,
    bindingContext: BindingContext
  ): Boolean {
    // Static method call: EntityTracker.register(entity)
    if (statement is KtDotQualifiedExpression) {
      val selector = statement.selectorExpression as? KtCallExpression ?: return false

      // Check if it's our target method through bindingContext
      val resolvedCall = selector.getResolvedCall(bindingContext) ?: return false
      val fqName = resolvedCall.resultingDescriptor.fqNameOrNull()?.asString() ?: return false

      if (fqName == requiredStaticMethod) {
        val args = selector.valueArguments
        return args.isNotEmpty() && args[0].getArgumentExpression()?.text == variableName
      }
    }

    // Extension method call: entity.register()
    if (statement is KtDotQualifiedExpression) {
      val receiver = statement.receiverExpression as? KtNameReferenceExpression ?: return false
      if (receiver.text != variableName) return false

      val selector = statement.selectorExpression as? KtCallExpression ?: return false

      // Check if it's our target method through bindingContext
      val resolvedCall = selector.getResolvedCall(bindingContext) ?: return false
      val fqName = resolvedCall.resultingDescriptor.fqNameOrNull()?.asString() ?: return false

      return fqName == requiredStaticMethod
    }

    return false
  }
}
