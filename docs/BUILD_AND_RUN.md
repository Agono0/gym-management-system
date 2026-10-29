# BUILD_AND_RUN.md — build, run and verify the Gym Management System (JavaFX branch)

> **What this file owns:** the Gradle build, the exact run commands, the local SQLite file, the IntelliJ setup, and the checks to run before you say a task is done.
> **What it does not own:** class names, fields and rules (`docs/UML.md`), the *why* behind a choice (`docs/DECISIONS.md`), or layer teaching (the folder `README.md` files under `src/main/java/com/gym/`). `docs/DOCS_STANDARD.md` sets the voice; this is a build guide, so it uses the ten sections below instead of the layer-README skeleton.
> **Verified on:** Windows 11, `.\gradlew.bat --version` → **Gradle 9.8.0**, launcher JVM **27** (Oracle 27+35-2325), `JAVA_HOME=C:\Users\pc\.jdks\openjdk-27`, `.\gradlew.bat build` → `BUILD SUCCESSFUL`. Anything not verified that way is marked ⚠️.

---

## 1. Purpose

This guide takes a fresh clone to a running window, and tells you how to check that the run was real. The app is built by **Gradle** — a multi-project build with the app at the repo root plus the two `m3fx` library modules. Whoever grades or demos the project builds nothing by hand; they run one of two things:

| Path | Command | What it uses |
|---|---|---|
| Terminal (recommended) | `.\gradlew.bat run` on Windows, `./gradlew run` on macOS/Linux | the committed Gradle wrapper, which pins Gradle 9.8.0 |
| IntelliJ IDEA | Gradle tool window → Tasks → application → `run` | the same wrapper IntelliJ imported |

Both end in the same place: the `run` task compiles the app and starts `com.gym.MainApp` (`build.gradle` line 38), which loads `/fxml/main.fxml` (`MainApp.java` line 13).

**Quick start:**

```text
git clone https://github.com/Agono0/gym-management-system.git
cd gym-management-system
.\gradlew.bat build        # POSIX: ./gradlew build
.\gradlew.bat run          # POSIX: ./gradlew run
```

Out of scope here: the app's layers (each folder has its own `README.md`) and `m3fx` internals — `m3fx-core` and `m3fx-controls` are dependencies of the app, and that is all this guide needs to say.

---

## 2. Prerequisites

| Requirement | Exact value | Where it comes from | How to check |
|---|---|---|---|
| JDK | **27** | `build.gradle` lines 10–14 (`JavaLanguageVersion.of(27)`) | `java -version`; `.\gradlew.bat -q javaToolchains` |
| `JAVA_HOME` | your JDK 27 install | the Gradle daemon starts on the current JVM | `$env:JAVA_HOME` (PowerShell), `echo $JAVA_HOME` (POSIX) |
| IntelliJ project SDK | `openjdk-27` | `.idea/misc.xml` line 7 — the only `.idea` file we commit | File → Project Structure → Project → SDK |
| Gradle | **9.8.0** via the wrapper — never installed by hand | `gradle/wrapper/gradle-wrapper.properties` line 3 | `.\gradlew.bat --version` |
| Network (first build only) | mavenCentral | `build.gradle` line 17 | the first `build` downloads SQLite JDBC, JavaFX, and the Gradle distribution |

### 2.1 Why the `java { toolchain { … } }` block matters

A **toolchain** is Gradle's way of saying "compile this project with JDK 27, whatever `java` is on my `PATH`". Three consequences you can rely on:

1. **A wrong JDK fails loudly and early.** With no JDK 27 available, Gradle stops with its *no matching toolchains found* error naming `languageVersion=27` — instead of compiling happily and throwing `UnsupportedClassVersionError` at runtime, which is the confusing failure this block prevents.
2. **Gradle 9.8 can provision the JDK itself.** Verified: `.\gradlew.bat -q javaToolchains` prints `Auto-detection: Enabled`, `Auto-download: Enabled`, our Oracle JDK 27 as *Current JVM*, **and** an Eclipse Temurin JDK 17 that Gradle downloaded on its own into `%USERPROFILE%\.gradle\jdks`. A missing JDK 27 is therefore either downloaded (needs network) or reported clearly. ⚠️ That auto-provisioning is built into Gradle 9; a Gradle 8 setup needed an extra resolver plugin in `settings.gradle`, which this repo does not have.
3. **The IDE must agree.** `.idea/misc.xml` pins `openjdk-27`, the JDK the toolchain asks for. If IntelliJ sits on 21 while Gradle compiles with 27, the editor marks red on code that builds.

### 2.2 The Gradle wrapper — never install Gradle by hand

`gradlew.bat` (Windows) and `gradlew` (POSIX) read `gradle/wrapper/gradle-wrapper.properties`, download that exact distribution once into `%USERPROFILE%\.gradle\wrapper\dists` (POSIX `~/.gradle/wrapper/dists`), and run it. Verified: this machine already caches `gradle-9.8.0-bin`, so `gradlew` starts without network.

- **Always use `gradlew`/`gradlew.bat`, never a globally installed `gradle`.** A system Gradle can resolve plugins differently, and the grader's machine has only what the wrapper brings.
- **The wrapper JAR is present and tracked.** Verified: `gradle/wrapper/gradle-wrapper.jar` exists (48,462 bytes) and `git ls-files gradle/wrapper` lists it. `.gitignore` line 3 ignores `*.jar`, but lines 29–32 re-include the wrapper JAR, so a fresh clone can run `gradlew` immediately. Do not delete it, and do not re-generate the wrapper casually — that changes the pinned version for everyone.
- ⚠️ `README.md` §5 says "Gradle 8+"; the wrapper argues for **9.8.0 exactly**. Use the wrapper and the looser wording never matters.

### 2.3 IntelliJ IDEA setup

1. **File → Open** the repository root (the folder holding `build.gradle` and `settings.gradle`). IntelliJ imports it as a Gradle project; `settings.gradle` (§3) creates the three projects.
2. **Confirm the SDK.** `.idea/misc.xml` already names `openjdk-27`; if your JDK 27 lives elsewhere, point the project SDK at your own JDK 27 — do not downgrade to 21.
3. **Annotation processing is not needed.** No annotation processor is configured anywhere: no Lombok, no MapStruct, no `annotationProcessor` entry in `build.gradle`, `m3fx-core/build.gradle` or `m3fx-controls/build.gradle`. Leave *Settings → Build → Compiler → Annotation Processors* alone.
4. `.idea/*` is git-ignored except `misc.xml` (`.gitignore` lines 34–39), so window layouts and run configurations stay local — that is intended, not a mistake to "fix".

---

## 3. `settings.gradle` line by line

The whole file (4 lines):

```groovy
rootProject.name = 'gym-management-system-javafx'

include('m3fx-core')
include('m3fx-controls')
```

| Line | Code | What it does |
|---|---|---|
| 1 | `rootProject.name = 'gym-management-system-javafx'` | names the **root project**: the build's name in the IDE, the prefix of the artifacts (`build/libs/gym-management-system-javafx-1.0.0.jar`), and the answer to `gradlew projects` |
| 3 | `include('m3fx-core')` | adds the folder `m3fx-core/` as subproject `:m3fx-core`; the argument is a path **relative to this file** |
| 4 | `include('m3fx-controls')` | the same for the second library module, `:m3fx-controls` |

**What a multi-project build is.** One Gradle invocation drives three projects that share a version, a wrapper and a task graph. `settings.gradle` decides **membership**; each `build.gradle` decides what a member *does*. Two practical effects:

| Effect | Example |
|---|---|
| a task name run from the root applies to every project that has it | `gradlew build` builds all three (verified: `:build`, `:m3fx-controls:build`, `:m3fx-core:build`) |
| a task with a project path applies to one project | `gradlew :m3fx-core:build`; adding a module later is one `include(...)` line |

**Why the two library modules are separate.** `m3fx-core` (colour engine, tokens, theme) and `m3fx-controls` (the `M3*` controls plus CSS) are a reusable Material Design 3 library — group `io.m3fx`, version `0.1.0` — not app code (`README.md` §2b, `DECISIONS.md` 1).

| Reason to split | Consequence |
|---|---|
| core has no UI; controls hold no colour maths | the arrow is one-way (`m3fx-controls` → `m3fx-core`), so the two cannot drift into a cycle |
| a library follows its own rules | the app depends on the modules; nothing in them depends on `com.gym` |
| reuse outside this repo | copying the two folders (or their jars) into another Gradle project is enough (`README.md` §2b) |

That is all this guide says about them: the modules are dependencies of the app; their internals belong to `docs/SPEC_NOTES/` and `docs/COVERAGE.md`.

**Where the app lives — the root project.** ⚠️ There is no `:app` subproject. The gym app *is* the root project:

| You want | Write | Not |
|---|---|---|
| the app's `run` task | `gradlew run` (or `gradlew :run`) | `gradlew :app:run` |
| the app's jar | `build/libs/gym-management-system-javafx-1.0.0.jar` | `app/build/libs/…` |
| a module's task | `gradlew :m3fx-core:build` | `gradlew m3fx-core:build` |

Verified: `gradlew projects` prints root project `gym-management-system-javafx` with children `:m3fx-controls` and `:m3fx-core`. The root project's path is `:` — that is why `run` is written bare.

---

## 4. `build.gradle` line by line

The root build file is 47 lines. Read it in blocks:

| Lines | Block | What it gives you |
|---|---|---|
| 1–5 | `plugins { … }` | the three plugins (§4.1) |
| 7–8 | `group` / `version` | `com.gym` and `1.0.0` — the coordinates written into the jar |
| 10–14 | `java { toolchain { … } }` | JDK **27** for compile and run (§2.1) |
| 16–18 | `repositories { mavenCentral() }` | where `sqlite-jdbc` and JavaFX come from |
| 20–23 | `javafx { version; modules }` | which JavaFX version and modules the app uses (§4.4) |
| 25–35 | `dependencies { … }` | the SQLite JDBC driver plus the two local `m3fx` projects (§4.3) |
| 37–39 | `application { mainClass = … }` | the class the `run` task starts (§4.5) |
| 41–47 | `tasks.register('runSamples', JavaExec)` | an extra, local-only window for eyeballing components (§4.6) |

### 4.1 The three plugins

| Plugin | What it adds | Why this project needs it |
|---|---|---|
| `java` | `compileJava`, `classes`, `jar`, `test`, `build`, `clean`, source sets, the `build/` layout | the baseline: the app is plain Java compiled to `build/classes/java/main` |
| `application` | the `run` task, `installDist`, `distZip`, start scripts, and it reads the `application { … }` block | gives the grader a one-word start command instead of a hand-typed classpath |
| `org.openjfx.javafxplugin` (`0.1.0`) | platform-correct JavaFX artifacts and the module-path arguments for JavaExec/Test tasks | without it a JavaFX app started from a plain classpath dies with *"JavaFX runtime components are missing"* (§4.4) |

The version `0.1.0` is declared **once**, here; the two module builds apply the same plugin id with no version and resolve it from the root build — proven by the green `gradlew build`, which configures all three projects.

### 4.2 `group`, `version` and the toolchain block

- `group = 'com.gym'`, `version = '1.0.0'` → the jar is `gym-management-system-javafx-1.0.0.jar` (file name from `rootProject.name`, version from here).
- `java { toolchain { languageVersion = JavaLanguageVersion.of(27) } }` → Gradle compiles with JDK 27.
- **The Java 21 source-level note (`DECISIONS.md` 2).** Only JDK 27 is installed, and the library modules compile with `--release 21` semantics so their jars run on Java 21+. That flag lives in `m3fx-core/build.gradle` and `m3fx-controls/build.gradle` (`options.release = 21`), **not** in this file. Verified by reading the bytecode with `javap -v`:

| Class | Compiled by | Class-file major | Means |
|---|---|---|---|
| `com/gym/MainApp.class` | the root app build | **71** | Java 27 bytecode |
| `io/m3fx/core/theme/M3Theme.class` | `:m3fx-core` | **65** | Java 21 bytecode |

So the app may use Java 22–27 APIs that the library deliberately cannot. **Open detail (decide + log in `DECISIONS.md`)** — either add `options.release = 21` to the root build for consistency with `DECISIONS.md` 2, or record that the app is allowed to compile at 27.

### 4.3 `repositories` and `dependencies`

`mavenCentral()` is the only repository: everything external (SQLite JDBC, JavaFX) comes from it, while the `m3fx` modules come from this build.

| Dependency | Line | Kind | Why it is here |
|---|---|---|---|
| `implementation 'org.xerial:sqlite-jdbc:3.45.3.0'` | 30 | runtime driver | §4.3.1 |
| `implementation(project(':m3fx-core'))` | 33 | project dependency | the theme/token engine the app calls |
| `implementation(project(':m3fx-controls'))` | 34 | project dependency | the `M3*` controls the screens use |
| `// implementation 'io.github.mkpaz:atlantafx:2.0.1'` | 27 | commented out | historical — §4.3.3 |
| `// implementation 'io.github.palexdev:materialfx:11.17.0'` | 28 | commented out | historical — §4.3.3 |

**4.3.1 Why a JDBC driver is a runtime dependency.** **JDBC** is the JDK's standard database API: `java.sql.Connection`, `Statement`, `ResultSet`. Those interfaces ship inside the JDK but contain no database code. The `sqlite-jdbc` jar is the **driver** that implements them for SQLite and registers itself under the `jdbc:sqlite:` URL scheme. Without it the first connection fails with `java.sql.SQLException: No suitable driver found for jdbc:sqlite:gym.db`, so a data-driven app needs the driver at runtime, not as an optional extra.

**4.3.2 `implementation` vs `api`.**

| Keyword | On *your* compile classpath? | On the compile classpath of whoever depends on *you*? | Use it when |
|---|---|---|---|
| `implementation` | yes | **no** | the dependency is an internal detail — the default |
| `api` | yes | **yes** | a type from it appears in your public API (parameters, return types, fields) |

- `m3fx-controls/build.gradle` line 25 uses `implementation(project(':m3fx-core'))`: core is an internal detail of controls. If a public `M3*` constructor ever exposes an `io.m3fx.core` type, that one line must become `api` — otherwise consumers get "cannot access class …" errors.
- The **app** uses `implementation` for both modules (lines 33–34). An application is not consumed as a library, so nothing is hidden, and both project jars sit on the app's own compile and runtime classpath — which `gradlew run` demonstrates. Check it with `gradlew dependencies` instead of guessing.

**4.3.3 The two commented-out theme libraries — do not enable them.** Lines 27–28 are leftovers from the Week-1 plan to pick an external Material-ish theme library (MaterialFX or AtlantaFX). The in-repo `m3fx` library replaced that plan (`README.md` §2b, `DECISIONS.md` 1), and the app styles itself with `-md-sys-color-*` lookups produced by `M3Theme` (`README.md` §8.6). Enabling either line would add a second theme system that fights those lookups plus an unapproved runtime dependency (`AGENTS.md` §9). Leave them commented; delete them only as an intentional cleanup.

### 4.4 The `javafx { … }` block

```groovy
javafx {
    version = '21.0.4'
    modules = ['javafx.controls', 'javafx.fxml']
}
```

**JavaFX is not part of the JDK any more.** Since JDK 11 it is a separate set of modules downloaded like any other library, which is why a JavaFX project always pins a version and a module list.

| Module | Brings | The app uses it for |
|---|---|---|
| `javafx.controls` | `Button`, `Label`, `TableView`, `Control`/`Skin` bases (and transitively `javafx.base` + `javafx.graphics`) | every screen and every `M3*` control |
| `javafx.fxml` | `FXMLLoader`, `javafx.fxml.FXML`, controller injection | loading `/fxml/main.fxml` (`MainApp.java` line 13) |

**What the plugin fixes.** Start a JavaFX `Application` subclass from a plain classpath and the JVM refuses:

```text
Error: JavaFX runtime components are missing, and the JavaFX modules are required to be on the module path
```

The plugin (a) adds the JavaFX artifacts with the right **classifier** for your OS — no hand-written `javafx-controls-21.0.4-win.jar` — and (b) passes `--module-path` and `--add-modules javafx.controls,javafx.fxml` to the tasks it configures (`run`, `test`). That is the whole reason `.\gradlew.bat run` works with zero VM options. ⚠️ The wiring covers *Gradle* tasks only: a bare `java -cp build/classes/java/main com.gym.MainApp`, or an IntelliJ "Application" configuration without those arguments, still hits the error (§7).

### 4.5 `application { mainClass = 'com.gym.MainApp' }`

`mainClass` is the **fully-qualified class name**, not a module name and not a file path. It is read by `run`, `installDist`, `distZip` and the generated start scripts. If it disagrees with the real class the failure is *"Could not find or load main class com.gym.MainApp"* (§8). The placeholder lives at `src/main/java/com/gym/MainApp.java`, so today's value is correct.

### 4.6 The `runSamples` task

```groovy
// Testing only: standalone window rendering EVERY m3fx component (M3Samples.java is git-ignored).
tasks.register('runSamples', JavaExec) {
    group = 'application'
    description = 'Runs the M3Samples all-components test window.'
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'com.gym.M3Samples'
}
```

- It is a **`JavaExec` task** like `run`, but with its own main class, so it needs no extra plugin.
- `classpath = sourceSets.main.runtimeClasspath` reuses the app's compiled classes **plus** every runtime dependency, including the two `m3fx` modules and JavaFX.
- **Why `M3Samples.java` is git-ignored.** `.gitignore` line 109 excludes `src/main/java/com/gym/M3Samples.java` as a "testing-only sample (never commit)": it is a scratch window that renders every component for eyeballing, and the team does not want it in review diffs or in the graded deliverable.
- ⚠️ Because the task is registered in `build.gradle` but the class is not committed, a fresh clone lists `runSamples` under `gradlew tasks` and **fails** when run, with *"Could not find or load main class com.gym.M3Samples"*. That is expected, not a broken build.

---

## 5. Everyday commands

Run everything from the **repository root** (the folder with `gradlew.bat`). In PowerShell the `.\` is required, because PowerShell does not execute programs from the current directory by name.

| What you want | Windows (PowerShell) | POSIX | What it does |
|---|---|---|---|
| Run the app | `.\gradlew.bat run` | `./gradlew run` | compiles, then starts `com.gym.MainApp` with JavaFX on the module path |
| Build everything | `.\gradlew.bat build` | `./gradlew build` | assemble + check for all three projects |
| Build one library | `.\gradlew.bat :m3fx-core:build` | `./gradlew :m3fx-core:build` | only core and its tests (`:m3fx-controls:build` is the mirror image) |
| Delete build output | `.\gradlew.bat clean` | `./gradlew clean` | removes every `build/` directory; never touches sources |
| Run the samples window | `.\gradlew.bat runSamples` | `./gradlew runSamples` | needs a local `M3Samples.java` (§4.6) |
| Stop the daemon | `.\gradlew.bat --stop` | `./gradlew --stop` | shuts down the background Gradle daemon (§8) |
| See running daemons | `.\gradlew.bat --status` | `./gradlew --status` | daemon PIDs and idle time |
| List tasks | `.\gradlew.bat tasks` | `./gradlew tasks` | add `--all` for every task in every project |
| See the dependency tree | `.\gradlew.bat dependencies` | `./gradlew dependencies` | per project: `.\gradlew.bat :m3fx-core:dependencies` |
| See the projects | `.\gradlew.bat projects` | `./gradlew projects` | confirms the root project and the two modules (§3) |
| See detected JDKs | `.\gradlew.bat -q javaToolchains` | `./gradlew -q javaToolchains` | what the toolchain block will pick (§2.1) |
| Debug the app | `.\gradlew.bat run --debug-jvm` | `./gradlew run --debug-jvm` | starts suspended on port 5005 (option confirmed by `gradlew help --task run`) |
| Re-download dependencies | `.\gradlew.bat build --refresh-dependencies` | `./gradlew build --refresh-dependencies` | when a cached artifact looks corrupt |

Notes that save time:

- The first `build` resolves dependencies from mavenCentral, so it needs internet; later builds are incremental and stay under a second (verified: `15 actionable tasks: 15 up-to-date`).
- `gradlew build` runs `test` too. In the root app that task reports `NO-SOURCE`, because the app has no `src/test` directory (verified). The two `m3fx` modules do have tests, and they run.
- `gradlew run` **blocks** while the window is open; closing the window ends the task. Run it in one terminal and use Git in another.

### 5.1 What `build/` contains, and why Git ignores it

| Path | Contents |
|---|---|
| `build/classes/java/main` | compiled app classes (`com/gym/MainApp.class`) |
| `build/resources/main` | everything copied from `src/main/resources` (`fxml/main.fxml`, …) — this is what `getResource("/fxml/main.fxml")` resolves against |
| `build/libs` | `gym-management-system-javafx-1.0.0.jar` (verified, ~4.8 KB: app classes only) |
| `build/distributions` | `…-1.0.0.zip` and `…-1.0.0.tar` (verified, ~22 MB each: the app plus every runtime jar) |
| `build/reports`, `build/tmp`, `build/generated`, `build/scripts` | test reports, intermediate files, generated start scripts |
| `m3fx-core/build`, `m3fx-controls/build` | the same layout for each library module |

`build/` is **generated**, so `.gitignore` lines 8–13 ignore `build/`, `target/`, `out/`, `bin/`, `dist/`, and lines 111–113 repeat it for the two modules. IntelliJ's own `out/` (`.idea/misc.xml` line 8) is ignored for the same reason. Deleting `build/` is always safe — that is what `gradlew clean` does — and editing anything inside it is wasted work, because the next build overwrites it.

---

## 6. Where the database file goes

The database is one SQLite file, `gym.db`. It is **created at runtime**, never committed, and its location is decided by the **working directory of the running process**:

| How you started the app | Working directory | Where `gym.db` appears |
|---|---|---|
| `.\gradlew.bat run` from the repo root | the repo root (Gradle sets a JavaExec task's working directory to the project directory) | `C:\…\JAVA-Project\gym.db` |
| §7.1 IntelliJ Gradle `run` task | the repo root | the same file |
| §7.2 IntelliJ "Application" configuration | whatever its *Working directory* field says — set it to the repo root | repo root |
| A packaged distribution (`build/install/.../bin/…`) | wherever you launch the script | that folder |

Rules to keep in mind:

- **One file, one machine.** This is a local file database; two teammates never share a `gym.db`. Seed data is recreated from `src/main/resources/db/schema.sql`, not copied from a colleague.
- **Never commit `gym.db`.** `.gitignore` lines 103–106 cover `gym.db`, `gym.db-*` and `*.db`. The file is binary, changes on every run, holds machine-local demo or real member data, and a committed copy conflicts on every pull. The schema is the committed artifact; the database is not.
- **What *is* committed** is the DDL and demo seed data, `src/main/resources/db/schema.sql`. ⚠️ Verified state today: `src/main/resources/db/` contains only `.gitkeep`. `schema.sql` is planned for Week 1 (`src/main/resources/README.md`; `docs/COVERAGE.md` lists it unticked), so this section describes the rule the DAO layer must follow — no code creates `gym.db` yet.

**Getting a clean database.** Stop the app first (a running instance holds the file open), delete the file and its sidecars, then start the app again:

| Windows (PowerShell) | POSIX |
|---|---|
| `Remove-Item -Force .\gym.db, .\gym.db-journal, .\gym.db-wal, .\gym.db-shm` | `rm -f gym.db gym.db-journal gym.db-wal gym.db-shm` |
| `Test-Path .\gym.db` (check) | `ls -l gym.db` (check) |

⚠️ Windows cannot delete a file another process has open. If `Remove-Item` says the file is in use, an earlier run is still alive (§8, *database is locked*).

---

## 7. IntelliJ run configuration

### 7.1 Recommended: run the Gradle task (no VM options)

1. **View → Tool Windows → Gradle** (IntelliJ imports the build when you open the root folder).
2. Expand `gym-management-system-javafx` → **Tasks** → **application**.
3. Double-click **`run`**, or right-click → **Debug 'gym-management-system-javafx [run]'** for breakpoints.
4. Equivalent from the IntelliJ terminal: `.\gradlew.bat run`.

This is the lowest-friction path because the task already carries the JavaFX module-path arguments the plugin adds (§4.4): you type no VM options, and you execute the same code path as `gradlew run` on the grader's machine. For a debugger without the IDE run configuration, `.\gradlew.bat run --debug-jvm` starts the app suspended on port 5005.

### 7.2 A plain "Java Application" configuration (only if you need it)

Use this when you want an IntelliJ-native config with program arguments or a custom working directory.

| Field | Value |
|---|---|
| Main class | `com.gym.MainApp` |
| Use classpath of module | the **root** project (`gym-management-system-javafx.main`) |
| VM options | `--module-path "<folder containing the JavaFX jars>" --add-modules javafx.controls,javafx.fxml` |
| Working directory | the repository root — this decides where `gym.db` lands (§6) |

⚠️ The JavaFX jars are **not** in this repository (no JavaFX SDK folder is committed). After the Gradle import, IntelliJ shows them under *External Libraries*; a plain `--module-path` needs either that cached Gradle copy (under `%USERPROFILE%\.gradle\caches\modules-2\files-2.1\org.openjfx\…`) or a JavaFX 21.0.4 SDK downloaded from openjfx.io. `.\gradlew.bat run --info` prints the exact argument list Gradle generates if you want to copy it, but that is the cost of not using §7.1.

### 7.3 Module path vs classpath — the caveat in one place

| Fact | Consequence |
|---|---|
| The app has **no** `module-info.java` (verified: only `m3fx-core` and `m3fx-controls` have one) | the app itself runs from the **classpath** |
| JavaFX must be on the **module path** | that is why `--module-path`/`--add-modules` are needed at all |
| the two `m3fx` modules arrive as `implementation` project dependencies | the unnamed module reads them from the classpath; you never add them to `--module-path` yourself |
| the JavaFX plugin supplies both arguments for `run` | §7.1 needs no VM options; §7.2 does |

If the plugin were removed, the `--module-path`/`--add-modules` form would become mandatory in *every* launcher. The other classic workaround — a tiny launcher class that does **not** extend `Application` and calls `Application.launch(MainApp.class, args)` — lets JavaFX start from the classpath, but we do not use that trick, and adding it would be a design change worth recording in `DECISIONS.md`.

---

## 8. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `Error: JavaFX runtime components are missing, and the JavaFX modules are required to be on the module path` | JavaFX classes sit on the classpath only: a bare `java -cp …`, or a §7.2 config without VM options | run `.\gradlew.bat run` (§7.1); or add `--module-path … --add-modules javafx.controls,javafx.fxml` |
| `UnsupportedClassVersionError` / "Unsupported class file major version" | an older JDK runs classes compiled by JDK 27 — remember `71` = Java 27, `65` = Java 21 (§4.2) | compare `java -version` with `.\gradlew.bat -q javaToolchains`; point IntelliJ's SDK and `JAVA_HOME` at JDK 27, then `.\gradlew.bat clean build` |
| Gradle reports *no matching toolchains found*, naming `languageVersion=27` | no JDK 27 on the machine, and auto-provisioning could not download one (offline) | install JDK 27 (`C:\Users\pc\.jdks\openjdk-27` here), or add `org.gradle.java.installations.paths=<your jdk 27>` to a `gradle.properties` (the repo has none today) |
| `Error: Could not find or load main class com.gym.MainApp` | `application { mainClass }` and the real class disagree, nothing was compiled, or the package is wrong | check `package com.gym;` in `MainApp.java` against `mainClass`; run `.\gradlew.bat classes` then `run`; it is a **class name**, never a module name |
| `Could not find or load main class com.gym.M3Samples` from `runSamples` | `M3Samples.java` is git-ignored, so your clone does not have it | expected — keep a local copy or use `.\gradlew.bat run` (§4.6) |
| `NullPointerException` inside `FXMLLoader`, or `getResource` returning `null` | the resource path is wrong: no leading `/`, wrong folder, or the file is not under `src/main/resources` | `getResource("/fxml/main.fxml")` means "root of the runtime classpath" = `build/resources/main`; the file must be `src/main/resources/fxml/main.fxml`. ⚠️ Windows is case-insensitive and Linux is not, so `Main.FXML` works locally and breaks elsewhere; after `gradlew classes`, `build/resources/main/fxml/main.fxml` existing is the proof |
| `SQLITE_BUSY` / `The database file is locked` | two writers at once: a second app instance, or two connections writing in the same instant | keep one `DbConnection` per app; close every `Connection`/`Statement`/`ResultSet` (try-with-resources); keep write transactions short; `PRAGMA busy_timeout = 5000;` makes SQLite wait instead of failing at once |
| `database is locked` at startup, or while deleting `gym.db` | an earlier run never exited and still holds the file (on Windows that also blocks deletion) | close the stray `java.exe` (Task Manager → Details, or `Get-Process java`), then delete `gym.db*` and rerun (§6) |
| daemon errors: "could not connect to the Gradle daemon", daemon disappears, `OutOfMemoryError` during a build | the background daemon is unhealthy, or its heap is too small | `.\gradlew.bat --stop`, then retry; if it repeats, create `gradle.properties` with `org.gradle.jvmargs=-Xmx2g` (the repo has no `gradle.properties` today) |
| `Task 'run' not found`, or `Project 'app' not found` | wrong directory, or a project path that does not exist | `cd` to the repository root (the folder with `gradlew.bat`); the app is the **root** project, so `run` takes no module prefix (§3) |
| `./gradlew: Permission denied` on macOS/Linux | the executable bit is not set on your checkout | `chmod +x gradlew`, then `./gradlew run`. `.gitattributes` already forces LF for code files, so this is permissions, not line endings |
| the build hangs before any task output | no network on the first run, or a corporate proxy | the wrapper caches the distribution in `%USERPROFILE%\.gradle\wrapper\dists` (verified: `gradle-9.8.0-bin` is cached here); run once with internet, or copy that folder from a teammate |
| blank, black or flickering window | a GPU/driver problem in JavaFX's renderer, not an app bug | force software rendering: `$env:JAVA_TOOL_OPTIONS='-Dprism.order=sw'` then `.\gradlew.bat run`; `-Dprism.verbose=true` prints which pipeline was chosen |

---

## 9. Team workflow

### 9.1 Branches and pull requests

| Rule | Detail | Source |
|---|---|---|
| Branch per task | `feat/<module>-<short-desc>` off `main`, e.g. `feat/payments-receipt-print` | `README.md` §8.1 |
| Ports get their own PR | branch off `swing` to port a module; never mix JavaFX and Swing code in one PR | `README.md` §8.3 |
| One PR per feature | with a screenshot or GIF of the UI change | `README.md` §8.2 |
| Build before pushing | `.\gradlew.bat build` must be green | `README.md` §8.4 |
| Status discipline | update the owning folder README's Status column (`To Do` → `In Progress` → `Done`) | `README.md` §8.5 |
| Error handling | validate every input and show a friendly message; stack traces go to the log, never to the user | `README.md` §8.4, §11 |

⚠️ `README.md` names only the `feat/…` prefix. If the team also wants `fix/…` or `docs/…`, that is a convention change: **Open detail (decide + log in `DECISIONS.md`)**.

### 9.2 Never commit these

| Path | Ignored by | Why | If it slipped into a commit |
|---|---|---|---|
| `build/`, `m3fx-*/build/`, `out/` | `.gitignore` 8–13, 111–113 | generated output, rewritten every build | `git rm -r --cached build` and commit the removal |
| `gym.db`, `gym.db-*`, `*.db` | `.gitignore` 103–106 | binary, machine-local, demo or real data, permanent conflicts | `git rm --cached gym.db`; rotate any real data it contained |
| `src/main/java/com/gym/M3Samples.java` | `.gitignore` 109 | scratch showcase window, not deliverable code | `git rm --cached` it and keep it local |
| `.env`, `.env.*`, `*.pem`, `*.p12`, `local.properties` | `.gitignore` 96–101 | secrets and machine paths | remove it **and** rotate the secret — git history keeps it |
| `.idea/*` except `misc.xml` (e.g. `workspace.xml`) | `.gitignore` 34–39 | personal layout and run configs | `git rm --cached .idea/workspace.xml` |
| `*.class`, `*.jar` (the wrapper JAR excepted) | `.gitignore` 2–6, 29–32 | compiled output | remove them; the wrapper JAR is the deliberate exception |

When unsure whether Git really ignores a path:

```text
git check-ignore -v build/                              # prints the matching .gitignore line, exit 0
git check-ignore -v gradle/wrapper/gradle-wrapper.jar    # exit 1 = NOT ignored (verified)
git status --ignored --short                             # everything Git is currently hiding
```

### 9.3 How the Swing port reuses this build

The layering exists so the port does not rewrite logic (`README.md` §7, `DECISIONS.md` 33):

| Layer | JavaFX branch | `swing` port |
|---|---|---|
| `model`, `dao`, `service`, most of `util` | written here first | **reused unchanged** — no JavaFX types, so they compile on the other branch |
| `controller` + `src/main/resources/fxml` | FXML controllers | **rewritten** as Swing panels, in a separate PR |
| `util/FxUtils` (dialogs) | JavaFX | replaced by a Swing equivalent |
| `m3fx-core` / `m3fx-controls` | the app's theme and controls | not used by the Swing branch |

Practical rule: a class that imports `javafx.*` cannot be reused by the port. That import boundary is the seam the folder READMEs defend.

---

## 10. What to check before saying "it works"

Run this list in order; every row is something you can show someone.

| # | Check | How | Passes when |
|---|---|---|---|
| 1 | build green on all three projects | `.\gradlew.bat build` | `BUILD SUCCESSFUL`, and the log names `:build`, `:m3fx-controls:build`, `:m3fx-core:build` (verified today: 15 tasks, no failures) |
| 2 | the app window opens | `.\gradlew.bat run` | a window appears; the task stays alive while it is open and returns the prompt when you close it |
| 3 | no stack trace in the console | read the `run` output | no `Exception`, no `Caused by`, no red text |
| 4 | the M3 theme is actually applied | the scene must be styled — `M3Stylesheets.applyTo(scene, M3Theme…)` before `stage.show()` | controls use `-md-sys-color-*` colours instead of default Modena greys |
| 5 | the database file is created | `Test-Path .\gym.db` (POSIX `ls -l gym.db`) after using a screen that reads or writes | the file exists at the working directory and grows after a save (§6) |
| 6 | one full CRUD round trip | on one screen: create a record, edit it, list it, delete it, restart the app | the record survives the restart while it exists, and is gone after the delete |
| 7 | failure paths stay friendly | type invalid input; point the app at a locked or read-only `gym.db` | a plain-language dialog, no stack trace shown to the user (`README.md` §11) |
| 8 | nothing unwanted reached Git | `git status --short` | only intended files — no `build/`, no `gym.db`, no `M3Samples.java` (§9.2) |

**Honest gaps in this guide.** These were *not* verifiable from the repository, so they are stated plainly instead of guessed:

- Writing this guide did not include launching the GUI, so row 2 is a check for a human, not a recorded result. What *is* verified: `.\gradlew.bat build` is green, and `MainApp.java` loads `/fxml/main.fxml`.
- ⚠️ Rows 4 and 5 do not pass today. `MainApp` has no `M3Stylesheets.applyTo(...)` call (`docs/COVERAGE.md` records "no theme call"), and no code creates `gym.db`: `model/`, `dao/`, `service/`, `controller/`, `util/` contain only `README.md` files, and `src/main/resources/db/` holds only `.gitkeep`. Section 6 documents the convention the DAO layer must implement.
- `src/main/resources/db/schema.sql` is not committed yet (`docs/COVERAGE.md` lists it unticked; `src/main/resources/README.md` calls it "planned Week 1").
- The nav shell and themed `MainApp` are blocked on a public `m3fx` overlay entry point (`DECISIONS.md` 35) — that is a library change, not an app change.

### Open details to settle later

Each of these is a real choice the repository does not make yet. Decide, then record it in `docs/DECISIONS.md` so the next reader does not re-open it.

| **Open detail (decide + log in `DECISIONS.md`)** | Why it is open | What it would change |
|---|---|---|
| add a formatter plugin (`spotless` with `google-java-format`) | `AGENTS.md` §6 asks for one formatter config; `PLAN.md` Phase 0 says none is set up in this repo | one plugin block in the root build; today every reviewer formats by hand |
| add app tests (`src/test`, JUnit 5, maybe TestFX) | the `java` plugin registers `test`, but the app has no test sources — verified `:test NO-SOURCE` — and `README.md` §11 says the app deliberately ships without automated tests | decides whether that stays deliberate; TestFX would be a new dependency needing approval (`AGENTS.md` §9) |
| put `options.release = 21` in the root `build.gradle` | the two module builds set it and the app does not — verified `MainApp.class` = major 71, `M3Theme.class` = major 65 (§4.2) — while `DECISIONS.md` 2 states the language level is 21 | one line, or an explicit note that the app is the exception |
| put `options.encoding = 'UTF-8'` in the root `build.gradle` | both module builds set it; the root build does not | avoids platform-default-encoding surprises for non-ASCII source on a teammate's machine |
| add a `gradle.properties` (daemon heap, build cache, configuration cache) | the repo has none, so Gradle defaults apply; Gradle 9.8 prints "Consider enabling configuration cache" on every build | faster repeat builds, but one committed file changes behaviour for everyone |
| decide the toolchain auto-download policy | Gradle 9.8 here reports `Auto-download: Enabled` and has already provisioned a Temurin JDK 17 into `%USERPROFILE%\.gradle\jdks` | either accept Gradle downloading JDKs (needs network), or pin `org.gradle.java.installations.paths` / disable auto-download and require a manual JDK 27 install |
