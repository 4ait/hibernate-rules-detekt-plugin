package ru.code4a.detekt.plugin.hibernate.test.rules

import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.TestConfig
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.code4a.detekt.plugin.hibernate.rules.HibernateAssociationsRule
import ru.code4a.detekt.plugin.hibernate.test.extenstions.detekt.lintAllWithContextAndPrint

/**
 * Tests specifically for the collection type validation functionality
 * in HibernateAssociationsRule.
 */
@KotlinCoreEnvironmentTest
class HibernateAssociationsRuleCollectionTypesTest(
  private val env: KotlinCoreEnvironment
) {

  @Test
  fun `should allow standard mutable collections in field declarations`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        interface MutableList<E>
        interface MutableSet<E>
        interface MutableMap<K, V>

        class ArrayList<E> : MutableList<E>
        class LinkedHashSet<E> : MutableSet<E>
        class HashSet<E> : MutableSet<E>
        class HashMap<K, V> : MutableMap<K, V>
        class LinkedHashMap<K, V> : MutableMap<K, V>

        fun <T> mutableListOf(): MutableList<T> = ArrayList()
        fun <T> mutableSetOf(): MutableSet<T> = LinkedHashSet()
        fun <K, V> mutableMapOf(): MutableMap<K, V> = LinkedHashMap()
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import kotlin.collections.*

        @Entity
        class ValidCollectionTypes {
          @OneToMany
          private var mutableListField: MutableList<Child> = mutableListOf()

          @OneToMany
          private var mutableSetField: MutableSet<Child> = mutableSetOf()

          @OneToMany
          private var mutableMapField: MutableMap<String, Child> = mutableMapOf()

          @OneToMany
          private var arrayListField: ArrayList<Child> = ArrayList()

          @OneToMany
          private var linkedHashSetField: LinkedHashSet<Child> = LinkedHashSet()

          @OneToMany
          private var hashSetField: HashSet<Child> = HashSet()

          @OneToMany
          private var hashMapField: HashMap<String, Child> = HashMap()

          @OneToMany
          private var linkedHashMapField: LinkedHashMap<String, Child> = LinkedHashMap()
        }

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, findings.size, "Valid mutable collection types should not produce findings")
  }

  @Test
  fun `should report immutable collections in field declarations`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        interface List<E>
        interface Set<E>
        interface Map<K, V>

        fun <T> listOf(): List<T> = error("")
        fun <T> setOf(): Set<T> = error("")
        fun <K, V> mapOf(): Map<K, V> = error("")
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import kotlin.collections.*

        @Entity
        class ImmutableCollectionTypes {
          @OneToMany
          private var listField: List<Child> = listOf()

          @OneToMany
          private var setField: Set<Child> = setOf()

          @OneToMany
          private var mapField: Map<String, Child> = mapOf()
        }

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(3, findings.size, "Immutable collection types should be reported")
  }

  @Test
  fun `should report custom collection types`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        interface List<E>
        interface MutableList<E> : List<E>
        interface Set<E>
        interface MutableSet<E> : Set<E>

        fun <T> mutableListOf(): MutableList<T> = error("")
        fun <T> mutableSetOf(): MutableSet<T> = error("")
      """.trimIndent(),
      """
        package custom.collections

        import kotlin.collections.MutableList
        import kotlin.collections.MutableSet
        import kotlin.collections.mutableListOf
        import kotlin.collections.mutableSetOf

        class CustomList<T> : MutableList<T> by mutableListOf()
        class CustomSet<T> : MutableSet<T> by mutableSetOf()
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import custom.collections.*

        @Entity
        class CustomCollectionTypes {
          @OneToMany
          private var customListField: CustomList<Child> = CustomList()

          @OneToMany
          private var customSetField: CustomSet<Child> = CustomSet()
        }

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(2, findings.size, "Custom collection types should be reported")
  }

  @Test
  fun `should report violations in constructor parameters`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        interface List<E>
        interface Set<E>
        interface MutableList<E> : List<E>

        class ArrayList<E> : MutableList<E>

        fun <T> listOf(): List<T> = error("")
        fun <T> mutableListOf(): MutableList<T> = ArrayList()
      """.trimIndent(),
      """
        package custom.collections

        import kotlin.collections.MutableList
        import kotlin.collections.mutableListOf

        class CustomList<T> : MutableList<T> by mutableListOf()
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import kotlin.collections.*
        import custom.collections.CustomList

        @Entity
        class ConstructorCollectionTypes(
          @OneToMany
          private var validField: MutableList<Child> = mutableListOf(),

          @OneToMany
          private var immutableField: List<Child> = listOf(),

          @OneToMany
          private var customField: CustomList<Child> = CustomList()
        )

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(2, findings.size, "Invalid types in constructor parameters should be reported")
  }

  @Test
  fun `should only check fields with Hibernate annotations`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
        annotation class Column
      """.trimIndent(),
      """
        package kotlin.collections

        interface List<E>
        interface Set<E>

        fun <T> listOf(): List<T> = error("")
        fun <T> setOf(): Set<T> = error("")
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import javax.persistence.Column
        import kotlin.collections.*

        @Entity
        class MixedAnnotationTypes {
          @OneToMany
          private var oneToManyField: List<Child> = listOf() // Should be reported

          @Column
          private var columnField: List<Child> = listOf() // Should NOT be reported

          private var plainField: Set<Child> = setOf() // Should NOT be reported
        }

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, findings.size, "Only fields with Hibernate annotations should be checked")
  }

  @Test
  fun `should handle Java collections`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        interface MutableCollection<E>
        interface MutableList<E> : MutableCollection<E>
      """.trimIndent(),
      """
        package java.util

        import kotlin.collections.MutableList

        interface List<E>
        class ArrayList<E> : MutableList<E>, List<E>
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import java.util.ArrayList
        import java.util.List

        @Entity
        class JavaCollectionTypes {
          @OneToMany
          private var javaListField: List<Child> = ArrayList() // Immutable Java List, should report

          @OneToMany
          private var javaArrayListField: ArrayList<Child> = ArrayList() // ArrayList, should report
        }

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(2, findings.size, "Java immutable collection should be reported but not ArrayList")
  }

  @Test
  fun `should check both type and original collection rules`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        class List<E>
        class MutableList<E> : List<E> {
            fun add(element: E) {

            }
        }

        fun <T> mutableListOf(): MutableList<T>
        fun <T> listOf(): List<T>
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import kotlin.collections.List
        import kotlin.collections.MutableList

        @Entity
        class CombinedViolations {
          @OneToMany
          private var invalidTypeField: List<Child> = listOf() // Type violation

          @OneToMany
          private var validTypeField: MutableList<Child> = mutableListOf() // Valid type

          fun directMutation(child: Child) {
            validTypeField.add(child) // Direct mutation violation
          }

          fun directReturn(): List<Child> {
            return validTypeField // Direct return violation
          }
        }

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(3, findings.size, "Should detect both type and original collection rule violations")
  }

  @Test
  fun `should check jakarta persistence annotations`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package jakarta.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        interface List<E>

        fun <T> listOf(): List<T> = error("")
      """.trimIndent(),
      """
        import jakarta.persistence.Entity
        import jakarta.persistence.OneToMany
        import kotlin.collections.*

        @Entity
        class JakartaPersistenceEntity {
          @OneToMany
          private var invalidField: List<Child> = listOf() // Should be reported
        }

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, findings.size, "Jakarta persistence annotations should also be checked")
  }

  @Test
  fun `should check ManyToMany annotation`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class ManyToMany
      """.trimIndent(),
      """
        package kotlin.collections

        interface List<E>

        fun <T> listOf(): List<T> = error("")
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.ManyToMany
        import kotlin.collections.*

        @Entity
        class ManyToManyEntity {
          @ManyToMany
          private var invalidField: List<Child> = listOf() // Should be reported
        }

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig(
        "active" to "true"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, findings.size, "ManyToMany annotations should also be checked")
  }
}
