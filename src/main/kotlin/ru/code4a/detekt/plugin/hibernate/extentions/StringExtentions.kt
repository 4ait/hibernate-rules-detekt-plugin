package ru.code4a.detekt.plugin.hibernate.extentions

fun String.isPartOfPackageName(packageName: String): Boolean =
  packageName == this ||
    packageName.startsWith("$this.")
