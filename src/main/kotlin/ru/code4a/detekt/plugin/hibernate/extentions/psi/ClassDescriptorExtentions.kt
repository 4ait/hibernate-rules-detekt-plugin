package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.descriptorUtil.getAllSuperClassifiers

fun ClassDescriptor.implementsInterface(interfaceDescriptor: ClassDescriptor): Boolean {
  val a = getAllSuperClassifiers()

  return a.mapNotNull { it as? ClassDescriptor }.any { it == interfaceDescriptor }
}

fun ClassDescriptor.getAnnotationsOutsideCompanion(): Annotations? {
  val annotations =
    if (isCompanionObject) {
      val containingClassDescriptor = containingDeclaration as? ClassDescriptor

      containingClassDescriptor?.annotations
    } else {
      annotations
    }

  return annotations
}
