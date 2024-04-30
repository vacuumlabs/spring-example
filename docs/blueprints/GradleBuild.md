# Gradle Build Blueprint

This is a simple gradle project without submodules.

## Directory layout

- `gradle` -- gradle files that are commited to git
  - `wrapper` -- [gradle wrapper](https://docs.gradle.org/current/userguide/gradle_wrapper.html)
    files, config and mini jar for running a gradle version pinned to the project
    using `./gradlew`
  - `libs.versions.toml` -- (optional) Centralized version management for dependencies
    using [Gradle Version Catalogs](https://docs.gradle.org/current/userguide/platforms.html#sub:central-declaration-of-dependencies)
- `.gradle` -- local cache directory for gradle files, can be safely deleted if needed
- `build` -- output directory for files generated during the build process, `.gitignored`
  - `gradle clean` task will remove it
- `src` -- directory containing source files
  - the actual structure depends on the applied gradle plugins, e.g. java / kotlin JVM plugin

See also:
- https://docs.gradle.org/current/userguide/organizing_gradle_projects.html
- https://docs.gradle.org/current/userguide/gradle_wrapper.html
- https://docs.gradle.org/current/userguide/platforms.html#sub:central-declaration-of-dependencies
- https://docs.gradle.org/current/userguide/java_plugin.html#source_sets

## Build files

- `settings.gradle.kts` -- this is what turns a directory into a standalone gradle project (vs
  submodule)
- `build.gradle.kts` -- build script, for each (sub)project
- `gradle.properties` -- (optional) -- for setting gradle properties (e.g. max memory) and possibly
  project properties
- `gradlew` -- gradle wrapper script for UNIX systems
- `gradlew.bat` -- gradle wrapper script for Windows systems

Note: settings and build files can also written in Groovy (without `.kts` extension), but Kotlin DSL
is recommended. Kotlin is more strongly typed so you get better IDE support,
and is expressive with DSLs, etc.

See also:

- https://docs.gradle.org/current/userguide/kotlin_dsl.html

## Configuring `settings.gradle.kts`

### Root project

At a minimum set the root project name. Gradle uses the directory name by default, but it's better
to be explicit, in case someone checks out the git repo under a different name.
This name will be used for naming produced artifacts, etc.

```kotlin
rootProject.name = "test"
```

### Subprojects

If you have subprojects, you can include them like this. This is useful for monorepos, where you
have multiple projects in the same repo.

Note: subprojects don't need a `settings.gradle.kts` file.

```kotlin
include("sub-project")
```

See also:

- https://docs.gradle.org/current/userguide/multi_project_builds.html#sub:building_the_tree

### Composite builds

If you'd like to develop multiple standalone projects at the same time you can use composite builds.
This way you can:
-
- add dependency without needing to publish other project to a local or remote repository
- open the other projects automatically in the IDE
- do refactoring across multiple projects
- you can also put independent projects in a monorepo and develop them using a composite build, but
  build and release them independently in a CICD pipeline

Note:  Subprojects DO need a `settings.gradle.kts` file -- because they can also exist
independently.

```kotlin
includeBuild("../another-project")
```

See also:
- https://docs.gradle.org/current/userguide/composite_builds.html

### Toolchain management (optional)

This section is just to document something you might come across in generated projects, but is not
yet tested .

We generally install JDKs separately:
- on dev machines: using[SDKMAN](https://sdkman.io/) or similar
- on CI : using a base image with the desired JDK version

    If you generate a new project with `gradle init`, it will configure this plugin in `settings.gradle.kts` :

```kotlin
plugins {
  // Apply the foojay-resolver plugin to allow automatic download of JDKs
  id("org.gradle.toolchains.foojay-resolver-convention") version "0.8.0"
}
```

See also:
- https://docs.gradle.org/current/userguide/toolchains.html#sub:download_repositories
- https://github.com/gradle/foojay-toolchains
- https://github.com/foojayio/discoapi
