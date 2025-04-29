package ru.code4a.detekt.plugin.hibernate.test.extenstions.detekt

import io.gitlab.arturbosch.detekt.api.BaseRule
import io.gitlab.arturbosch.detekt.api.Finding
import io.gitlab.arturbosch.detekt.test.lintWithContext
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment

fun BaseRule.lintAllWithContex(
  environment: KotlinCoreEnvironment,
  fileContents: List<String>
): List<Finding> =
  fileContents.flatMap { fileContent ->
    lintWithContext(
      environment,
      fileContents.first { it == fileContent },
      *fileContents.filter { it != fileContent }.toTypedArray()
    )
  }

fun BaseRule.lintAllWithContextAndPrint(
  environment: KotlinCoreEnvironment,
  fileContents: List<String>
): List<Finding> {
  val findings = lintAllWithContex(environment, fileContents)

  findings.forEach {
    println(it.entity.compact())
    println(it.messageOrDescription())
  }

  return findings
}
