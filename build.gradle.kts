import org.jreleaser.model.Active

plugins {
  kotlin("jvm") version "1.9.23"
  kotlin("plugin.serialization") version "1.9.23"

  `java-library`
  `maven-publish`
  id("org.jreleaser") version "1.12.0"
}

val detektVersion: String by project

group = "ru.code4a"
version = file("version").readText().trim()

repositories {
  mavenCentral()
  mavenLocal()
}

java {
  withJavadocJar()
  withSourcesJar()
}

dependencies {
  implementation("net.mamoe.yamlkt:yamlkt:0.13.0")

  compileOnly("io.gitlab.arturbosch.detekt:detekt-api:$detektVersion")
  testImplementation("io.gitlab.arturbosch.detekt:detekt-test:$detektVersion")

  testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
}

publishing {
  publications {
    create<MavenPublication>("mavenJava") {
      artifactId = "hibernate-rules-detekt-plugin"

      from(components["java"])

      pom {
        name = "Hibernate Rules Detekt Plugin Extension"
        description =
          "This plugin provides specialized Detekt rules to enforce proper usage patterns when working with Hibernate/JPA " +
            "annotations in Kotlin code. It helps developers avoid common pitfalls and ensures that the Hibernate ORM works correctly " +
            "with Kotlin entities."
        url = "https://github.com/4ait/hibernate-rules-detekt-plugin"
        inceptionYear = "2024"
        licenses {
          license {
            name = "The Apache License, Version 2.0"
            url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
          }
        }
        developers {
          developer {
            id = "tikara"
            name = "Evgeniy Simonenko"
            email = "tiikara93@gmail.com"
            organization.set("4A LLC")
            roles.set(
              listOf(
                "Software Developer",
                "Head of Development"
              )
            )
          }
        }
        organization {
          name = "4A LLC"
          url = "https://4ait.ru"
        }
        scm {
          connection = "scm:git:git://github.com:4ait/hibernate-rules-detekt-plugin.git"
          developerConnection = "scm:git:ssh://github.com:4ait/hibernate-rules-detekt-plugin.git"
          url = "https://github.com/4ait/hibernate-rules-detekt-plugin"
        }
      }
    }
  }
  repositories {
    maven {
      url =
        layout.buildDirectory
          .dir("staging-deploy")
          .get()
          .asFile
          .toURI()
    }
  }
}

jreleaser {
  project {
    copyright.set("4A LLC")
  }
  gitRootSearch.set(true)
  signing {
    active.set(Active.ALWAYS)
    armored.set(true)
  }
  release {
    github {
      overwrite.set(true)
      branch.set("master")
    }
  }
  deploy {
    maven {
      mavenCentral {
        create("maven-central") {
          active.set(Active.ALWAYS)
          url.set("https://central.sonatype.com/api/v1/publisher")
          stagingRepositories.add("build/staging-deploy")
          retryDelay.set(30)
        }
      }
    }
  }
}

tasks.test {
  useJUnitPlatform()
}
