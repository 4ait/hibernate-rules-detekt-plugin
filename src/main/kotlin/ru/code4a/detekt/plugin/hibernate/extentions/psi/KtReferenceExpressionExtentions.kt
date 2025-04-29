package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext

fun KtReferenceExpression.isClassMember(bindingContext: BindingContext): Boolean {
  val descriptor = getReferenceTarget(bindingContext)
  return descriptor is PropertyDescriptor && descriptor.containingDeclaration.isClassOwner()
}

fun KtReferenceExpression.getReferenceTarget(bindingContext: BindingContext): DeclarationDescriptor? =
  bindingContext[BindingContext.REFERENCE_TARGET, this]

fun KtReferenceExpression.hasAnyAnnotation(
  annotations: List<String>,
  bindingContext: BindingContext
): Boolean {
  val selectorReferenceTarget = this.getReferenceTarget(bindingContext)

  val selectorPropertyDescriptor = selectorReferenceTarget as? PropertyDescriptor

  val originalAnnotations =
    selectorPropertyDescriptor
      ?.annotations

  val backingFieldAnnotations =
    selectorPropertyDescriptor
      ?.backingField
      ?.annotations

  return annotations.any {
    originalAnnotations
      ?.hasAnnotation(
        FqName(it)
      ) ?: false ||
      backingFieldAnnotations
        ?.hasAnnotation(
          FqName(it)
        ) ?: false
  }
}
