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
class HibernateAssociationsRuleCompleteTest(
  private val env: KotlinCoreEnvironment
) {

  @Test
  fun `test companion object mutations`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        class ArrayList<T>
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import kotlin.collections.ArrayList

        @Entity
        class EntityWithCompanion {
            @OneToMany
            private val children: MutableList<Child> = mutableListOf()

            companion object {
                fun addChild(entity: EntityWithCompanion, child: Child) {
                    entity.children.add(child) // Violation
                }

                fun getChildren(entity: EntityWithCompanion): List<Child> {
                    return entity.children // Violation
                }

                fun addChildCorrect(entity: EntityWithCompanion, child: Child) {
                    val copy = ArrayList(entity.children)
                    copy.add(child)
                }

                fun getChildrenCorrect(entity: EntityWithCompanion): List<Child> {
                    return ArrayList(entity.children) // Correct: returned copy
                }
            }
        }

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig("active" to "true")
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(2, findings.size)
  }

  @Test
  fun `test jakarta persistence annotations`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package jakarta.persistence

        annotation class Entity
        annotation class OneToMany
        annotation class ManyToOne
        annotation class OneToOne
        annotation class ManyToMany
      """.trimIndent(),
      """
        package kotlin.collections

        class ArrayList<T>
      """.trimIndent(),
      """
        import jakarta.persistence.Entity
        import jakarta.persistence.OneToMany
        import jakarta.persistence.ManyToOne
        import kotlin.collections.ArrayList

        @Entity
        class JakartaEntity {
            @OneToMany
            private val children: MutableList<Child> = mutableListOf()

            @ManyToOne
            private lateinit var parent: Parent

            fun violations() {
                children.add(Child()) // Violation
                parent = Parent() // Violation
            }

            fun correct() {
                val copyList = ArrayList(children)
                copyList.add(Child())
            }
        }

        @Entity
        class Child

        @Entity
        class Parent
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig("active" to "true")
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(2, findings.size)
  }

  @Test
  fun `test different collection types`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
        annotation class ManyToMany
      """.trimIndent(),
      """
        package kotlin.collections

        class ArrayList<T>
        class HashSet<T>
        class HashMap<K, V>
      """.trimIndent(),
      """
        package java.util

        class HashMap<K, V>
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import javax.persistence.ManyToMany
        import kotlin.collections.ArrayList
        import kotlin.collections.HashSet
        import kotlin.collections.HashMap

        @Entity
        class CollectionTypes {
            @OneToMany
            private val childrenList: MutableList<Child> = mutableListOf()

            @OneToMany
            private val childrenSet: MutableSet<Child> = mutableSetOf()

            @ManyToMany
            private val childrenMap: MutableMap<String, Child> = mutableMapOf()

            fun violations() {
                childrenList.add(Child()) // Violation
                childrenSet.add(Child()) // Violation
                childrenMap["key"] = Child() // Violation
                childrenMap.put("key", Child()) // Violation
            }

            fun correct() {
                val copyList = ArrayList(childrenList)
                copyList.add(Child())

                val copySet = HashSet(childrenSet)
                copySet.add(Child())

                val copyMap = HashMap(childrenMap)
                copyMap["key"] = Child()
            }
        }

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig("active" to "true")
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(4, findings.size)
  }

  @Test
  fun `test extension functions`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        fun <T> Collection<T>.toList(): List<T> = listOf()
        fun <T> List<T>.toMutableList(): MutableList<T> = mutableListOf()

        fun <T> Collection<T>.forEach(action: (T) -> Unit) {}
        fun <T> Collection<T>.map(transform: (T) -> R): List<R> = listOf()
        fun <T> Collection<T>.filter(predicate: (T) -> Boolean): List<T> = listOf()
      """.trimIndent(),
      """
        package custom.utils

        fun <T> Collection<T>.customCopy(): List<T> = toList()
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import kotlin.collections.toList
        import kotlin.collections.toMutableList
        import kotlin.collections.forEach
        import kotlin.collections.map
        import kotlin.collections.filter
        import custom.utils.customCopy

        @Entity
        class ExtensionFunctionsTest {
            @OneToMany
            private val children: MutableList<Child> = mutableListOf()

            fun standardExtensionsCorrect() {
                // These should be allowed since they are whitelisted
                val copy1 = children.toList()
                val copy2 = children.toMutableList()

                children.forEach { it.process() } // Read-only operation, allowed
                val filtered = children.filter { it.active } // Creates new list, allowed
                val mapped = children.map { it.name } // Creates new list, allowed
            }

            fun customExtensionViolation() {
                // This should be a violation since custom.utils.customCopy is not in whitelist
                val copy = children.customCopy()
            }

            fun directMutationViolation() {
                // This should be a violation since it directly mutates the collection
                children.add(Child())
            }
        }

        @Entity
        class Child {
            val name: String = ""
            val active: Boolean = true

            fun process() {}
        }
      """.trimIndent()
    )

    val findingsStandard = HibernateAssociationsRule(
      TestConfig("active" to "true")
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(2, findingsStandard.size) // One for customCopy and one for direct mutation

    // Now test with custom function whitelisted
    val findingsWithWhitelist = HibernateAssociationsRule(
      TestConfig(
        "active" to "true",
        HibernateAssociationsRule.ADDITIONAL_SAFE_OPERATIONS to listOf("custom.utils.customCopy")
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, findingsWithWhitelist.size) // Only direct mutation should be reported
  }

  @Test
  fun `test object declarations`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        class ArrayList<T>
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import kotlin.collections.ArrayList

        @Entity
        class EntityWithObject {
            @OneToMany
            private val children: MutableList<Child> = mutableListOf()

            // Singleton object accessing entity
            object ChildrenManager {
                fun addChild(entity: EntityWithObject, child: Child) {
                    entity.children.add(child) // Violation
                }

                fun getChildren(entity: EntityWithObject): List<Child> {
                    return entity.children // Violation
                }

                fun addChildCorrect(entity: EntityWithObject, child: Child) {
                    val copy = ArrayList(entity.children)
                    copy.add(child)
                }

                fun getChildrenCorrect(entity: EntityWithObject): List<Child> {
                    return ArrayList(entity.children) // Correct
                }
            }
        }

        // Top-level object
        object GlobalManager {
            fun processEntity(entity: EntityWithObject) {
                entity.processChildren() // This is fine
            }
        }

        @Entity
        class Child

        @Entity
        class EntityWithObject {
            @OneToMany
            private val children: MutableList<Child> = mutableListOf()

            fun processChildren() {
                children.add(Child()) // Violation
            }
        }
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig("active" to "true")
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(3, findings.size)
  }

  @Test
  fun `test inheritance and overrides`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
        annotation class ManyToOne
      """.trimIndent(),
      """
        package kotlin.collections

        class ArrayList<T>
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import javax.persistence.ManyToOne
        import kotlin.collections.ArrayList

        interface EntityOperation<T> {
            fun getItems(): List<T>
            fun addItem(item: T)
        }

        abstract class BaseEntity {
            abstract fun getRelations(): List<Relation>
            abstract fun addRelation(relation: Relation)
        }

        @Entity
        class ConcreteEntity : BaseEntity(), EntityOperation<Child> {
            @OneToMany
            private val children: MutableList<Child> = mutableListOf()

            @OneToMany
            private val relations: MutableList<Relation> = mutableListOf()

            // Implementing interface method incorrectly
            override fun getItems(): List<Child> {
                return children // Violation
            }

            // Implementing interface method correctly
            override fun addItem(item: Child) {
                val copy = ArrayList(children)
                copy.add(item)
                // No direct mutation
            }

            // Overriding abstract method incorrectly
            override fun getRelations(): List<Relation> {
                return relations // Violation
            }

            // Overriding abstract method incorrectly
            override fun addRelation(relation: Relation) {
                relations.add(relation) // Violation
            }
        }

        @Entity
        class Child

        @Entity
        class Relation
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig("active" to "true")
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(3, findings.size)
  }

  @Test
  fun `test lambda and function references`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        class ArrayList<T>

        fun <T> Collection<T>.forEach(action: (T) -> Unit) {}
        fun <T, R> Collection<T>.map(transform: (T) -> R): List<R> = listOf()
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import kotlin.collections.ArrayList
        import kotlin.collections.forEach
        import kotlin.collections.map

        @Entity
        class LambdaAndFunctionReferences {
            @OneToMany
            private val children: MutableList<Child> = mutableListOf()

            fun violations() {
                // Lambda that captures and returns the collection
                val getChildren = { children } // Violation

                // Lambda that captures and modifies the collection
                val addChild = { child: Child -> children.add(child) } // Violation

                // Passing collection to higher-order function
                processItems(children) // Violation

                // Calling function with collection as receiver
                children.process() // Depends on what process() does

                // Method reference to collection
                val childrenSupplier = this::getChildrenDirect // Not a violation itself

                // Anonymous function
                val processer = fun(child: Child) {
                    children.add(child) // Violation
                }
            }

            fun correct() {
                // Lambda that captures and returns a copy
                val getChildrenCopy = { ArrayList(children) }

                // Lambda that works with a copy
                val addChildSafe = { child: Child ->
                    val copy = ArrayList(children)
                    copy.add(child)
                }

                // Passing copy to higher-order function
                processItems(ArrayList(children))

                // Calling safe extension function with collection as receiver
                children.forEach { it.process() }

                // Method reference to safe method
                val safeSupplier = this::getChildrenCopy

                // Anonymous function with copy
                val safeProcesser = fun(child: Child) {
                    val copy = ArrayList(children)
                    copy.add(child)
                }
            }

            fun getChildrenDirect(): List<Child> = children // Violation

            fun getChildrenCopy(): List<Child> = ArrayList(children)

            private fun processItems(items: List<Child>) {
                // Some processing
            }

            private fun MutableList<Child>.process() {
                this.add(Child()) // Would be a violation if called on children
            }
        }

        @Entity
        class Child {
            fun process() {}
        }
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig("active" to "true")
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(7, findings.size)
  }

  @Test
  fun `test destructuring and complex assignments`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        class ArrayList<T>
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import kotlin.collections.ArrayList

        @Entity
        class DestructuringAndComplexAssignments {
            @OneToMany
            private val children: MutableList<Child> = mutableListOf()

            fun violations() {
                // Multiple assignment
                val (first, second) = children // Not a violation - just reading

                // Destructuring in for loop
                for ((index, child) in children.withIndex()) {
                    children[index] = Child() // Violation
                }

                // Complex assignment with +=
                children += listOf(Child(), Child()) // Violation

                // Assignment with elvis
                val items = otherList ?: children // Violation

                // Assignment in try-catch
                val result = try {
                    children // Violation
                } catch (e: Exception) {
                    mutableListOf()
                }
            }

            fun correct() {
                // Safe destructuring by making a copy first
                val copy = ArrayList(children)
                val (first, second) = copy

                // Safe destructuring in for loop
                val copy2 = ArrayList(children)
                for ((index, child) in copy2.withIndex()) {
                    copy2[index] = Child()
                }

                // Safe complex assignment
                val copy3 = ArrayList(children)
                copy3 += listOf(Child(), Child())

                // Safe assignment with elvis
                val items = otherList ?: ArrayList(children)

                // Safe assignment in try-catch
                val result = try {
                    ArrayList(children)
                } catch (e: Exception) {
                    mutableListOf()
                }
            }

            private val otherList: MutableList<Child>? = null
        }

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig("active" to "true")
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(5, findings.size)
  }

  @Test
  fun `test scope functions and DSL-like patterns`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        class ArrayList<T>
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import kotlin.collections.ArrayList

        @Entity
        class ScopeFunctionsAndDSL {
            @OneToMany
            private val children: MutableList<Child> = mutableListOf()

            fun violations() {
                // Using apply - modifies the receiver
                children.apply { // Violation
                    add(Child())
                    removeAt(0)
                }

                // Using also
                children.also { // Violation
                    it.add(Child())
                }

                // Using let
                children.let { // Violation
                    processChildren(it)
                }

                // Using with
                with(children) { // Violation
                    add(Child())
                }

                // Using run
                children.run { // Violation
                    add(Child())
                    this
                }
            }

            fun correct() {
                // Safe apply on a copy
                ArrayList(children).apply {
                    add(Child())
                    removeAt(0)
                }

                // Safe also on a copy
                ArrayList(children).also {
                    it.add(Child())
                }

                // Safe with on a copy
                with(ArrayList(children)) {
                    add(Child())
                }

                // Safe run on a copy
                ArrayList(children).run {
                    add(Child())
                    this
                }
            }

            private fun processChildren(list: List<Child>) {
                // Some processing
            }
        }

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig("active" to "true")
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(5, findings.size)
  }

  @Test
  fun `test nested classes and inner classes`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        class ArrayList<T>
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import kotlin.collections.ArrayList

        @Entity
        class OuterEntity {
            @OneToMany
            private val children: MutableList<Child> = mutableListOf()

            // Nested class (static inner class in Java terms)
            class NestedClass {
                fun processOuterChildren(outer: OuterEntity) {
                    outer.children.add(Child()) // Violation
                }

                fun processSafely(outer: OuterEntity) {
                    val copy = ArrayList(outer.children)
                    copy.add(Child())
                }
            }

            // Inner class (has access to outer class members)
            inner class InnerClass {
                fun processChildren() {
                    children.add(Child()) // Violation
                }

                fun processSafely() {
                    val copy = ArrayList(children)
                    copy.add(Child())
                }
            }

            // Local class inside a method
            fun methodWithLocalClass() {
                class LocalClass {
                    fun processChildren() {
                        children.add(Child()) // Violation
                    }

                    fun processSafely() {
                        val copy = ArrayList(children)
                        copy.add(Child())
                    }
                }

                val local = LocalClass()
                local.processChildren() // This triggers the violation
            }
        }

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig("active" to "true")
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(3, findings.size)
  }

  @Test
  fun `test property delegates and lazy initialization`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        class ArrayList<T>
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import kotlin.collections.ArrayList

        @Entity
        class PropertyDelegatesAndLazy {
            @OneToMany
            private val children: MutableList<Child> = mutableListOf()

            // Lazy property that returns the collection directly
            val lazyChildren by lazy { children } // Violation when accessed

            // Lazy property that returns a copy - safe
            val lazyChildrenCopy by lazy { ArrayList(children) }

            // Property with custom getter returning collection directly
            val customGetterChildren: List<Child>
                get() = children // Violation when accessed

            // Property with custom getter returning a copy - safe
            val customGetterChildrenCopy: List<Child>
                get() = ArrayList(children)

            fun useChildren() {
                val lazy1 = lazyChildren
                val lazy2 = lazyChildrenCopy
                val getter1 = customGetterChildren
                val getter2 = customGetterChildrenCopy
            }
        }

        @Entity
        class Child
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig("active" to "true")
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(2, findings.size)
  }

  @Test
  fun `test when expressions and if expressions`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
        annotation class OneToMany
      """.trimIndent(),
      """
        package kotlin.collections

        class ArrayList<T>
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import javax.persistence.OneToMany
        import kotlin.collections.ArrayList

        @Entity
        class ConditionalExpressions {
            @OneToMany
            private val children: MutableList<Child> = mutableListOf()

            @OneToMany
            private val tasks: MutableList<Task> = mutableListOf()

            fun violations() {
                // When expression returning collection directly
                val result1 = when {
                    isActive() -> children // Violation
                    else -> tasks // Violation
                }

                // If expression returning collection directly
                val result2 = if (isActive()) children else tasks // Two violations

                // When used in a function call
                processItems(when {
                    isActive() -> children // Violation
                    else -> emptyList()
                })
            }

            fun correct() {
                // When expression returning copies
                val result1 = when {
                    isActive() -> ArrayList(children)
                    else -> ArrayList(tasks)
                }

                // If expression returning copies
                val result2 = if (isActive()) ArrayList(children) else ArrayList(tasks)

                // When used safely in a function call
                processItems(when {
                    isActive() -> ArrayList(children)
                    else -> emptyList()
                })
            }

            private fun isActive(): Boolean = true

            private fun processItems(items: List<Any>) {
                // Some processing
            }
        }

        @Entity
        class Child

        @Entity
        class Task
      """.trimIndent()
    )

    val findings = HibernateAssociationsRule(
      TestConfig("active" to "true")
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(6, findings.size)
  }
}
