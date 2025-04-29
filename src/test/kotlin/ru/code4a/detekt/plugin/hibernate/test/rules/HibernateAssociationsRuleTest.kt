package ru.code4a.detekt.plugin.hibernate.test.rules

import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.TestConfig
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.code4a.detekt.plugin.hibernate.rules.HibernateAssociationsRule
import ru.code4a.detekt.plugin.hibernate.test.extenstions.detekt.lintAllWithContextAndPrint

@KotlinCoreEnvironmentTest
class HibernateAssociationsRuleTest(
  private val env: KotlinCoreEnvironment
) {
  @Test
  fun `should not process some methods`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class OneToMany
        """.trimIndent(),
        """
          package kotlin.collections

          class ArrayList
        """.trimIndent(),
        """
               import javax.persistence.Entity
               import javax.persistence.OneToMany
               import kotlin.collections.ArrayList

                @Entity
                class Parent {
                    @OneToMany
                    private val children: MutableList<Child> = mutableListOf()

                    @OneToMany
                    private val childrenSet: MutableSet<Child> = mutableSetOf()

                    fun addChild(child: Child) {
                        children.add(child) // INCORRECT: modification collection
                    }

                    fun getChildren(): List<Child> {
                        return children // INCORRECT direct return
                    }

                    fun getChildrenSet(): Set<Child> {
                       return childrenSet.toSet()
                    }

                    fun correctAddChild(child: Child) {
                        val copy = ArrayList(children)
                        copy.add(child)
                    }

                    fun correctGetChildren(): List<Child> {
                        return ArrayList(children) // Correct: returned copy
                    }
                }

                @Entity
                class Child
        """.trimIndent()
      )

    val finding =
      HibernateAssociationsRule(
        TestConfig(
          Pair("active", "true")
        )
      ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(2, finding.size)
  }

  @Test
  fun `direct set to val is not allowed`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class OneToMany
        """.trimIndent(),
        """
               import javax.persistence.Entity
               import javax.persistence.OneToMany

                @Entity
                class Parent {
                    @OneToMany
                    private val children: MutableList<Child> = mutableListOf()

                    fun notAllowed() {
                        val a = children
                    }
                }

                @Entity
                class Child
        """.trimIndent()
      )

    val finding =
      HibernateAssociationsRule(
        TestConfig(
          Pair("active", "true")
        )
      ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `should not allow dirty map hack`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class OneToMany
        """.trimIndent(),
        """
               import javax.persistence.Entity
               import javax.persistence.OneToMany

                @Entity
                class Parent {
                    @OneToMany
                    private val children: MutableList<Child> = mutableListOf()

                    fun notAllowed() {
                        val test = mutableListOf(1).map { children }
                    }
                }

                @Entity
                class Child
        """.trimIndent()
      )

    val finding =
      HibernateAssociationsRule(
        TestConfig(
          Pair("active", "true")
        )
      ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `should allow dirty map hack with copy`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class OneToMany
        """.trimIndent(),
        """
          package kotlin.collections

          class ArrayList
        """.trimIndent(),
        """
               import javax.persistence.Entity
               import javax.persistence.OneToMany
               import kotlin.collections.ArrayList

                @Entity
                class Parent {
                    @OneToMany
                    private val children: MutableList<Child> = mutableListOf()

                    fun notAllowed() {
                        val test = mutableListOf(1).map { ArrayList(children) }
                    }
                }

                @Entity
                class Child
        """.trimIndent()
      )

    val finding =
      HibernateAssociationsRule(
        TestConfig(
          Pair("active", "true")
        )
      ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should not allow dirty map hack with unknown extention`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class OneToMany
        """.trimIndent(),
        """
          package kotlin.collections

          class ArrayList
        """.trimIndent(),
        """
               package custom.util

               fun <T> Collection<T>.unsafeCopy(): List<T> {
                  return toList() // Safe internally, but not whitelisted
                }
        """.trimIndent(),
        """
               import javax.persistence.Entity
               import javax.persistence.OneToMany
               import kotlin.collections.ArrayList
               import custom.util.unsafeCopy

                @Entity
                class Parent {
                    @OneToMany
                    private val children: MutableSet<Child> = mutableSetOf()

                    fun notAllowed() {
                        val test = mutableListOf(1).map { children.unsafeCopy() }
                    }
                }

                @Entity
                class Child
        """.trimIndent()
      )

    val finding =
      HibernateAssociationsRule(
        TestConfig(
          Pair("active", "true")
        )
      ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(2, finding.size)
  }

  @Test
  fun `should not allow direct set reference`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class ManyToOne
        """.trimIndent(),
        """
               import javax.persistence.Entity
               import javax.persistence.ManyToOne

                @Entity
                class Parent {
                    @ManyToOne
                    private var children: Child? = null

                    fun notAllowed(child: Child) {
                        children = child
                    }
                }

                @Entity
                class Child
        """.trimIndent()
      )

    val finding =
      HibernateAssociationsRule(
        TestConfig(
          Pair("active", "true")
        )
      ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `should not allow direct set reference with this`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class ManyToOne
        """.trimIndent(),
        """
               import javax.persistence.Entity
               import javax.persistence.ManyToOne

                @Entity
                class Parent {
                    @ManyToOne
                    private var children: Child? = null

                    fun notAllowed(child: Child) {
                        this.children = child
                    }
                }

                @Entity
                class Child
        """.trimIndent()
      )

    val finding =
      HibernateAssociationsRule(
        TestConfig(
          Pair("active", "true")
        )
      ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `should allow dirty map hack with allowed extention`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class OneToMany
        """.trimIndent(),
        """
          package kotlin.collections

          class ArrayList

            fun <T> Collection<T>.toSet(): List<T> {
              return toList() // Safe internally, but not whitelisted
            }
        """.trimIndent(),
        """
               import javax.persistence.Entity
               import javax.persistence.OneToMany
               import kotlin.collections.toSet

                @Entity
                class Parent {
                    @OneToMany
                    private val children: MutableSet<Child> = mutableSetOf()

                    fun allowed() {
                        val test = mutableListOf(1).map { children.toSet() }
                    }
                }

                @Entity
                class Child
        """.trimIndent()
      )

    val finding =
      HibernateAssociationsRule(
        TestConfig(
          Pair("active", "true")
        )
      ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should report when non-whitelisted custom classes are used`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class OneToMany
        """.trimIndent(),
        """
               package custom.util

               class UnsafeCopier {
                  companion object {
                    fun <T> unsafeCopy(collection: Collection<T>): List<T> {
                      return collection.toList() // Safe internally, but not whitelisted
                    }
                  }
               }
        """.trimIndent(),
        """
               import javax.persistence.Entity
               import javax.persistence.OneToMany
               import custom.util.UnsafeCopier

                @Entity
                class Parent {
                    @OneToMany
                    private val children: MutableList<Child> = mutableListOf()

                    // Using non-whitelisted custom util method
                    fun customUnsafeCopy(): List<Child> {
                        return UnsafeCopier.unsafeCopy(children) // Should be reported without whitelist
                    }
                }

                @Entity
                class Child
        """.trimIndent()
      )

    val finding =
      HibernateAssociationsRule(
        TestConfig(
          Pair("active", "true")
        )
      ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size) // Should report 1 violation
  }

  @Test
  fun `should support custom whitelist configuration`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class OneToMany
        """.trimIndent(),
        """
               package custom.util

               class SafeCopier {
                  companion object {
                    fun <T> safeCopy(collection: Collection<T>): List<T> {
                      return ArrayList(collection)
                    }
                  }
               }
        """.trimIndent(),
        """
               import javax.persistence.Entity
               import javax.persistence.OneToMany
               import custom.util.SafeCopier

                @Entity
                class Parent {
                    @OneToMany
                    private val children: MutableList<Child> = mutableListOf()

                    // Using custom util method which should be whitelisted
                    fun customSafeCopy(): List<Child> {
                        return SafeCopier.safeCopy(children)
                    }
                }

                @Entity
                class Child
        """.trimIndent()
      )

    // Configure the rule with custom whitelist entry
    val finding =
      HibernateAssociationsRule(
        TestConfig(
          "active" to "true",
          HibernateAssociationsRule.ADDITIONAL_SAFE_OPERATIONS to listOf("custom.util.SafeCopier.Companion.safeCopy")
        )
      ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size) // Should find no violations with custom whitelist
  }

  @Test
  fun `should allow all safe operations`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class OneToMany
        """.trimIndent(),
        """
          package kotlin.collections

          class ArrayList
        """.trimIndent(),
        """
          package java.util

          class LinkedList
        """.trimIndent(),
        """
               import javax.persistence.Entity
               import javax.persistence.OneToMany
               import java.util.LinkedList

                @Entity
                class Parent {
                    @OneToMany
                    private val children: MutableList<Child> = mutableListOf()

                    // Allowed operations with constructors
                    fun correctAddChildWithArrayList(child: Child) {
                        val copy = ArrayList(children)
                        copy.add(child)
                    }

                    fun correctAddChildWithLinkedList(child: Child) {
                        val copy = LinkedList(children)
                        copy.add(child)
                    }

                    // Allowed operations with extension functions
                    fun correctGetChildrenWithToList(): List<Child> {
                        return children.toList() // Correct: returned copy
                    }

                    fun correctGetChildrenWithToMutableList(): MutableList<Child> {
                        return children.toMutableList() // Correct: returned copy
                    }

                    // Allowed operations with parameter passing
                    fun passToFunction() {
                        processChildren(ArrayList(children))
                        processChildren(children.toList())
                    }

                    // Allowed operations with variable assignment
                    fun correctAssignment() {
                        val copy1 = children.toList()
                        val copy2 = ArrayList(children)
                        val copy3 = children.toMutableList()
                    }

                    // Allowed read-only operations
                    fun readOps() {
                        children.forEach { it.process() }
                        val count = children.count()
                        val anyMatch = children.any { it.isActive }
                        val first = children.firstOrNull()
                        val index = children.indexOf(first)
                    }

                    // Allowed transforming operations
                    fun transformOps(): List<String> {
                        return children.map { it.name }
                    }

                    fun filterOps(): List<Child> {
                        return children.filter { it.isActive }
                    }

                    private fun processChildren(list: List<Child>) {
                        // Some processing
                    }
                }

                @Entity
                class Child {
                    val name: String = ""
                    val isActive: Boolean = true

                    fun process() {
                        // Some processing
                    }
                }
        """.trimIndent()
      )

    val finding =
      HibernateAssociationsRule(
        TestConfig(
          Pair("active", "true")
        )
      ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size) // Should find no violations
  }
}
