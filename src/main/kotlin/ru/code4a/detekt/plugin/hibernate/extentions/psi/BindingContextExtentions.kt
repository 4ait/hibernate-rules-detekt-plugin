package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.BindingContext

fun BindingContext.findClassesImplementingInterface(interfaceDescriptor: ClassDescriptor): List<ClassDescriptor> {
  val result = mutableListOf<ClassDescriptor>()

  getSliceContents(BindingContext.CLASS).forEach { (ktClass, classDescriptor) ->
    if (classDescriptor != interfaceDescriptor && classDescriptor.implementsInterface(interfaceDescriptor)) {
      result.add(classDescriptor)
    }
  }

  return result
}
