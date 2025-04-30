package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor

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
