package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext

fun KtExpression.isClassMember(bindingContext: BindingContext): Boolean {
  return (this as? KtReferenceExpression)?.isClassMember(bindingContext) ?: return false
}

fun KtExpression.getContainingClassOrObject(): KtClassOrObject? {
  var parent = this.parent
  while (parent != null && parent !is KtClassOrObject) {
    parent = parent.parent
  }
  return parent as? KtClassOrObject
}

fun KtExpression.getContainingFunctionOrMethod(): KtNamedFunction? {
  var parent = this.parent
  while (parent != null && parent !is KtNamedFunction) {
    parent = parent.parent
  }
  return parent as? KtNamedFunction
}

fun KtExpression.getContainingClassDescriptor(context: BindingContext): ClassDescriptor? {
  val containingClassOrObject = this.getContainingClassOrObject() ?: return null
  return context[BindingContext.CLASS, containingClassOrObject]
}

fun KtExpression.getContainingClassDescriptorOutsideCompanion(context: BindingContext): ClassDescriptor? {
  val containingClass =
    getContainingClassDescriptor(context) ?: return null

  if (containingClass.isCompanionObject) {
    return containingClass.containingDeclaration as? ClassDescriptor
  }

  return containingClass
}
