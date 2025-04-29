package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.psi.KtPropertyDelegate
import org.jetbrains.kotlin.resolve.BindingContext

fun KtPropertyDelegate.getResolvedFunctionDescriptor(bindingContext: BindingContext): SimpleFunctionDescriptor? =
  bindingContext.get(BindingContext.FUNCTION, this)
