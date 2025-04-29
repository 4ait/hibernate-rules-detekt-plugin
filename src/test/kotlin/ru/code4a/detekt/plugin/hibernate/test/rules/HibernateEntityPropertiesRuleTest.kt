package ru.code4a.detekt.plugin.hibernate.test.rules

import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import ru.code4a.detekt.plugin.hibernate.rules.HibernateEntityPropertiesRule
import io.gitlab.arturbosch.detekt.test.TestConfig
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.code4a.detekt.plugin.hibernate.test.extenstions.detekt.lintAllWithContextAndPrint

@KotlinCoreEnvironmentTest
class HibernateEntityPropertiesRuleTest(
  private val env: KotlinCoreEnvironment
) {
  @Test
  fun `properties in entity should be private var`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
            package javax.persistence

            annotation class Entity
        """.trimIndent(), """
            import javax.persistence.Entity

            @Entity
            class User {
                private var id: Long = 0          // Correct
                private var name: String = ""      // Correct
                var address: String = ""          // Incorrect: not private
                private val email: String = ""     // Incorrect: not var
                val phone: String = ""            // Incorrect: not private and not var
            }
        """.trimIndent()
    )

    val findings =
      HibernateEntityPropertiesRule(
        TestConfig(
          "active" to "true"
        )
      ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(3, findings.size)
    // Check that non-private property is reported
    Assertions.assertTrue(findings.any { it.message.contains("address") && it.message.contains("must be private") })
    // Check that val property is reported
    Assertions.assertTrue(findings.any { it.message.contains("email") && it.message.contains("must be var") })
    // Check that both issues are reported for non-private val
    Assertions.assertTrue(findings.any {
      it.message.contains("phone") &&
        it.message.contains("must be private") &&
        it.message.contains("must be var")
    })
  }

  @Test
  fun `properties in non-entity class should not be reported`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
            package javax.persistence

            annotation class Entity
        """.trimIndent(), """
            import javax.persistence.Entity

            // No @Entity annotation here
            class RegularClass {
                var id: Long = 0                  // Should not be reported
                val name: String = ""              // Should not be reported
                public var address: String = ""    // Should not be reported
            }
        """.trimIndent()
    )

    val findings = HibernateEntityPropertiesRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, findings.size)
  }

  @Test
  fun `companion object properties should be ignored`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
            package javax.persistence

            annotation class Entity
        """.trimIndent(), """
            import javax.persistence.Entity

            @Entity
            class User {
                private var id: Long = 0          // Correct

                companion object {
                    val TABLE_NAME = "users"       // Should be ignored
                    public const val ID_COLUMN = "id" // Should be ignored
                }
            }
        """.trimIndent()
    )

    val findings = HibernateEntityPropertiesRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, findings.size)
  }

  @Test
  fun `properties with different visibility modifiers`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
            package javax.persistence

            annotation class Entity
        """.trimIndent(), """
            import javax.persistence.Entity

            @Entity
            class User {
                private var id: Long = 0          // Correct
                public var name: String = ""       // Incorrect: not private
                protected var email: String = ""   // Incorrect: not private
                internal var phone: String = ""    // Incorrect: not private
                var address: String = ""          // Incorrect: not private (implicit public)
            }
        """.trimIndent()
    )

    val findings = HibernateEntityPropertiesRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(4, findings.size)
    Assertions.assertTrue(findings.any { it.message.contains("name") && it.message.contains("must be private") })
    Assertions.assertTrue(findings.any { it.message.contains("email") && it.message.contains("must be private") })
    Assertions.assertTrue(findings.any { it.message.contains("phone") && it.message.contains("must be private") })
    Assertions.assertTrue(findings.any { it.message.contains("address") && it.message.contains("must be private") })
  }

  @Test
  fun `entity with all correct properties`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
            package javax.persistence

            annotation class Entity
        """.trimIndent(), """
            import javax.persistence.Entity

            @Entity
            class PerfectEntity {
                private var id: Long = 0
                private var name: String = ""
                private var createdAt: Long = 0

                companion object {
                    const val ID_COLUMN = "id"
                }
            }
        """.trimIndent()
    )

    val findings = HibernateEntityPropertiesRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, findings.size)
  }

  @Test
  fun `multiple entity classes in one file`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
            package javax.persistence

            annotation class Entity
        """.trimIndent(), """
            import javax.persistence.Entity

            @Entity
            class User {
                private var id: Long = 0
                val name: String = ""  // Incorrect: not var
            }

            @Entity
            class Address {
                private var id: Long = 0
                var street: String = ""  // Incorrect: not private
            }

            // Not an entity, should be ignored
            class Helper {
                val util: String = ""
            }
        """.trimIndent()
    )

    val findings = HibernateEntityPropertiesRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(2, findings.size)
    Assertions.assertTrue(findings.any { it.message.contains("name") && it.message.contains("must be var") })
    Assertions.assertTrue(findings.any { it.message.contains("street") && it.message.contains("must be private") })
  }

  @Test
  fun `entity with different property types`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
            package javax.persistence

            annotation class Entity

            class CustomType
        """.trimIndent(), """
            import javax.persistence.Entity
            import javax.persistence.CustomType

            @Entity
            class ComplexEntity {
                private var id: Long = 0                     // Correct
                private var names: List<String> = listOf()    // Correct
                private var data: Map<String, Int> = mapOf()  // Correct
                private var custom: CustomType? = null        // Correct
                val items: Set<String> = setOf()             // Incorrect: not var
            }
        """.trimIndent()
    )

    val findings = HibernateEntityPropertiesRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, findings.size)
    Assertions.assertTrue(findings.any { it.message.contains("items") && it.message.contains("must be var") })
  }

  @Test
  fun `inactive rule should not report anything`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
            package javax.persistence

            annotation class Entity
        """.trimIndent(), """
            import javax.persistence.Entity

            @Entity
            class User {
                val id: Long = 0          // Would normally be reported
                public var name: String = ""  // Would normally be reported
            }
        """.trimIndent()
    )

    val findings = HibernateEntityPropertiesRule(
      TestConfig(
        "active" to "false"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, findings.size)
  }
}
