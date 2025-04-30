# Hibernate Rules Detekt Plugin

A custom set of [Detekt](https://github.com/detekt/detekt) static analysis rules for enforcing best practices when
working with Hibernate/JPA in Kotlin applications.

## Overview

This plugin provides specialized Detekt rules to enforce proper usage patterns when working with Hibernate/JPA
annotations in Kotlin code. It helps developers avoid common pitfalls and ensures that the Hibernate ORM works correctly
with Kotlin entities.

The plugin includes the following rules:

1. **HibernateAssociationsRule**: Ensures proper handling of JPA/Hibernate associations to prevent detached entity
   issues and lazy loading problems.
2. **HibernateEntityPropertiesRule**: Enforces that all properties in `@Entity` classes are declared as `private var`.
3. **RequireEntityRegistrationRule**: Verifies that entity objects are registered with a tracking system after creation.

## Installation

### Gradle

Add the following to your project's build file:

```kotlin
plugins {
  id("io.gitlab.arturbosch.detekt") version "<detekt-version>"
}

dependencies {
  detektPlugins("ru.code4a:hibernate-rules-detekt-plugin:<version>")
}
```

### Maven

```xml

<dependency>
  <groupId>ru.code4a</groupId>
  <artifactId>hibernate-rules-detekt-plugin</artifactId>
  <version>[version]</version>
</dependency>
```

## Rules

### HibernateAssociationsRule

Detects and prevents direct mutations of Hibernate association fields. This rule helps avoid common issues like lazy
initialization exceptions and unintentional changes to persistent collections.

**What this rule checks:**

1. Direct mutations to `@ManyToOne`, `@OneToOne`, `@ManyToMany`, `@OneToMany` fields
2. Direct assignments to `@ManyToOne`, `@OneToOne` fields
3. Returning, storing in variables, or passing Hibernate collections as parameters without copying

**Configuration:**

```yaml
foura_hibernate_rule_set:
  HibernateAssociationsRule:
    active: true
    additionalSafeOperations:
      - "your.custom.CopyUtility.safeCopy"
      - "your.custom.utilities.EntityUtils.cloneCollection"
```

#### Examples

❌ **Non-compliant code:**

```kotlin
@Entity
class Parent {
  @OneToMany
  private var children: MutableList<Child> = mutableListOf()

  fun addChild(child: Child) {
    children.add(child) // Violation: direct modification
  }

  fun getChildren(): List<Child> {
    return children // Violation: direct return without copying
  }
}
```

✅ **Compliant code:**

```kotlin
@Entity
class Parent {
  @OneToMany
  private var children: MutableList<Child> = mutableListOf()

  fun addChild(child: Child) {
    val copy = ArrayList(children)
    copy.add(child)
  }

  fun getChildren(): List<Child> {
    return ArrayList(children) // Correct: returns a copy
  }
}
```

### HibernateEntityPropertiesRule

Ensures that all properties in classes annotated with `@Entity` follow best practices for Hibernate.

**What this rule checks:**

All properties in `@Entity` classes must be:

1. Declared as `private`
2. Declared as `var` (mutable)

**Configuration:**

```yaml
foura_hibernate_rule_set:
  HibernateEntityPropertiesRule:
    active: true
```

#### Examples

❌ **Non-compliant code:**

```kotlin
@Entity
class User {
  val id: Long = 0 // Violation: should be var
  public var name: String = "" // Violation: should be private
}
```

✅ **Compliant code:**

```kotlin
@Entity
class User {
  private var id: Long = 0
  private var name: String = ""
}
```

### RequireEntityRegistrationRule

Ensures that after creating an entity, a specific registration method is called. This is useful for tracking entities or
enforcing a particular lifecycle for entity objects.

**What this rule checks:**

After each object creation of a class marked with `@Entity`, a specified static method must be called with this entity
passed as a parameter.

**Configuration:**

```yaml
foura_hibernate_rule_set:
  RequireEntityRegistrationRule:
    active: true
    requiredStaticMethod: "com.example.EntityTracker.register" # The method that must be called
```

#### Examples

❌ **Non-compliant code:**

```kotlin
@Entity
class User(val name: String)

class UserService {
  fun createUser(name: String) {
    val user = User(name) // Violation: Entity created without registration
  }
}
```

✅ **Compliant code:**

```kotlin
@Entity
class User(val name: String)

class UserService {
  fun createUser(name: String) {
    val user = User(name)
    EntityTracker.register(user) // Entity properly registered
  }
}
```

## Full Configuration Example

```yaml
foura_hibernate_rule_set:
  active: true

  HibernateAssociationsRule:
    active: true
    additionalSafeOperations: [ ]

  HibernateEntityPropertiesRule:
    active: true

  RequireEntityRegistrationRule:
    active: true
    requiredStaticMethod: "com.example.EntityTracker.register"
```

## Kotlin & Detekt Compatibility

This plugin is compatible with:

- Kotlin 1.9.x
- Detekt version specified in your project configuration

# Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

# License

Apache 2.0
