package ru.code4a.detekt.plugin.hibernate.test.rules

import io.gitlab.arturbosch.detekt.rules.KotlinCoreEnvironmentTest
import io.gitlab.arturbosch.detekt.test.TestConfig
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import ru.code4a.detekt.plugin.hibernate.rules.RequireEntityRegistrationRule
import ru.code4a.detekt.plugin.hibernate.test.extenstions.detekt.lintAllWithContextAndPrint

@KotlinCoreEnvironmentTest
class RequireEntityRegistrationRuleTest(
  private val env: KotlinCoreEnvironment
) {
  @Test
  fun `should report when entity is created without registration call`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserService {
          fun createUser(name: String) {
            val user = User(name) // Violation: Entity created without registration
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `should not report when entity is created and registered`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserService {
          fun createUser(name: String) {
            val user = User(name)
            EntityTracker.register(user) // Correct: Entity registered after creation
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should report when entity is created and registered with wrong method`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }

            fun wrongMethod(entity: Any) {
              // Some other logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserService {
          fun createUser(name: String) {
            val user = User(name)
            EntityTracker.wrongMethod(user) // Violation: Wrong method called
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `should not report when non-entity object is created`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        // No @Entity annotation
        class User(val name: String)

        class UserService {
          fun createUser(name: String) {
            val user = User(name) // No violation: Not an entity
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should not report for entities created with custom annotation`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package org.hibernate.annotations

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import org.hibernate.annotations.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserService {
          fun createUser(name: String) {
            val user = User(name) // No violation: Not using the configured entity annotation
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register",
        "entityAnnotation" to "javax.persistence.Entity" // Only check this annotation
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should report when entity is created in a variable and not registered`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserService {
          fun createUser(name: String) {
            val user = User(name) // Violation: Entity created but not registered
            doSomethingElse()
          }

          private fun doSomethingElse() {}
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `should not report when entity is created inline and registered`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserService {
          fun createUser(name: String) {
            EntityTracker.register(User(name)) // Correct: Entity registered inline
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should report when entity is created and another method is called on it before registration`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String) {
          fun initialize() {}
        }

        class UserService {
          fun createUser(name: String) {
            val user = User(name)
            user.initialize() // Entity is used before registration
            EntityTracker.register(user) // Registration happens too late
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `should report when entity is created in a loop without registration`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserService {
          fun createUsers(names: List<String>) {
            for (name in names) {
              val user = User(name) // Violation: Entity created in loop but not registered
              saveToDatabase(user)
            }
          }

          private fun saveToDatabase(user: User) {}
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `should not report when entity is created in a loop and registered`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserService {
          fun createUsers(names: List<String>) {
            for (name in names) {
              val user = User(name)
              EntityTracker.register(user) // Correct: Entity registered after creation in loop
              saveToDatabase(user)
            }
          }

          private fun saveToDatabase(user: User) {}
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should work with custom method configuration`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package org.app.tracking

        class AuditManager {
          companion object {
            fun trackEntity(entity: Any) {
              // Tracking logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import org.app.tracking.AuditManager

        @Entity
        class User(val name: String)

        class UserService {
          fun createUser(name: String) {
            val user = User(name)
            AuditManager.trackEntity(user) // Correct: Using configured method name
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "org.app.tracking.AuditManager.Companion.trackEntity" // Custom method
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should report when entity is created in an if-else branch without registration`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserService {
          fun createUserConditionally(name: String?, createDefault: Boolean) {
            if (name != null) {
              val user = User(name)
              EntityTracker.register(user) // Correct: Entity registered
            } else if (createDefault) {
              val defaultUser = User("default") // Violation: Entity created without registration
            }
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `should check entity registration when created inside nested functions`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserService {
          fun processUserData(name: String) {
            fun createUserInternally(): User {
              val user = User(name) // Violation: Entity created without registration
              return user
            }

            val user = createUserInternally()
            // Missing registration here
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `should not report when entity creation is wrapped in a function that handles registration`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserFactory {
          fun createUser(name: String): User {
            val user = User(name)
            EntityTracker.register(user)
            return user
          }
        }

        class UserService(private val factory: UserFactory) {
          fun processUser(name: String) {
            val user = factory.createUser(name) // No violation: Factory handles registration
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should not report when entity creation is wrapped factory`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserFactory {
          fun createUser(name: String): User {
            return EntityTracker.register(User(name))
          }
        }

        class UserService(private val factory: UserFactory) {
          fun processUser(name: String) {
            val user = factory.createUser(name) // No violation: Factory handles registration
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should not report when entity creation is wrapped`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserFactory {
          fun createUser(name: String): User {
            return EntityTracker.register(User(name))
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should not report when entity creation with extention factory`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example.EntityTracker

        fun Any.register() {
          // Registration logic
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker.register

        @Entity
        class User(val name: String)

        class UserFactory {
          fun createUser(name: String): User {
            return User(name).register()
          }
        }

        class UserService(private val factory: UserFactory) {
          fun processUser(name: String) {
            val user = factory.createUser(name) // No violation: Factory handles registration
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should not report when entity creation with extention`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example.EntityTracker

        fun Any.register() {
          // Registration logic
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker.register

        @Entity
        class User(val name: String)

        class UserFactory {
          fun createUser(name: String): User {
            return User(name).register()
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should report when entity is created with multiple constructors and not registered`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User {
          constructor(name: String) {}
          constructor(name: String, email: String) {}
        }

        class UserService {
          fun createUsers() {
            val user1 = User("John") // Violation: First constructor
            val user2 = User("Jane", "jane@example.com") // Violation: Second constructor
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(2, finding.size)
  }

  @Test
  fun `should handle entity creation with custom factory method`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User private constructor(val name: String) {
          companion object {
            fun create(name: String): User {
              val user = User(name)
              EntityTracker.register(user) // Registration handled in factory method
              return user
            }
          }
        }

        class UserService {
          fun createUser(name: String) {
            val user = User.create(name) // No violation: Factory method handles registration
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should report when entity is created in a try-catch block without registration`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserService {
          fun createUserSafely(name: String) {
            try {
              val user = User(name) // Violation: Entity created without registration
              processUser(user)
            } catch (e: Exception) {
              handleError(e)
            }
          }

          private fun processUser(user: User) {}
          private fun handleError(e: Exception) {}
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `should not report when entity is created and registered in try-catch block`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserService {
          fun createUserSafely(name: String) {
            try {
              val user = User(name)
              EntityTracker.register(user) // Correct: Entity registered in try block
              processUser(user)
            } catch (e: Exception) {
              handleError(e)
            }
          }

          private fun processUser(user: User) {}
          private fun handleError(e: Exception) {}
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `should report when entity is created as part of a data structure and not registered`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserService {
          fun createUserList(names: List<String>): List<User> {
            return names.map { name ->
              User(name) // Violation: Entities created in map without registration
            }
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `should not report when entity is created as part of a data structure and registered`() {
    @Language("kotlin")
    val fileContents = listOf(
      """
        package javax.persistence

        annotation class Entity
      """.trimIndent(),
      """
        package com.example

        class EntityTracker {
          companion object {
            fun register(entity: Any) {
              // Registration logic
            }
          }
        }
      """.trimIndent(),
      """
        import javax.persistence.Entity
        import com.example.EntityTracker

        @Entity
        class User(val name: String)

        class UserService {
          fun createUserList(names: List<String>): List<User> {
            return names.map { name ->
              val user = User(name)
              EntityTracker.register(user) // Correct: Entity registered after creation
              user
            }
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.Companion.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }
}
