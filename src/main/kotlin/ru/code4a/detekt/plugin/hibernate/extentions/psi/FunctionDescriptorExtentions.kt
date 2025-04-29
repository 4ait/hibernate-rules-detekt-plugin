package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.isTopLevelInPackage
import org.jetbrains.kotlin.fileClasses.isTopLevelInJvmMultifileClass
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered

fun FunctionDescriptor.getImplementedFunctions(bindingContext: BindingContext): List<FunctionDescriptor>? {
  val containingDescriptor = containingDeclaration
  if (containingDescriptor is ClassDescriptor && containingDescriptor.kind == ClassKind.INTERFACE) {
    val classDescriptors = bindingContext.findClassesImplementingInterface(containingDescriptor)

    return classDescriptors.flatMap { classDescriptor ->
      classDescriptor
        .unsubstitutedMemberScope
        .getDescriptorsFiltered()
        .filterIsInstance<FunctionDescriptor>()
        .filter { it.overriddenDescriptors.any { overridden -> overridden == this } }
    }
  }

  return null
}

fun FunctionDescriptor.getNamedFunction(): KtNamedFunction? = getNameFunction()

/**
 * Determines whether the given FunctionDescriptor represents a top-level function.
 *
 * @return true if the function is a top-level function, false otherwise.
 */
fun FunctionDescriptor.isTopLevelFunction(): Boolean {
  // Проверяем, имеет ли функция контекст класса
  return isTopLevelInPackage() || isTopLevelInJvmMultifileClass()
}

/**
 * Retrieves the name of the class that is being delegated to, if the function descriptor represents a delegate.
 *
 * @return the name of the class being delegated to, or null if the function descriptor does not represent a delegate
 */
fun FunctionDescriptor.getDelegatedClassName(): String? {
  val containingDeclaration = containingDeclaration

  val isDelegate = containingDeclaration is PropertyDescriptor

  if (isDelegate) {
    val containingClassDescriptor = containingDeclaration.containingDeclaration as? ClassDescriptor
    val containingClassName = containingClassDescriptor?.name?.asString()
    return containingClassName
  }
  return null
}
