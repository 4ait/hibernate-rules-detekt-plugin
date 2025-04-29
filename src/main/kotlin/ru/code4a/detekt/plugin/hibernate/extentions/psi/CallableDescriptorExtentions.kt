package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.source.KotlinSourceElement

fun CallableDescriptor.getNameFunction(): KtNamedFunction? {
  val sourceElement = source as? KotlinSourceElement ?: return null

  return sourceElement.psi as? KtNamedFunction
}

fun CallableDescriptor.isCollectionMutation(): Boolean =
  containingDeclaration.name.asString().startsWith("Mutable") &&
    listOf("add", "remove", "clear", "put").any {
      name.asString().contains(it, ignoreCase = true)
    }
