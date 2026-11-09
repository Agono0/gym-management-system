# BUILD_AND_RUN.md — build, run and verify the Gym Management System (JavaFX branch)

> **What this file owns:** the Gradle build, the exact run commands, the local SQLite file, the IntelliJ setup, and the checks to run before you say a task is done.
> **What it does not own:** class names, fields and rules (`docs/UML.md`), the *why* behind a choice (`docs/DECISIONS.md`), or layer teaching (the folder `README.md` files under `src/main/java/com/gym/`). `docs/DOCS_STANDARD.md` sets the voice; this is a build guide, so it uses the ten sections below instead of the layer-README skeleton.
> **Verified on:** Windows 11, `.\gradlew.bat --version` → **Gradle 9.8.0**, launcher JVM **27** (Oracle 27+35-2325), `JAVA_HOME=C:\Users\pc\.jdks\openjdk-27`, `.\gradlew.bat build` → `BUILD SUCCESSFUL`. Anything not verified that way is marked ⚠️.

---

## 1. Purpose

This guide takes a fresh clone to a running window, and tells you how to check that the run was real. The app is built by **Gradle** — a multi-project build with the app at the repo root plus the two `m3fx` library modules. Whoever grades or demos the project builds nothing by hand; they run one of two things:

| Path | Command | What it uses |
|---|---|---|
| Terminal (recommended) | `.\gradlew.bat runGym` (app) or `.\gradlew.bat run` (gallery) | the committed Gradle wrapper, which pins Gradle 9.8.0 |
| IntelliJ IDEA | Gradle tool window → Tasks → application → `runGym` / `run` | the same wrapper IntelliJ imported |

`run` compiles the app and starts `com.gym.MainApp` (`build.gradle` line 57), the m3fx component showcase gallery with a 19-screen interactive viewer. `runGym` starts `com.gym.GymApp` (`build.gradle` lines 75–85), the standalone Gym Management System application shell built from Java `ScreenController` views.

**Quick start:**

```text
git clone https://github.com/Agono0/gym-management-system.git
cd gym-management-system
.\gradlew.bat build        # POSIX: ./gradlew build
.\gradlew.bat runGym       # POSIX: ./gradlew runGym  (full Gym-OS shell)
.\gradlew.bat run          # POSIX: ./gradlew run     (m3fx gallery showcase)
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

The root build file is 86 lines. Read it in blocks:

| Lines | Block | What it gives you |
|---|---|---|
| 1–5 | `plugins { … }` | `java`, `application`, `org.openjfx.javafxplugin` (§4.1) |
| 7–8 | `group` / `version` | `com.gym` and `1.0.0` — the coordinates written into the jar |
| 10–14 | `java { toolchain { … } }` | JDK **27** toolchain for running Gradle compile tasks (§2.1) |
| 16–18 | `repositories { mavenCentral() }` | where `sqlite-jdbc`, JUnit, and JavaFX come from |
| 20–23 | `javafx { version; modules }` | JavaFX `21.0.4` with `javafx.controls` (§4.4) |
| 25–33 | `dependencies { … }` | `:m3fx-core`, `:m3fx-controls`, `sqlite-jdbc:3.45.1.0`, JUnit 5 (§4.3) |
| 35–44 | `test { … }` | JUnit Platform config (root app currently has no `src/test/` — `DECISIONS.md` 77) |
| 48–54 | `tasks.withType(JavaCompile)` | forces UTF-8, `--release 21` bytecode, `-Xlint:all,-processing`, `-Xmaxwarns 10000` |
| 56–58 | `application { mainClass = … }` | `com.gym.MainApp` (`gradlew run`) (§4.5) |
| 61–72 | `tasks.register('runSamples', JavaExec)` | local-only window (`com.gym.M3Samples`) (§4.6) |
| 75–85 | `tasks.register('runGym', JavaExec)` | full Gym-OS application shell (`com.gym.GymApp`) |

### 4.1 The three plugins

| Plugin | What it adds | Why this project needs it |
|---|---|---|
| `java` | `compileJava`, `classes`, `jar`, `test`, `build`, `clean`, source sets, the `build/` layout | the baseline: the app is plain Java compiled to `build/classes/java/main` |
| `application` | the `run` task, `installDist`, `distZip`, start scripts, and it reads the `application { … }` block | gives the grader a one-word start command instead of a hand-typed classpath |
| `org.openjfx.javafxplugin` (`0.1.0`) | platform-correct JavaFX artifacts and the module-path arguments for JavaExec/Test tasks | without it a JavaFX app started from a plain classpath dies with *"JavaFX runtime components are missing"* (§4.4) |

The version `0.1.0` is declared **once**, here; the two module builds apply the same plugin id with no version and resolve it from the root build — proven by the green `gradlew build`, which configures all three projects.

### 4.2 `group`, `version`, toolchain and Java 21 release target

- `group = 'com.gym'`, `version = '1.0.0'` → the jar is `gym-management-system-javafx-1.0.0.jar`.
- `java { toolchain { languageVersion = JavaLanguageVersion.of(27) } }` → Gradle runs javac from JDK 27.
- **`options.release = 21` everywhere (`DECISIONS.md` 2, 73).** All three projects (`build.gradle`, `m3fx-core/build.gradle`, `m3fx-controls/build.gradle`) compile with `options.release = 21` and `options.encoding = 'UTF-8'`, producing Java 21 bytecode (`class-file major 65`) with strict lint checking (`-Xlint:all,-processing`).

### 4.3 `repositories` and `dependencies`

`mavenCentral()` is the only repository: everything external (SQLite JDBC, JavaFX, JUnit 5) comes from it, while the `m3fx` modules come from this build.

| Dependency | Line | Kind | Why it is here |
|---|---|---|---|
| `implementation(project(':m3fx-core'))` | 27 | project dependency | the theme/token engine the app calls |
| `implementation(project(':m3fx-controls'))` | 28 | project dependency | the `M3*` controls the screens use |
| `implementation('org.xerial:sqlite-jdbc:3.45.1.0')` | 29 | runtime/compile driver | SQLite JDBC driver used by `DatabaseConnection` (§4.3.1) |

**4.3.1 Why a JDBC driver is a dependency.** **JDBC** is the JDK's standard database API: `java.sql.Connection`, `Statement`, `ResultSet`. Those interfaces ship inside the JDK (`java.sql`) without any vendor engine. The `sqlite-jdbc` jar implements them for SQLite and registers the `jdbc:sqlite:` driver with `DriverManager`. Without it `DatabaseConnection.getConnection()` fails with `No suitable driver found for jdbc:sqlite:gym.db`. Note: `sqlite-jdbc` depends transitively on `slf4j-api`, which Gradle puts on the classpath automatically.

**4.3.2 `implementation` vs `api`.**

| Keyword | On *your* compile classpath? | On the compile classpath of whoever depends on *you*? | Use it when |
|---|---|---|---|
| `implementation` | yes | **no** | the dependency is an internal detail — the default |
| `api` | yes | **yes** | a type from it appears in your public API (parameters, return types, fields) |

- `m3fx-controls/build.gradle` uses `implementation(project(':m3fx-core'))`: core is an internal detail of controls unless exposed in public signatures.
- The **app** uses `implementation` for `:m3fx-core`, `:m3fx-controls`, and `org.xerial:sqlite-jdbc:3.45.1.0`.

### 4.4 The `javafx { … }` block

```groovy
javafx {
    version = '21.0.4'
    modules = ['javafx.controls']
}
```

**JavaFX is not part of the JDK any more.** Since JDK 11 it is a separate set of modules downloaded like any other library, which is why a JavaFX project always pins a version and a module list.

| Module | Brings | The app uses it for |
|---|---|---|
| `javafx.controls` | `Button`, `Label`, `TableView`, `Control`/`Skin` bases (and transitively `javafx.base` + `javafx.graphics`) | every Java-built screen and every `M3*` control (`ScreenController`) |

All 19 Gym-OS screens were converted from FXML to pure Java constructors in commit `fb3486d` (`Screen.java`, `ScreenController.java`), so `javafx.fxml` is no longer needed in `build.gradle`. Legacy `.fxml` files remain in `src/main/resources/fxml/` as architectural reference artifacts only.

**What the plugin fixes.** Start a JavaFX `Application` subclass from a plain classpath and the JVM refuses:

```text
Error: JavaFX runtime components are missing, and the JavaFX modules are required to be on the module path
```

The plugin adds the platform-specific JavaFX jars and passes `--module-path` and `--add-modules javafx.controls` to `run`, `runGym`, `runSamples`, and `test`. ⚠️ The wiring covers *Gradle* tasks only: a bare `java -cp build/classes/java/main com.gym.GymApp` command hits that error unless module options are supplied (§7).

### 4.5 Entry-point tasks (`run` and `runGym`)

- `application { mainClass = 'com.gym.MainApp' }` → `.\gradlew.bat run` starts the m3fx showcase gallery and 19-screen previewer.
- `runGym` → `.\gradlew.bat runGym` starts `com.gym.GymApp` (the full Gym Management System window with splash/login gates and navigation rail).

### 4.6 The `runSamples` task

`runSamples` runs `com.gym.M3Samples`, a thin subclass alias (`M3Samples extends MainApp`) tracked in `src/main/java/com/gym/M3Samples.java`. Running `.\gradlew.bat runSamples` launches the same showcase gallery via `M3Samples.main`.

---

## 5. Everyday commands

Run everything from the **repository root** (the folder with `gradlew.bat`). In PowerShell the `.\` is required, because PowerShell does not execute programs from the current directory by name.

| What you want | Windows (PowerShell) | POSIX | What it does |
|---|---|---|---|
| Run the Gym-OS shell | `.\gradlew.bat runGym` | `./gradlew runGym` | compiles, then starts `com.gym.GymApp` (all 19 screens) |
| Run the m3fx gallery | `.\gradlew.bat run` | `./gradlew run` | compiles, then starts `com.gym.MainApp` (component gallery + screen viewer) |
| Build everything | `.\gradlew.bat build` | `./gradlew build` | assemble + check for all three projects |
| Build one library | `.\gradlew.bat :m3fx-core:build` | `./gradlew :m3fx-core:build` | only core and its tests (`:m3fx-controls:build` is the mirror image) |
| Delete build output | `.\gradlew.bat clean` | `./gradlew clean` | removes every `build/` directory; never touches sources |
| Run the gallery alias | `.\gradlew.bat runSamples` | `./gradlew runSamples` | starts `com.gym.M3Samples` (forwards to `MainApp`) |
| Stop the daemon | `.\gradlew.bat --stop` | `./gradlew --stop` | shuts down the background Gradle daemon (§8) |
| See running daemons | `.\gradlew.bat --status` | `./gradlew --status` | daemon PIDs and idle time |
| List tasks | `.\gradlew.bat tasks` | `./gradlew tasks` | add `--all` for every task in every project |
| See the dependency tree | `.\gradlew.bat dependencies` | `./gradlew dependencies` | per project: `.\gradlew.bat :m3fx-core:dependencies` |
| See the projects | `.\gradlew.bat projects` | `./gradlew projects` | confirms the root project and the two modules (§3) |
| See detected JDKs | `.\gradlew.bat -q javaToolchains` | `./gradlew -q javaToolchains` | what the toolchain block will pick (§2.1) |
| Debug the app | `.\gradlew.bat runGym --debug-jvm` | `./gradlew runGym --debug-jvm` | starts suspended on port 5005 |
| Re-download dependencies | `.\gradlew.bat build --refresh-dependencies` | `./gradlew build --refresh-dependencies` | when a cached artifact looks corrupt |

Notes that save time:

- The first `build` resolves dependencies from mavenCentral, so it needs internet; later builds are incremental.
- `gradlew build` runs `test` too. In the root app, `:test` reports `NO-SOURCE` because the gym app contains no `src/test` files (`DECISIONS.md` 77). The `:m3fx-core` and `:m3fx-controls` modules still run their JUnit 5 suites.
- `gradlew run` and `gradlew runGym` **block** while the window is open; closing the window ends the task. Run them in one terminal and use Git in another.

### 5.1 What `build/` contains, and why Git ignores it

| Path | Contents |
|---|---|
| `build/classes/java/main` | compiled app classes (`com/gym/GymApp.class`, `model/*`, `dao/*`, `util/*`, `exception/*`, `controller/*`) |
| `build/resources/main` | everything copied from `src/main/resources` (`images/`, legacy `fxml/`, …) |
| `build/libs` | `gym-management-system-javafx-1.0.0.jar` |
| `build/distributions` | `…-1.0.0.zip` and `…-1.0.0.tar` (the app plus runtime jars including `sqlite-jdbc`) |
| `build/reports`, `build/tmp`, `build/generated`, `build/scripts` | test reports, intermediate files, generated start scripts |
| `m3fx-core/build`, `m3fx-controls/build` | the same layout for each library module |

`build/` is **generated**, so `.gitignore` lines 8–13 ignore `build/`, `target/`, `out/`, `bin/`, `dist/`, and lines 111–113 repeat it for the two modules. Deleting `build/` is always safe — that is what `gradlew clean` does — and editing anything inside it is wasted work, because the next build overwrites it.

---

## 6. Where the database file and serialization files go

The database is one SQLite file, `gym.db`. It is **created at runtime** by `DatabaseConnection.initializeDatabase()`, never committed, and its location is decided by the **working directory of the running process**:

| How you started the app | Working directory | Where `gym.db` appears |
|---|---|---|
| `.\gradlew.bat runGym` from the repo root | the repo root (Gradle sets a JavaExec task's working directory to the project directory) | `C:\…\JAVA-Project\gym.db` |
| §7.1 IntelliJ Gradle `runGym` / `run` task | the repo root | the same file |
| §7.2 IntelliJ "Application" configuration | whatever its *Working directory* field says — set it to the repo root | repo root |
| A packaged distribution (`build/install/.../bin/…`) | wherever you launch the script | that folder |

Rules to keep in mind:

- **One file, one machine.** Two teammates never share a `gym.db`. Calling `DatabaseConnection.initializeDatabase()` runs all 13 `CREATE TABLE IF NOT EXISTS` statements automatically.
- **No `schema.sql` file needed in `resources/db/`.** In Milestone 2 (`DECISIONS.md` 77), all 13 `CREATE TABLE IF NOT EXISTS` statements live directly inside `DatabaseConnection.initializeDatabase()` in `src/main/java/com/gym/util/DatabaseConnection.java`, so `src/main/resources/db/` stays with `.gitkeep` and `README.md`.
- **Never commit `gym.db` or `.ser` files.** `.gitignore` covers `gym.db`, `gym.db-*`, and `*.db`; do not commit binary backup files created with `SerializationUtil.saveToFile(...)` either.

**Getting a clean database.** Stop the app first (a running instance holds the file open), delete the file and its sidecars, then re-initialize:

| Windows (PowerShell) | POSIX |
|---|---|
| `Remove-Item -Force -ErrorAction SilentlyContinue .\gym.db, .\gym.db-journal, .\gym.db-wal, .\gym.db-shm` | `rm -f gym.db gym.db-journal gym.db-wal gym.db-shm` |
| `Test-Path .\gym.db` (check) | `ls -l gym.db` (check) |

⚠️ Windows cannot delete a file another process has open. If `Remove-Item` says the file is in use, an earlier run is still alive (§8, *database is locked*).

---

## 7. IntelliJ run configuration

### 7.1 Recommended: run the Gradle task (no VM options)

1. **View → Tool Windows → Gradle** (IntelliJ imports the build when you open the root folder).
2. Expand `gym-management-system-javafx` → **Tasks** → **application**.
3. Double-click **`runGym`** (full app) or **`run`** (component gallery), or right-click → **Debug**.
4. Equivalent from the IntelliJ terminal: `.\gradlew.bat runGym` or `.\gradlew.bat run`.

### 7.2 A plain "Java Application" configuration (only if you need it)

Use this when you want an IntelliJ-native config with program arguments or a custom working directory.

| Field | Value |
|---|---|
| Main class | `com.gym.GymApp` (or `com.gym.MainApp`) |
| Use classpath of module | the **root** project (`gym-management-system-javafx.main`) |
| VM options | `--module-path "<folder containing the JavaFX jars>" --add-modules javafx.controls` |
| Working directory | the repository root — this decides where `gym.db` lands (§6) |

⚠️ If you run a plain CLI `java -cp ...` test/harness directly outside Gradle for DAOs only, remember `sqlite-jdbc` depends transitively on `slf4j-api`, so include `slf4j-api` alongside `sqlite-jdbc` on the classpath.

### 7.3 Module path vs classpath — the caveat in one place

| Fact | Consequence |
|---|---|
| The app has **no** `module-info.java` (only `m3fx-core` and `m3fx-controls` have one) | the app runs from the **classpath** |
| JavaFX must be on the **module path** | `--module-path`/`--add-modules javafx.controls` are required |
| the two `m3fx` modules + `sqlite-jdbc` arrive as `implementation` dependencies | the unnamed module reads them from the classpath |
| the JavaFX plugin supplies both arguments for `run` / `runGym` / `test` | §7.1 needs no manual VM options |

---

## 8. Troubleshooting

| Symptom | Likely cause | Fix |
|---|---|---|
| `Error: JavaFX runtime components are missing, and the JavaFX modules are required to be on the module path` | JavaFX classes sit on the classpath only: a bare `java -cp …`, or a §7.2 config without VM options | run `.\gradlew.bat runGym` (§7.1); or add `--module-path … --add-modules javafx.controls` |
| `UnsupportedClassVersionError` / "Unsupported class file major version" | an older JDK runs classes compiled with `--release 21` (major version `65`) | compare `java -version` with `.\gradlew.bat -q javaToolchains`; point IntelliJ's SDK and `JAVA_HOME` at JDK 21/27, then `.\gradlew.bat clean build` |
| Gradle reports *no matching toolchains found*, naming `languageVersion=27` | no JDK 27 on the machine | install JDK 27 or adjust `org.gradle.java.installations.paths` |
| `Error: Could not find or load main class com.gym.MainApp` | `application { mainClass }` and compiled output disagree | run `.\gradlew.bat classes` then `.\gradlew.bat run` |
| `DatabaseException: Could not connect to SQLite database` (`No suitable driver found`) | `sqlite-jdbc` (or transitive `slf4j-api` in manual CLI runs) missing from classpath | run via Gradle (`.\gradlew.bat build` / `runGym`), or put both `sqlite-jdbc` and `slf4j-api` on `-cp` |
| `SQLITE_BUSY` / `The database file is locked` | two writers at once or unclosed JDBC resource | always use `DatabaseConnection.getConnection()` inside try-with-resources; keep writes short |
| `database is locked` while deleting `gym.db` | an earlier Java process still holds `gym.db` open | close the stray `java.exe`, then delete `gym.db*` and rerun (§6) |
| daemon errors / `OutOfMemoryError` | unhealthy Gradle daemon | `.\gradlew.bat --stop`, then retry |
| `Task 'run' not found`, or `Project 'app' not found` | wrong directory or invalid `:app` prefix | `cd` to the repo root; the app is the root project |
| blank, black or flickering window | GPU driver issue in JavaFX Prism | `$env:JAVA_TOOL_OPTIONS='-Dprism.order=sw'`; then `.\gradlew.bat runGym` |

---

## 9. Team workflow

### 9.1 Branches and pull requests

| Rule | Detail | Source |
|---|---|---|
| Branch per task | `feat/<module>-<short-desc>` off `main` | `README.md` §8.1 |
| Ports get their own PR | branch off `swing` to port backend classes; never mix JavaFX and Swing code in one PR | `README.md` §8.3 |
| Build before pushing | `.\gradlew.bat build` must be green | `README.md` §8.4 |
| Status discipline | update `docs/UML_TEAM.md` and folder `README.md` Status column (`To Do` → `Done`) | `README.md` §8.5 |
| Error handling | catch `ValidationException` / `DatabaseException` and show user-readable messages | `README.md` §8.4, §11 |

### 9.2 Never commit these

| Path | Ignored by | Why |
|---|---|---|
| `build/`, `m3fx-*/build/`, `out/` | `.gitignore` | generated output, rewritten every build |
| `gym.db`, `gym.db-*`, `*.db`, `*.ser` | `.gitignore` | machine-local runtime database and serialized test dumps |
| `.env`, `.env.*`, `*.pem`, `*.p12`, `local.properties` | `.gitignore` | secrets and local configuration |
| `.idea/*` except `misc.xml` | `.gitignore` | personal workspace layouts |
| `*.class`, `*.jar` (wrapper JAR excepted) | `.gitignore` | compiled artifacts |

### 9.3 How the Swing port reuses this build

| Layer | JavaFX branch (`main`) | `swing` port |
|---|---|---|
| `model`, `dao`, `util`, `exception` | written here (`Done` in Milestone 2) | **reused unchanged** — zero JavaFX imports |
| `controller` (19 screens + shell) | Java-built JavaFX `ScreenController`s | **rewritten** as Swing panels |
| `m3fx-core` / `m3fx-controls` | theme and Material 3 controls | not used by the Swing branch |

---

## 10. What to check before saying "it works"

| # | Check | How | Passes when |
|---|---|---|---|
| 1 | build green on all three projects | `.\gradlew.bat build` | `BUILD SUCCESSFUL` (root compiles warning-free; `:m3fx-core:test` and `:m3fx-controls:test` pass; root `:test` is `NO-SOURCE`) |
| 2 | app window & gallery open cleanly | `.\gradlew.bat runGym` and `.\gradlew.bat run` | windows appear themed in dark/light with zero console exceptions |
| 3 | backend schema + DAOs + serialization | verify `DatabaseConnection.initializeDatabase()` + concrete DAO CRUD + `SerializationUtil` | SQLite creates all 13 tables in `gym.db`; `.ser` files round-trip entities via `Object` cast |
| 4 | nothing unwanted reached Git | `git status --short` | no `build/`, no `gym.db`, no `.ser`, no log files |

**Honest status today:**
- **Shipped (UI demo mode):** `MainApp`, `GymApp`, and all 20 Java-built controllers run cleanly (`Screen` registry, no runtime FXML loading).
- **Shipped (Milestone 2 backend):** All 13 `Serializable` entities, 11 enums, `GymException`/`ValidationException`/`DatabaseException`, `DatabaseConnection` (13 SQLite tables), `SerializationUtil`, and 13 concrete DAOs compile cleanly under Java 21 target with zero warnings.
- **Pending (Milestone 3):** Controllers currently show demo data and are ready to be wired directly to the shipped DAOs and `SerializationUtil` in the next milestone.

### Open details

| **Open detail** | Context |
|---|---|
| add a formatter plugin (`spotless`) | `AGENTS.md` §6 asks for a formatter on `m3fx`; none is configured yet |
| ~~add app tests~~ | `src/test/java/com/gym/` was removed per `DECISIONS.md` 77; library tests remain in `m3fx-core` and `m3fx-controls` |
| ~~put `options.release = 21` and `UTF-8` in root `build.gradle`~~ | settled and active in `build.gradle` |
| Milestone 3 UI-to-DAO wiring | connect each controller in `com.gym.controller` directly to `com.gym.dao` and `DatabaseConnection.initializeDatabase()` |
