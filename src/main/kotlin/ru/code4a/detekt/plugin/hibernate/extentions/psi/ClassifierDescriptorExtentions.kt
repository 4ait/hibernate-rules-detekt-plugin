package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.descriptors.LazyClassDescriptor

fun ClassifierDescriptor.getOriginalClassName(): String {
  val lazyClassDeclarationDescriptor = this as? LazyClassDescriptor

  val companionClassName =
    if (lazyClassDeclarationDescriptor != null && lazyClassDeclarationDescriptor.isCompanionObject) {
      val containingClassDescriptor = lazyClassDeclarationDescriptor.containingDeclaration as? ClassDescriptor

      containingClassDescriptor?.fqNameSafe?.asString()
    } else {
      null
    }

  return companionClassName ?: this.fqNameSafe.asString()
}

fun ClassifierDescriptor.getOriginalAnnotations(): Annotations {
  val lazyClassDeclarationDescriptor = this as? LazyClassDescriptor

  val companionAnnotations =
    if (lazyClassDeclarationDescriptor != null && lazyClassDeclarationDescriptor.isCompanionObject) {
      val containingClassDescriptor = lazyClassDeclarationDescriptor.containingDeclaration as? ClassDescriptor

      containingClassDescriptor?.annotations
    } else {
      null
    }

  return companionAnnotations ?: this.annotations
}
