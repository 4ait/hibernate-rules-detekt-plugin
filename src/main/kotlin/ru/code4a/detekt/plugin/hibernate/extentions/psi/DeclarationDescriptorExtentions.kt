package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

fun DeclarationDescriptor.isClassOwner(): Boolean =
  this.containingDeclaration?.name?.asString()?.let {
    it != "<root>" && it != "<anonymous>"
  } ?: false

fun DeclarationDescriptor.getClassNameOutsideCompanion(): String {
  val classDeclarationDescriptor = this as? ClassDescriptor

  val companionClassName =
    if (classDeclarationDescriptor != null && classDeclarationDescriptor.isCompanionObject) {
      val containingClassDescriptor = classDeclarationDescriptor.containingDeclaration as? ClassDescriptor

      containingClassDescriptor?.fqNameSafe?.asString()
    } else {
      null
    }

  return companionClassName ?: this.fqNameSafe.asString()
}
