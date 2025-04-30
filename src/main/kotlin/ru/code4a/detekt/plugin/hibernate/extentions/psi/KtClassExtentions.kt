package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.resolve.BindingContext

fun KtClass.getClassDescriptor(bindingContext: BindingContext): ClassDescriptor? {
  return bindingContext[BindingContext.CLASS, this]
}

fun KtClass.hasAnnotationAnyOf(bindingContext: BindingContext, annotationNames: Set<FqName>): Boolean {
  val classDescriptor = getClassDescriptor(bindingContext)
  return classDescriptor?.annotations?.any {
    it.fqName in annotationNames
  } == true
}
