package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.util.getResolvedCall

fun KtCallExpression.getNamedFunction(bindingContext: BindingContext): KtNamedFunction? {
  val resolvedCall = getResolvedCall(bindingContext) ?: return null

  return resolvedCall.resultingDescriptor.getNameFunction()
}

/**
 * Retrieves the function descriptor of the containing function for the given KtCallExpression.
 *
 * @return The FunctionDescriptor of the containing function, or null if the containing function cannot be found or resolved.
 */
fun KtCallExpression.getContainingFunctionDescriptor(bindingContext: BindingContext): FunctionDescriptor? {
  val containingFunction = this.getContainingFunctionOrMethod() ?: return null
  return bindingContext[BindingContext.FUNCTION, containingFunction]
}
