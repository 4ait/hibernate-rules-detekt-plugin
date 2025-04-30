package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.resolve.BindingContext

fun KtProperty.getContainingClassOrObject(): KtClassOrObject? {
  var parent = this.parent
  while (parent != null && parent !is KtClassOrObject) {
    parent = parent.parent
  }
  return parent as? KtClassOrObject
}

fun KtProperty.getVariableDescriptor(bindingContext: BindingContext): VariableDescriptor? {
  return bindingContext[BindingContext.VARIABLE, this]
}

fun KtProperty.getPropertyDescriptor(bindingContext: BindingContext): PropertyDescriptor? {
  return getVariableDescriptor(bindingContext) as? PropertyDescriptor
}
