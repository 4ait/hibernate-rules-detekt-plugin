package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue

fun ReceiverValue.isClassMember(bindingContext: BindingContext): Boolean {
  return (this as? KtReferenceExpression)?.isClassMember(bindingContext) ?: return false
}
