package ru.code4a.detekt.plugin.hibernate.extentions.psi

import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtProperty

fun KtProperty.getContainingClassOrObject(): KtClassOrObject? {
  var parent = this.parent
  while (parent != null && parent !is KtClassOrObject) {
    parent = parent.parent
  }
  return parent as? KtClassOrObject
}
