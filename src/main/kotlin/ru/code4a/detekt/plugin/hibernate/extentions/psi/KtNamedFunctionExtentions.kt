package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.resolve.BindingContext

fun KtNamedFunction.getAnnotations(bindingContext: BindingContext): Annotations? =
  getResolvedFunctionDescriptor(bindingContext)?.annotations

fun KtNamedFunction.getResolvedFunctionDescriptor(bindingContext: BindingContext): FunctionDescriptor? =
  bindingContext.get(BindingContext.FUNCTION, this)
