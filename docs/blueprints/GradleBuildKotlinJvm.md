# Gradle build script for kotlin projects

## Basic structure of a build script

A gradle script has a few basic sections.

To make the script easier to understand reading top-down, the order in this list reflects
higher-level configuration above sections that "depend" on them (logically or actually). Thererofe,
also follow this ordering in the build script.

- `plugins {}`: what gradle plugins to apply to the project
- project properties (e.g. `group`, `version`, `sourceCompatibility`, etc)
- `repositories {}`: where to find dependencies
- `dependencyManagement {}`: spring boot extension to manage maven BOMs, etc
- `dependencies {}`: what dependencies to use
- task configurations (definition and customization)

Note: if you CMD+click a top-level section in the IDE it will jump to the corresponding definition
in `package org.gradle.kotlin.dsl` -- which contains extension functions for configuring
an `org.gradle.api.Project`. Some of these are pre-defined (e.g. `repositories`), some are generated
after applying plugins (e.g `java`). So feel free to explore the source code in your IDE in addition
to the documentation. Also, if some sections show compile errors it usually means something went
wrong while applying plugins. You might need to refresh the gradle project or in some cases restart
the IDE (and maybe clearing IDE caches).

## Common plugins for a Kotlin project

Minimal setup:

```kotlin
plugins {
  kotlin("jvm") version "1.9.10" // kotlin plugin for JVM, also pulls in java and base plugins
  id("org.jlleitschuh.gradle.ktlint") version "11.6.1" // ensure consisten code formatting
  id("com.github.ben-manes.versions") version "0.50.0" // optional, check for newer versions of dependencies
}
```

Spring boot setup:

```kotlin
plugins {
  kotlin("jvm") version "1.9.10"
  kotlin("plugin.spring") version "1.9.10" // helps with kotlin classes with spring annotations, which need to be `open`
  kotlin("plugin.jpa") version "1.9.10" // helps with kotlin classes with JPA annotations
  id("org.springframework.boot") version "3.2.1" // spring boot tasks
  id("io.spring.dependency-management") version "1.1.4" // spring dependency management to ensure consistent versions
  id("org.jlleitschuh.gradle.ktlint") version "11.6.1"
  id("com.github.ben-manes.versions") version "0.50.0"
}
```

See also:

- https://kotlinlang.org/docs/gradle-configure-project.html
- https://docs.gradle.org/current/userguide/plugin_reference.html
- https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/htmlsingle/
- https://kotlinlang.org/docs/all-open-plugin.html#spring-support
- https://spring.io/guides/tutorials/spring-boot-kotlin#gradle-build
- https://github.com/JLLeitschuh/ktlint-gradle
- https://github.com/ben-manes/gradle-versions-plugin

Note: make sure to set the same version for plugins that belong together, in this case `1.9.10` for
kotlin plugins. Variables specified in the `build.gradle.kts` cannot be used in
the `plugins {}` section, because the `plugins {}` block is evaluated before the rest of the script.

However, you can set variables in gradle.properties (this might be hard to discover if someone
doesn't know where to look) or version catalogs (especially if also used for project dependencies).
Sometimes it easier to duplicate the version strings as there are rarely many plugins to warrant a
more complicated solution.

See also:

- https://docs.gradle.org/current/userguide/plugins.html
- https://docs.gradle.org/current/userguide/plugins.html#sec:plugin_version_management

## Project properties

```kotlin
group = "com.vacuumlabs.project"
version = "1.0.0"
```

Note: you don't set project name here. The root project is named in `settings.gradle.kts` and
subprojects
are named based on subproject directory names. Artifact naming can be configured in e.g. java plugin
configuration.

## Dependencies

Basic dependency example

```kotlin
repositories {
  mavenCentral() // use the maven central repository to resolve dependencies
}

dependencies {
  api("com.fasterxml.jackson.core:jackson-core:2.13.0") // also expose as transitive dependencies downstream
  implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.13.0") // "regular" dependency

  testImplementation(kotlin("test")) // test dependency using a DLS helper
  testImplementation("io.strikt:strikt-core:0.34.1") // test dependency using string notation
}
```

See also:

- https://docs.gradle.org/current/userguide/java_platform_plugin.html#sec:java_platform_separation

### Using native gradle support form Maven BOMs

Using a BOM (Bill of Materials) to manage versions. These are based on special artifacts that only
define transitive dependencies without any code.

```kotlin
dependencies {
  implementation(platform("org.jetbrains.kotlin:kotlin-bom"))
}
```

See also:

- https://docs.gradle.org/current/userguide/dependency_management_terminology.html#sub::terminology_platform

### Using spring dependency management plugin

Spring boot projects can use the `io.spring.dependency-management` plugin to manage pinned versions
of spring dependencies that have been tested together before a spring release.

```kotlin
plugins {
  id("io.spring.dependency-management") version "1.1.4" // what makes it work
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter") // no version needed, but can be overridden e.g. to use newer version
}
```

Import more BOMs using spring dependency management plugin:

```kotlin
dependencyManagement {
  imports {
    mavenBom("org.springframework.cloud:spring-cloud-dependencies:2022.0.1")
  }
}

dependencies {
  implementation("org.springframework.cloud:spring-cloud-starter-stream-kafka")
}
```

Note: `dependencyManagement {}` is optional, you can just use `platform()` dependencies or combine
the
two. However, by applying the `io.spring.dependency-management` -- those versions will be pinned.

See also:

- https://docs.spring.io/dependency-management-plugin/docs/current/reference/html/
- https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/htmlsingle/#managing-dependencies.gradle-bom-support
- https://docs.spring.io/spring-boot/docs/current/reference/html/dependency-versions.html
  - make sure you refer to the correct version of the docs in the URL, that matches your project!

### Version properties

You can define variables on a project as `extra` properties extension on projects.

```kotlin

extra["temporalVersion"] = "1.20.1" // override our own dependency
extra["slf4j.version"] = "1.7.20" // override spring dependency version

dependencies {
  implementation("io.temporal:temporal-sdk:${property("temporalVersion")}")
  implementation("org.springframework.boot:spring-boot-starter") // slf4j is a transitive dependency
}
```

See also:

- https://docs.gradle.org/current/userguide/writing_build_scripts.html#sec:extra_properties
- https://docs.spring.io/spring-boot/docs/current/gradle-plugin/reference/htmlsingle/#managing-dependencies.dependency-management-plugin.customizing

### Version catalogs

You can declare dependency versions in a catalog file. This is especially useful if you want to
ensure the same versions in multiple (sub)projects in a monorepo.

A file in the root project: `gradle/libs.versions.toml`
```toml
[versions]
kotlin = "1.9.22"
spring-boot = "3.2.4"
spring-dependency-management = "1.1.4"
ktlint-plugin = "12.1.0"
benmanes-versions = "0.50.0"
jackson = "2.17.0"

[plugins]
kotlin-jvm = { id = "org.jetbrains.kotlin.jvm", version.ref = "kotlin" }
kotlin-spring = { id = "org.jetbrains.kotlin.plugin.spring", version.ref = "kotlin" }
kotlin-jpa = { id = "org.jetbrains.kotlin.plugin.jpa", version.ref = "kotlin" }
spring-boot = { id = "org.springframework.boot", version.ref = "spring-boot" }
spring-dependency-management = { id = "io.spring.dependency-management", version.ref = "spring-dependency-management" }
ktlint = { id = "org.jlleitschuh.gradle.ktlint", version.ref = "ktlint-plugin" }
benmanes-versions = { id = "com.github.ben-manes.versions", version.ref = "benmanes-versions" }

[libraries]
jackson-module-kotlin = { module = "com.fasterxml.jackson.module:jackson-module-kotlin", version.ref = "jackson" }
jackson-datatype-jsr310 = { module = "com.fasterxml.jackson.datatype:jackson-datatype-jsr310", version.ref = "jackson" }
```

`build.gradle.kts` in the (sub)module:

```kotlin
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.jpa)
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.ktlint)
    alias(libs.plugins.benmanes.versions)
}

dependencies {
    api(libs.jackson.core.annotations)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.jackson.datatype.jsr310)
}
```
Note: the kebab-case property names in the `libs.versions.toml` file are converted to dot notation in `build.gradle.kts` and prefixed with `libs`.

You could even specify versions externally from project and reference it in `settings.gradle.kts`

```kotlin
dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}
```

See also:
  - https://docs.gradle.org/current/userguide/platforms.html#sub:central-declaration-of-dependencies

## Commonly applied Java / kotlin project task configurations

E.g. these are added by spring initializr when you create a new project.

```kotlin
tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict" // better interop with Java @Nonnull annotation (from JSR-305)
        jvmTarget = "17" // set the target JVM version
    }
}

tasks.withType<Test> {
    useJUnitPlatform() // discover and execute JUnit5 Platform-based tests
}
```

See also:
- https://spring.io/guides/tutorials/spring-boot-kotlin#_compiler_options
- https://kotlinlang.org/docs/java-interop.html#jsr-305-support
- https://docs.gradle.org/current/userguide/java_testing.html#using_junit5
