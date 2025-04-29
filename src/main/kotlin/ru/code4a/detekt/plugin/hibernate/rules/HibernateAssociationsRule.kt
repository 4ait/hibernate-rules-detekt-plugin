package ru.code4a.detekt.plugin.hibernate.rules

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import io.gitlab.arturbosch.detekt.api.internal.RequiresTypeResolution
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.impl.referencedProperty
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtValueArgumentList
import org.jetbrains.kotlin.psi.KtWhenEntry
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.getReferenceTargets
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.jvm.annotations.findJvmFieldAnnotation

/**
 * This rule detects direct mutations of Hibernate association fields.
 * Disallows:
 * 1. Direct mutations to @ManyToOne, @OneToOne, @ManyToMany, @OneToMany fields
 * 2. Assigning @ManyToOne, @OneToOne fields directly
 * 3. Returning, storing in variables, or passing hibernate collections as parameters without copying
 *
 * Uses a strict whitelist approach - only operations explicitly allowed in the whitelist are permitted.
 */
@RequiresTypeResolution
class HibernateAssociationsRule(config: Config = Config.empty) : Rule(config) {
  override val issue = Issue(
    javaClass.simpleName,
    Severity.Defect,
    "Direct mutations or assignments of Hibernate association fields are not allowed.",
    Debt.TWENTY_MINS
  )

  // Default whitelist of fully qualified methods/functions that are safe to use with Hibernate collections
  private val defaultSafeOperations = setOf(
    // Constructors - java.util
    "java.util.ArrayList",
    "java.util.LinkedList",
    "java.util.HashSet",
    "java.util.TreeSet",
    "java.util.HashMap",
    "java.util.TreeMap",
    "java.util.LinkedHashMap",
    "java.util.LinkedHashSet",

    // Constructors - kotlin.collections
    "kotlin.collections.ArrayList",
    "kotlin.collections.HashSet",
    "kotlin.collections.LinkedHashSet",
    "kotlin.collections.HashMap",

    // Factory methods
    "kotlin.collections.mutableListOf",
    "kotlin.collections.listOf",
    "kotlin.collections.setOf",
    "kotlin.collections.mutableSetOf",
    "kotlin.collections.mapOf",
    "kotlin.collections.mutableMapOf",
    "kotlin.collections.emptyList",
    "kotlin.collections.emptySet",
    "kotlin.collections.emptyMap",
    "kotlin.collections.copyOf",

    // Extension functions - Copy operations
    "kotlin.collections.toList",
    "kotlin.collections.toMutableList",
    "kotlin.collections.toSet",
    "kotlin.collections.toMutableSet",
    "kotlin.collections.toCollection",
    "kotlin.collections.map",
    "kotlin.collections.mapNotNull",
    "kotlin.collections.filter",
    "kotlin.collections.filterNotNull",
    "kotlin.collections.filterNot",
    "kotlin.collections.filterIsInstance",
    "kotlin.collections.slice",
    "kotlin.collections.take",
    "kotlin.collections.takeLast",
    "kotlin.collections.drop",
    "kotlin.collections.dropLast",
    "kotlin.collections.plus",
    "kotlin.collections.minus",
    "kotlin.collections.distinct",
    "kotlin.collections.distinctBy",
    "kotlin.collections.sorted",
    "kotlin.collections.sortedBy",
    "kotlin.collections.sortedDescending",
    "kotlin.collections.sortedByDescending",
    "kotlin.collections.reversed",
    "kotlin.collections.shuffled",
    "kotlin.collections.chunked",
    "kotlin.collections.windowed",
    "kotlin.collections.zipWithNext",
    "kotlin.collections.zip",
    "kotlin.collections.associate",
    "kotlin.collections.associateBy",
    "kotlin.collections.groupBy",
    "kotlin.collections.flatten",
    "kotlin.collections.flatMap",
    "kotlin.collections.asReversed",
    "kotlin.sequences.asSequence",

    // Extension functions - Read operations
    "kotlin.collections.forEach",
    "kotlin.collections.forEachIndexed",
    "kotlin.collections.any",
    "kotlin.collections.all",
    "kotlin.collections.none",
    "kotlin.collections.count",
    "kotlin.collections.find",
    "kotlin.collections.findLast",
    "kotlin.collections.first",
    "kotlin.collections.firstOrNull",
    "kotlin.collections.last",
    "kotlin.collections.lastOrNull",
    "kotlin.collections.single",
    "kotlin.collections.singleOrNull",
    "kotlin.collections.isEmpty",
    "kotlin.collections.isNotEmpty",
    "kotlin.collections.contains",
    "kotlin.collections.containsAll",
    "kotlin.collections.indexOf",
    "kotlin.collections.lastIndexOf",
    "kotlin.collections.maxBy",
    "kotlin.collections.maxByOrNull",
    "kotlin.collections.minBy",
    "kotlin.collections.minByOrNull",
    "kotlin.collections.maxOf",
    "kotlin.collections.maxOfOrNull",
    "kotlin.collections.minOf",
    "kotlin.collections.minOfOrNull",
    "kotlin.collections.maxWith",
    "kotlin.collections.maxWithOrNull",
    "kotlin.collections.minWith",
    "kotlin.collections.minWithOrNull",
    "kotlin.collections.sumBy",
    "kotlin.collections.sumByDouble",
    "kotlin.collections.sumOf",
    "kotlin.collections.reduce",
    "kotlin.collections.reduceOrNull",
    "kotlin.collections.fold",
    "kotlin.collections.foldRight",
    "kotlin.collections.foldIndexed",
    "kotlin.collections.joinToString",
    "kotlin.collections.joinTo",
    "kotlin.collections.elementAt",
    "kotlin.collections.elementAtOrNull",
    "kotlin.collections.elementAtOrElse",
    "kotlin.collections.getOrElse",
    "kotlin.collections.getOrNull",
    "kotlin.collections.random",
    "kotlin.collections.randomOrNull",
    "kotlin.collections.binarySearch",
    "kotlin.collections.size",
    "kotlin.collections.iterator",
    "kotlin.collections.indices"
  )

  // User-defined additional safe operations from configuration
  private val additionalSafeOperations = valueOrDefault(ADDITIONAL_SAFE_OPERATIONS, emptyList<String>()).toSet()

  // Combined sets with default and user-defined values
  private val safeOperations = defaultSafeOperations + additionalSafeOperations

  private val hibernateAnnotations = setOf(
    "javax.persistence.OneToMany",
    "javax.persistence.ManyToMany",
    "javax.persistence.OneToOne",
    "javax.persistence.ManyToOne",
    "jakarta.persistence.OneToMany",
    "jakarta.persistence.ManyToMany",
    "jakarta.persistence.OneToOne",
    "jakarta.persistence.ManyToOne"
  )

  private val collectionAnnotations = setOf(
    "javax.persistence.OneToMany",
    "javax.persistence.ManyToMany",
    "jakarta.persistence.OneToMany",
    "jakarta.persistence.ManyToMany"
  )

  override fun visitBinaryExpression(expression: KtBinaryExpression) {
    super.visitBinaryExpression(expression)

    // Check only assignments
    if (expression.operationToken !in setOf(
        KtTokens.EQ, KtTokens.PLUSEQ, KtTokens.MINUSEQ,
        KtTokens.MULTEQ, KtTokens.DIVEQ, KtTokens.PERCEQ
      )
    ) {
      if (expression.operationToken == KtTokens.ELVIS) {
        val right = expression.right ?: return

        // Check if the right side is a direct reference to a Hibernate collection
        if (!isSafeOperation(right) && isHibernateCollectionField(right)) {
          report(
            CodeSmell(
              issue,
              Entity.from(expression),
              "Using Hibernate collection directly with elvis operator is not allowed. Use a copy instead."
            )
          )
        }
      }
    }

    val left = expression.left ?: return

    // Check for array access on the left side (map[key] = value)
    if (left is KtArrayAccessExpression) {
      val arrayExpression = left.arrayExpression
      if (arrayExpression != null && isHibernateCollectionField(arrayExpression)) {
        report(
          CodeSmell(
            issue,
            Entity.from(expression),
            "Direct element modification in Hibernate collection is not allowed."
          )
        )
        return
      }
    }

    when (left) {
      is KtDotQualifiedExpression -> {
        checkLeftSideOfAssignment(left, expression)
      }

      is KtNameReferenceExpression -> {
        checkLeftSideOfAssignment(left, expression)
      }
    }
  }

  override fun visitWhenExpression(expression: KtWhenExpression) {
    super.visitWhenExpression(expression)

    // Go through each entry in the when expression
    expression.entries.forEach { entry ->
      checkWhenEntry(entry)
    }

    // Also check if the whole when expression is used in a context where it would return a Hibernate collection
    val parent = expression.parent
    if (parent is KtProperty || parent is KtReturnExpression || parent is KtValueArgumentList) {
      // The when expression is used as a value, so we need to ensure all branches are safe
      val hasUnsafeBranch = expression.entries.any { entry ->
        val result = entry.expression
        result != null && isHibernateCollectionField(result) && !isSafeOperation(result)
      }

      if (hasUnsafeBranch) {
        report(
          CodeSmell(
            issue,
            Entity.from(expression),
            "Using when expression that returns Hibernate collection directly is not allowed. Use copies instead."
          )
        )
      }
    }
  }

  private fun checkWhenEntry(entry: KtWhenEntry) {
    val result = entry.expression ?: return

    // Skip checking if the result is a safe operation
    if (isSafeOperation(result)) {
      return
    }

    if (isHibernateCollectionField(result)) {
      report(
        CodeSmell(
          issue,
          Entity.from(result),
          "Using Hibernate collection directly in when expression is not allowed. Use a copy instead."
        )
      )
    }
  }

  override fun visitCallExpression(expression: KtCallExpression) {
    super.visitCallExpression(expression)

    // First, check if this is a safe constructor call with a hibernate collection parameter
    // This is handled in the visitValueArgumentList method

    // Check method calls on collections like add/remove/clear
    val dotExpression = expression.parent as? KtDotQualifiedExpression ?: return
    val receiverExpression = dotExpression.receiverExpression

    // Skip if receiver is not a Hibernate collection
    if (!isHibernateCollectionField(receiverExpression)) {
      return
    }

    // Use binding context to get fully qualified name of the method being called
    val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
    val methodFqName = resolvedCall.resultingDescriptor.fqNameSafe.asString()

    // Check if this is a safe operation based on FQ name
    if (safeOperations.any { methodFqName.startsWith(it) }) {
      // This is a safe operation, allowed
      return
    }

    // If not explicitly allowed, report violation (strict whitelist approach)
    report(
      CodeSmell(
        issue,
        Entity.from(expression),
        "Operation '$methodFqName' on Hibernate collection is not allowed. Only whitelisted operations are permitted."
      )
    )
  }

  override fun visitReturnExpression(expression: KtReturnExpression) {
    super.visitReturnExpression(expression)

    // Check that hibernate collections are not returned directly
    val returnValue = expression.returnedExpression ?: return

    // Skip checking if the return value is a safe operation
    if (isSafeOperation(returnValue)) {
      return
    }

    if (isHibernateCollectionField(returnValue)) {
      report(
        CodeSmell(
          issue,
          Entity.from(expression),
          "Returning Hibernate collection directly is not allowed. Return a copy instead."
        )
      )
    }
  }

  override fun visitLambdaExpression(expression: KtLambdaExpression) {
    super.visitLambdaExpression(expression)

    expression.bodyExpression?.acceptChildren(this)

    expression.bodyExpression?.statements?.forEach { statement ->
      // If the statement is just a reference to a hibernate collection, report it
      if (statement is KtExpression && isHibernateCollectionField(statement) && !isSafeOperation(statement)) {
        report(
          CodeSmell(
            issue,
            Entity.from(statement),
            "Referencing Hibernate collection directly in a lambda is not allowed. Use a copy instead."
          )
        )
      }
    }
  }

  override fun visitPropertyAccessor(accessor: KtPropertyAccessor) {
    super.visitPropertyAccessor(accessor)

    accessor.bodyExpression?.acceptChildren(this)

    // Only check getters (not setters)
    if (!accessor.isGetter) return

    // Check the body of the getter
    val bodyExpression = accessor.bodyExpression ?: return

    // Skip checking if the body is a safe operation
    if (isSafeOperation(bodyExpression)) return

    // Check if the getter returns a Hibernate collection directly
    if (isHibernateCollectionField(bodyExpression)) {
      report(
        CodeSmell(
          issue,
          Entity.from(accessor),
          "Returning Hibernate collection directly from a property getter is not allowed. Return a copy instead."
        )
      )
    }
  }

  override fun visitProperty(property: KtProperty) {
    super.visitProperty(property)

    // Check variable assignments of hibernate collections
    val initializer = property.initializer ?: return

    // Skip checking if the initializer is a safe operation
    if (isSafeOperation(initializer)) {
      return
    }

    if (isHibernateCollectionField(initializer)) {
      report(
        CodeSmell(
          issue,
          Entity.from(property),
          "Storing Hibernate collection in a variable directly is not allowed. Store a copy instead."
        )
      )
    }
  }

  override fun visitValueArgumentList(list: KtValueArgumentList) {
    super.visitValueArgumentList(list)

    // Get parent call expression to check if it's a safe constructor or factory method
    val callExpression = list.parent as? KtCallExpression ?: return
    val resolvedCall =
      callExpression.getResolvedCall(bindingContext) ?: callExpression.calleeExpression?.getResolvedCall(bindingContext)

    if (resolvedCall != null) {
      val callFqName = resolvedCall.resultingDescriptor.fqNameSafe.asString()

      // Skip checking if the call is to a safe constructor or factory method
      if (safeOperations.any { callFqName.startsWith(it) }) {
        return
      }
    }

    // Check passing hibernate collections as function parameters
    for (argument in list.arguments) {
      val argumentExpression = argument.getArgumentExpression() ?: continue

      // Skip checking if the argument itself is a safe operation
      if (isSafeOperation(argumentExpression)) {
        continue
      }

      if (isHibernateCollectionField(argumentExpression)) {
        report(
          CodeSmell(
            issue,
            Entity.from(argument),
            "Passing Hibernate collection as a parameter directly is not allowed. Pass a copy instead."
          )
        )
      }
    }
  }

  /**
   * Checks if the expression is a safe copy operation on a collection
   */
  private fun isSafeOperation(expression: KtExpression): Boolean {
    // Handle direct call expressions like collection.toList()
    if (expression is KtDotQualifiedExpression) {
      val receiverIsHibernateCollection = isHibernateCollectionField(expression.receiverExpression)
      val callExpression = expression.selectorExpression as? KtCallExpression ?: return false

      if (receiverIsHibernateCollection) {
        val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return false
        val methodFqName = resolvedCall.resultingDescriptor.fqNameSafe.asString()

        // Check if it matches any safe operation
        return safeOperations.any { methodFqName.startsWith(it) }
      }
    }

    // Handle constructor expressions like ArrayList(collection)
    if (expression is KtCallExpression) {
      val resolvedCall = expression.getResolvedCall(bindingContext) ?: return false
      val callFqName = resolvedCall.resultingDescriptor.fqNameSafe.asString()

      // Check if it's a safe operation
      if (safeOperations.any { callFqName.startsWith(it) }) {
        // Check if any argument is a Hibernate collection (at least one should be)
        for (arg in expression.valueArguments) {
          val argExpr = arg.getArgumentExpression()
          if (argExpr != null && isHibernateCollectionField(argExpr)) {
            return true
          }
        }
      }
    }

    return false
  }

  private fun checkLeftSideOfAssignment(dotExpression: KtDotQualifiedExpression, assignment: KtBinaryExpression) {
    if (isHibernateField(dotExpression.selectorExpression ?: dotExpression.receiverExpression)) {
      val isCollectionAccess = dotExpression.text.contains("[") ||
        (dotExpression.selectorExpression?.text in setOf("[]", "get"))

      if (isCollectionAccess) {
        report(
          CodeSmell(
            issue,
            Entity.from(assignment),
            "Direct element modification in Hibernate collection is not allowed."
          )
        )
      } else {
        report(
          CodeSmell(
            issue,
            Entity.from(assignment),
            "Direct assignment to Hibernate association field is not allowed."
          )
        )
      }
    }
  }

  private fun checkLeftSideOfAssignment(dotExpression: KtNameReferenceExpression, assignment: KtBinaryExpression) {
    if (isHibernateField(dotExpression)) {
      val isCollectionAccess = dotExpression.text.contains("[") ||
        (dotExpression.text in setOf("[]", "get"))

      if (isCollectionAccess) {
        report(
          CodeSmell(
            issue,
            Entity.from(assignment),
            "Direct element modification in Hibernate collection is not allowed."
          )
        )
      } else {
        report(
          CodeSmell(
            issue,
            Entity.from(assignment),
            "Direct assignment to Hibernate association field is not allowed."
          )
        )
      }
    }
  }

  override fun visitTryExpression(expression: KtTryExpression) {
    super.visitTryExpression(expression)

    // The try block should already be checked by other visit methods
    // But we need to ensure we check the direct use of Hibernate collections in the try block
    val tryBlock = expression.tryBlock

    // Find any direct references to Hibernate collections
    tryBlock.children.forEach { child ->
      if (child is KtExpression && !isSafeOperation(child) && isHibernateCollectionField(child)) {
        // Only report if this is a direct reference that would be used as a value
        // (e.g., in a variable initialization or return)
        if (child.parent is KtReturnExpression ||
          (child.parent.parent is KtTryExpression && expression.parent is KtProperty)
        ) {
          report(
            CodeSmell(
              issue,
              Entity.from(child),
              "Using Hibernate collection directly in try block is not allowed. Use a copy instead."
            )
          )
        }
      }
    }
  }

  override fun visitIfExpression(expression: KtIfExpression) {
    super.visitIfExpression(expression)

    // Check for elvis operator usage (which is compiled as if-else)
    // This won't directly catch elvis, but will help with general if-expressions
    val thenExpr = expression.then
    val elseExpr = expression.`else`

    // Check if either branch directly references a Hibernate collection
    if (thenExpr != null && !isSafeOperation(thenExpr) && isHibernateCollectionField(thenExpr)) {
      report(
        CodeSmell(
          issue,
          Entity.from(thenExpr),
          "Using Hibernate collection directly in conditional expression is not allowed. Use a copy instead."
        )
      )
    }

    if (elseExpr != null && !isSafeOperation(elseExpr) && isHibernateCollectionField(elseExpr)) {
      report(
        CodeSmell(
          issue,
          Entity.from(elseExpr),
          "Using Hibernate collection directly in conditional expression is not allowed. Use a copy instead."
        )
      )
    }
  }

  private fun isHibernateField(expression: KtExpression): Boolean {
    val bindingContext = bindingContext
    if (bindingContext == BindingContext.EMPTY) return false

    val targets = expression.getReferenceTargets(bindingContext)

    return targets.any { descriptor ->
      when (descriptor) {
        // Handle regular property descriptors
        is PropertyDescriptor -> {
          descriptor.getAllAssociatedAnnotations().any { fqName ->
            hibernateAnnotations.contains(fqName)
          }
        }

        else -> false
      }
    }
  }

  private fun isHibernateCollectionField(expression: KtExpression): Boolean {
    val bindingContext = bindingContext
    if (bindingContext == BindingContext.EMPTY) return false

    val annotations = when (expression) {
      is KtNameReferenceExpression -> {
        val targets = expression.getReferenceTargets(bindingContext)
        targets.filterIsInstance<PropertyDescriptor>().firstOrNull()?.getAllAssociatedAnnotations()
      }

      is KtDotQualifiedExpression -> {
        val selectorExpression = expression.selectorExpression as? KtNameReferenceExpression ?: return false
        val targets = selectorExpression.getReferenceTargets(bindingContext)
        targets.filterIsInstance<PropertyDescriptor>().firstOrNull()?.getAllAssociatedAnnotations()
      }

      else -> null
    } ?: return false

    return annotations.any { annotation ->
      collectionAnnotations.contains(annotation)
    }
  }

  fun PropertyDescriptor.getAllAssociatedAnnotations(): List<String> {
    // Check annotations on the property itself
    val propertyAnnotations = annotations.mapNotNull { it.fqName?.asString() }

    // Check annotations on constructor parameter if this is a constructor property
    val constructorParameterAnnotations = backingField?.annotations?.mapNotNull {
      it.fqName?.asString()
    } ?: emptyList()

    // Check if this is a primary constructor property parameter
    val primaryCtorParamAnnotations =
      if (containingDeclaration is ClassDescriptor) {
        val classDescriptor = containingDeclaration as ClassDescriptor
        val primaryCtor = classDescriptor.constructors.firstOrNull { it.isPrimary }

        primaryCtor?.valueParameters
          ?.find {
            it.name == name
          }
          ?.annotations
          ?.mapNotNull {
            it.fqName?.asString()
          } ?: emptyList()
      } else {
        emptyList()
      }

    return propertyAnnotations + constructorParameterAnnotations + primaryCtorParamAnnotations
  }

  companion object {
    const val DEFAULT_DETECTOR_DESCRIPTION = "Detects direct mutations of Hibernate association fields"

    // Single configuration parameter for additional safe operations
    const val ADDITIONAL_SAFE_OPERATIONS = "additionalSafeOperations"
  }
}
