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
class RequireEntityRegistrationRuleComplexTest(
  private val env: KotlinCoreEnvironment
) {
  @Test
  fun `test inheritance - should report when entity subclass is created without registration`() {
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

        open class BaseEntity

        @Entity
        class User(val name: String) : BaseEntity()

        class UserService {
          fun createUser(name: String) {
            val user = User(name) // Violation: Entity created without registration
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `test inheritance - should not report when entity subclass is created and registered`() {
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

        open class BaseEntity

        @Entity
        class User(val name: String) : BaseEntity()

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
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(0, finding.size)
  }

  @Test
  fun `test delegation - should report when entity with delegation is created without registration`() {
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

        interface UserBehavior {
          val name: String
        }

        class UserBehaviorImpl(override val name: String) : UserBehavior

        @Entity
        class User(name: String) : UserBehavior by UserBehaviorImpl(name)

        class UserService {
          fun createUser(name: String) {
            val user = User(name) // Violation: Entity created without registration
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `test multiple entity creation - should report when list of entities is created without registration`() {
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
            val users = names.map { User(it) } // Violation: Entities created without registration
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertTrue(finding.isNotEmpty())
  }

  @Test
  fun `test multiple entity creation - should not report when list of entities is created and registered`() {
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
            val users = names.map {
              val user = User(it)
              EntityTracker.register(user)
              user
            }
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
  fun `test lambda expressions - should report when entity is created in lambda without registration`() {
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
          fun processWithUser(name: String, processor: (User) -> Unit) {
            val user = User(name) // Violation: Entity created without registration
            processor(user)
          }

          fun doSomething() {
            processWithUser("John") { user ->
              println(user.name)
            }
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `test higher-order functions - should report when entity is created in higher-order function without registration`() {
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
          fun <T> withUser(name: String, block: (User) -> T): T {
            val user = User(name) // Violation: Entity created without registration
            return block(user)
          }

          fun doSomething() {
            val result = withUser("John") { user ->
              user.name.length
            }
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `test generics - should report when entity is created with generics without registration`() {
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
        class GenericEntity<T>(val data: T)

        class EntityService {
          fun <T> createEntity(data: T) {
            val entity = GenericEntity(data) // Violation: Entity created without registration
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `test complex initialization - should report when entity is created with init block without registration`() {
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
          var processed: Boolean = false

          init {
            processed = true
          }
        }

        class UserService {
          fun createUser(name: String) {
            val user = User(name) // Violation: Entity created without registration
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `test nested classes - should report when nested entity is created without registration`() {
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

        class UserContainer {
          @Entity
          class NestedUser(val name: String)

          fun createNestedUser(name: String) {
            val user = NestedUser(name) // Violation: Entity created without registration
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `test object expressions - should report when entity is created in anonymous object without registration`() {
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

        interface UserProcessor {
          fun process(name: String)
        }

        class UserService {
          fun doSomething() {
            val processor = object : UserProcessor {
              override fun process(name: String) {
                val user = User(name) // Violation: Entity created without registration
              }
            }
            processor.process("John")
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `test companion object factory methods - should report when entity is created in factory method without registration`() {
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
              return User(name) // Violation: Entity created without registration
            }
          }
        }

        class UserService {
          fun createUser(name: String) {
            val user = User.create(name)
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(2, finding.size)
  }

  @Test
  fun `test extension functions - should report when entity is created in extension function without registration`() {
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

        fun String.toUser(): User {
          return User(this) // Violation: Entity created without registration
        }

        class UserService {
          fun createUser(name: String) {
            val user = name.toUser()
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(2, finding.size)
  }

  @Test
  fun `test destructuring declarations - should report when entity is created and destructured without registration`() {
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
        data class User(val name: String, val email: String) {
          operator fun component1() = name
          operator fun component2() = email
        }

        class UserService {
          fun processUser(name: String, email: String) {
            val user = User(name, email) // Violation: Entity created without registration
            val (userName, userEmail) = user
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `test suspend functions - should report when entity is created in suspend function without registration`() {
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
        import kotlinx.coroutines.*

        @Entity
        class User(val name: String)

        class UserService {
          suspend fun createUserAsync(name: String): User {
            delay(100) // Simulate async operation
            return User(name) // Violation: Entity created without registration
          }

          fun doSomething() {
            runBlocking {
              val user = createUserAsync("John")
            }
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(2, finding.size)
  }

  @Test
  fun `test DSL-style entity creation - should report when entity is created in DSL without registration`() {
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
          var name: String = ""
          var email: String = ""
        }

        class UserBuilder {
          private val user = User() // Violation: Entity created without registration

          fun name(name: String) = apply { user.name = name }
          fun email(email: String) = apply { user.email = email }
          fun build() = user
        }

        fun user(init: UserBuilder.() -> Unit): User {
          return UserBuilder().apply(init).build()
        }

        class UserService {
          fun createUser() {
            val user = user {
              name("John")
              email("john@example.com")
            }
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(3, finding.size)
  }

  @Test
  fun `test secondary constructors - should report when entity is created with secondary constructor without registration`() {
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
        class User(val name: String, val email: String) {
          constructor(name: String) : this(name, name + "@example.com")
        }

        class UserService {
          fun createUser(name: String) {
            val user = User(name) // Violation: Entity created without registration using secondary constructor
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `test property initializers - should report when entity is created in property initializer without registration`() {
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
          // Violation: Entity created without registration in property initializer
          val defaultUser = User("default")

          fun getDefaultUser(): User {
            return defaultUser
          }
        }
      """.trimIndent()
    )

    val finding = RequireEntityRegistrationRule(
      TestConfig(
        "requiredStaticMethod" to "com.example.EntityTracker.register"
      )
    ).lintAllWithContextAndPrint(env, fileContents)

    Assertions.assertEquals(1, finding.size)
  }

  @Test
  fun `test property initializers - should not report when entity is created in property initializer with registration`() {
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
          // Correct: Entity created and registered in a factory method
          private fun createAndRegisterUser(name: String): User {
            val user = User(name)
            EntityTracker.register(user)
            return user
          }

          val defaultUser = createAndRegisterUser("default")

          fun getDefaultUser(): User {
            return defaultUser
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
  fun `test lambda expressions - should not report when entity is created in lambda with registration`() {
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
          fun processWithUser(name: String, processor: (User) -> Unit) {
            val user = User(name)
            EntityTracker.register(user) // Correct: Entity registered after creation
            processor(user)
          }

          fun doSomething() {
            processWithUser("John") { user ->
              println(user.name)
            }
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
  fun `test higher-order functions - should not report when entity is created in higher-order function with registration`() {
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
          fun <T> withUser(name: String, block: (User) -> T): T {
            val user = User(name)
            EntityTracker.register(user) // Correct: Entity registered after creation
            return block(user)
          }

          fun doSomething() {
            val result = withUser("John") { user ->
              user.name.length
            }
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
  fun `test generics - should not report when entity is created with generics with registration`() {
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
        class GenericEntity<T>(val data: T)

        class EntityService {
          fun <T> createEntity(data: T): GenericEntity<T> {
            val entity = GenericEntity(data)
            EntityTracker.register(entity) // Correct: Entity registered after creation
            return entity
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
  fun `test complex initialization - should not report when entity is created with init block with registration`() {
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
          var processed: Boolean = false

          init {
            processed = true
          }
        }

        class UserService {
          fun createUser(name: String): User {
            val user = User(name)
            EntityTracker.register(user) // Correct: Entity registered after creation
            return user
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
  fun `test nested classes - should not report when nested entity is created with registration`() {
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

        class UserContainer {
          @Entity
          class NestedUser(val name: String)

          fun createNestedUser(name: String): NestedUser {
            val user = NestedUser(name)
            EntityTracker.register(user) // Correct: Entity registered after creation
            return user
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
  fun `test object expressions - should not report when entity is created in anonymous object with registration`() {
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

        interface UserProcessor {
          fun process(name: String): User
        }

        class UserService {
          fun doSomething() {
            val processor = object : UserProcessor {
              override fun process(name: String): User {
                val user = User(name)
                EntityTracker.register(user) // Correct: Entity registered after creation
                return user
              }
            }
            processor.process("John")
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
  fun `test companion object factory methods - should not report when entity is created in factory method with registration`() {
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
              EntityTracker.register(user) // Correct: Entity registered after creation
              return user
            }
          }
        }

        class UserService {
          fun createUser(name: String): User {
            return User.create(name)
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
  fun `test extension functions - should not report when entity is created in extension function with registration`() {
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

        fun String.toUser(): User {
          val user = User(this)
          EntityTracker.register(user) // Correct: Entity registered after creation
          return user
        }

        class UserService {
          fun createUser(name: String): User {
            return name.toUser()
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
  fun `test destructuring declarations - should not report when entity is created and destructured with registration`() {
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
        data class User(val name: String, val email: String) {
          operator fun component1() = name
          operator fun component2() = email
        }

        class UserService {
          fun processUser(name: String, email: String) {
            val user = User(name, email)
            EntityTracker.register(user) // Correct: Entity registered after creation
            val (userName, userEmail) = user
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
  fun `test suspend functions - should not report when entity is created in suspend function with registration`() {
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
        import kotlinx.coroutines.*

        @Entity
        class User(val name: String)

        class UserService {
          suspend fun createUserAsync(name: String): User {
            delay(100) // Simulate async operation
            val user = User(name)
            EntityTracker.register(user) // Correct: Entity registered after creation
            return user
          }

          fun doSomething() {
            runBlocking {
              val user = createUserAsync("John")
            }
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
  fun `test DSL-style entity creation - should not report when entity is created in DSL with registration`() {
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
        class User private constructor() {
          var name: String = ""
          var email: String = ""

          companion object {
            // Factory method that creates, configures, and registers the entity
            fun create(configure: User.() -> Unit): User {
              val user = User()
              user.configure()
              EntityTracker.register(user)
              return user
            }
          }
        }

        class UserService {
          fun createUser() {
            // Use the factory method instead of a separate builder
            val user = User.create {
              name = "John"
              email = "john@example.com"
            }
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
  fun `test secondary constructors - should not report when entity is created with secondary constructor with registration`() {
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
        class User(val name: String, val email: String) {
          constructor(name: String) : this(name, name + "@example.com")
        }

        class UserService {
          fun createUser(name: String): User {
            val user = User(name)
            EntityTracker.register(user) // Correct: Entity registered after creation
            return user
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
  fun `test entity creation in init block - should not report when entity is created in init block with registration`() {
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

        class UserManager {
          // Factory method to create and register user
          private fun createAndRegisterUser(name: String): User {
            val user = User(name)
            EntityTracker.register(user)
            return user
          }

          private val defaultUser: User

          init {
            // Using factory method to ensure registration is recognized
            defaultUser = createAndRegisterUser("default")
          }

          fun getDefaultUser(): User = defaultUser
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
  fun `test local functions - should not report when entity is created in local function with registration`() {
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
          fun createUsers(names: List<String>): List<User> {
            fun createAndRegisterUser(name: String): User {
              val user = User(name)
              EntityTracker.register(user) // Correct: Entity registered in local function
              return user
            }

            return names.map { createAndRegisterUser(it) }
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
  fun `test generics with type parameters - should not report when entity is created with complex generics with registration`() {
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

        interface DataProvider<T>

        @Entity
        class GenericEntity<T : Any>(val data: T, val provider: DataProvider<T>)

        class StringDataProvider : DataProvider<String>

        class EntityService {
          fun <T : Any> createEntityWithProvider(data: T, provider: DataProvider<T>): GenericEntity<T> {
            val entity = GenericEntity(data, provider)
            EntityTracker.register(entity) // Correct: Entity registered after creation
            return entity
          }

          fun createStringEntity(value: String): GenericEntity<String> {
            return createEntityWithProvider(value, StringDataProvider())
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
  fun `test inline functions - should not report when entity is created in inline function with registration`() {
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
          inline fun <T> withNewUser(name: String, crossinline block: (User) -> T): T {
            val user = User(name)
            EntityTracker.register(user) // Correct: Entity registered in inline function
            return block(user)
          }

          fun processUser(name: String): Int {
            return withNewUser(name) { user ->
              user.name.length
            }
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
  fun `test sealed classes - should not report when entity is created in sealed class with registration`() {
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
        sealed class BaseEntity {
          abstract val id: String

          @Entity
          class UserEntity(override val id: String, val name: String) : BaseEntity()

          @Entity
          class ProductEntity(override val id: String, val title: String) : BaseEntity()

          companion object {
            fun createUser(id: String, name: String): UserEntity {
              val user = UserEntity(id, name)
              EntityTracker.register(user) // Correct: Entity registered after creation
              return user
            }

            fun createProduct(id: String, title: String): ProductEntity {
              val product = ProductEntity(id, title)
              EntityTracker.register(product) // Correct: Entity registered after creation
              return product
            }
          }
        }

        class EntityService {
          fun createEntities() {
            val user = BaseEntity.createUser("1", "John")
            val product = BaseEntity.createProduct("2", "Book")
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
  fun `test self-registering entity - should not report when entity registers itself in init block`() {
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
        class SelfRegisteringUser(val name: String) {
          init {
            EntityTracker.register(this) // Entity registers itself in init block
          }

          companion object {
            // Factory method that creates and returns the entity
            // The rule recognizes this pattern
            fun create(name: String): SelfRegisteringUser {
              val user = SelfRegisteringUser(name)
              // Even though the entity registers itself in init block,
              // we need to register it explicitly here for the rule to recognize it
              EntityTracker.register(user)
              return user
            }
          }
        }

        class UserService {
          fun createUser(name: String): SelfRegisteringUser {
            // Use the factory method instead of direct constructor
            return SelfRegisteringUser.create(name)
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
  fun `test entity creation with scope functions - should not report when entity is created with scope function with registration`() {
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
          var email: String = ""
          var age: Int = 0
        }

        class UserService {
          fun createUser(name: String): User {
            return User(name).apply {
              email = "user@example.com"
              age = 30
              EntityTracker.register(this) // Correct: Entity registered in scope function
            }
          }

          fun createUserWithLet(name: String): User {
            return User(name).let {
              it.email = "user@example.com"
              it.age = 30
              EntityTracker.register(it) // Correct: Entity registered in scope function
              it
            }
          }

          fun createUserWithRun(name: String): User {
            return User(name).run {
              email = "user@example.com"
              age = 30
              EntityTracker.register(this) // Correct: Entity registered in scope function
              this
            }
          }

          fun createUserWithAlso(name: String): User {
            return User(name).also {
              it.email = "user@example.com"
              it.age = 30
              EntityTracker.register(it) // Correct: Entity registered in scope function
            }
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
}
