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
class HibernateAssociationsRuleNullabilityTest(
  private val env: KotlinCoreEnvironment
) {
  @Test
  fun `should report error for nullable OneToMany collection in property`() {
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
              private var children: MutableList<Child>? = mutableListOf()
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
    Assertions.assertEquals("Hibernate collection property cannot be nullable.", finding[0].message)
  }

  @Test
  fun `should report error for nullable ManyToMany collection in property`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class ManyToMany
        """.trimIndent(),
        """
          import javax.persistence.Entity
          import javax.persistence.ManyToMany

          @Entity
          class Student {
              @ManyToMany
              private var courses: Set<Course>? = setOf()
          }

          @Entity
          class Course
        """.trimIndent()
      )

    val finding =
      HibernateAssociationsRule(
        TestConfig(
          Pair("active", "true")
        )
      ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
    Assertions.assertEquals("Hibernate collection property cannot be nullable.", finding[0].message)
  }

  @Test
  fun `should report error for nullable OneToMany collection in constructor property`() {
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
          class Parent(
              @OneToMany
              private var children: List<Child>? = listOf()
          )

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
    Assertions.assertEquals("Hibernate collection property cannot be nullable.", finding[0].message)
  }

  @Test
  fun `should report error for nullable ManyToMany collection in constructor property`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class ManyToMany
        """.trimIndent(),
        """
          import javax.persistence.Entity
          import javax.persistence.ManyToMany

          @Entity
          class Student(
              @ManyToMany
              private var courses: Set<Course>? = setOf()
          )

          @Entity
          class Course
        """.trimIndent()
      )

    val finding =
      HibernateAssociationsRule(
        TestConfig(
          Pair("active", "true")
        )
      ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
    Assertions.assertEquals("Hibernate collection property cannot be nullable.", finding[0].message)
  }

  @Test
  fun `should not report error for nullable ManyToMany collection in constructor property`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class ManyToMany
        """.trimIndent(),
        """
          import javax.persistence.Entity
          import javax.persistence.ManyToMany

          @Entity
          class Student(
              @ManyToMany
              private var courses: Set<Course> = setOf()
          )

          @Entity
          class Course
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
  fun `should report error for nullable collection with jakarta persistence`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package jakarta.persistence

          annotation class Entity
          annotation class OneToMany
        """.trimIndent(),
        """
          import jakarta.persistence.Entity
          import jakarta.persistence.OneToMany

          @Entity
          class Parent {
              @OneToMany
              private var children: List<Child>? = listOf()
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
    Assertions.assertEquals("Hibernate collection property cannot be nullable.", finding[0].message)
  }

  @Test
  fun `should not report error for non-nullable collections`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class OneToMany
          annotation class ManyToMany
        """.trimIndent(),
        """
          import javax.persistence.Entity
          import javax.persistence.OneToMany
          import javax.persistence.ManyToMany

          @Entity
          class Parent {
              @OneToMany
              private var children: MutableList<Child> = mutableListOf()

              @ManyToMany
              private var relatedEntities: Set<Related> = setOf()
          }

          @Entity
          class Child

          @Entity
          class Related
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
  fun `should not report error for collections in non-Entity classes`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class OneToMany
          annotation class ManyToMany
        """.trimIndent(),
        """
          import javax.persistence.OneToMany
          import javax.persistence.ManyToMany

          class NonEntity {
              @OneToMany
              private var children: MutableList<Child>? = mutableListOf()

              @ManyToMany
              private var relatedEntities: Set<Related>? = setOf()
          }

          class Child
          class Related
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
  fun `should not report error for non-collection nullable associations`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class ManyToOne
          annotation class OneToOne
        """.trimIndent(),
        """
          import javax.persistence.Entity
          import javax.persistence.ManyToOne
          import javax.persistence.OneToOne

          @Entity
          class Child {
              @ManyToOne
              private var parent: Parent? = null

              @OneToOne
              private var related: Related? = null
          }

          @Entity
          class Parent

          @Entity
          class Related
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
  fun `should report multiple errors in the same class`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class OneToMany
          annotation class ManyToMany
        """.trimIndent(),
        """
          import javax.persistence.Entity
          import javax.persistence.OneToMany
          import javax.persistence.ManyToMany

          @Entity
          class ComplexEntity {
              @OneToMany
              private var children: List<Child>? = listOf()

              @ManyToMany
              private var partners: Set<Partner>? = setOf()
          }

          @Entity
          class Child

          @Entity
          class Partner
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
  fun `should report error in mix of constructor and body properties`() {
    @Language("kotlin")
    val fileContents =
      listOf(
        """
          package javax.persistence

          annotation class Entity
          annotation class OneToMany
          annotation class ManyToMany
        """.trimIndent(),
        """
          import javax.persistence.Entity
          import javax.persistence.OneToMany
          import javax.persistence.ManyToMany

          @Entity
          class MixedEntity(
              @OneToMany
              private var children: List<Child>? = listOf()
          ) {
              @ManyToMany
              private var partners: Set<Partner>? = setOf()
          }

          @Entity
          class Child

          @Entity
          class Partner
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
}
