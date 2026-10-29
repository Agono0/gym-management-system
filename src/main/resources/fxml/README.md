# `fxml` — Declarative screens: one file per controller (JavaFX branch)

> **Signature authority:** `docs/UML.md` §5 owns the 16 controller names, their methods and the FXML
> pairing; §2 owns the startup flow that loads the first screen. This README explains *why* the view is
> a separate XML file and *how* to write one. XML here is an illustrative sketch, not the
> specification — if a sketch and `docs/UML.md` disagree, `docs/UML.md` wins.
>
> **Not documented here:** what a controller *does*. Behaviour, service calls and error handling live in
> `src/main/java/com/gym/controller/README.md`. This README documents the **view**: node tree, `fx:id`s,
> event wiring, styling hooks.

## 1. Purpose — the problem this folder solves

An FXML file is a **declarative** description of a screen: it says *what the screen contains*, in XML,
instead of *how to build it*, in Java. `FXMLLoader` reads that XML, creates the JavaFX nodes, creates the
controller named in `fx:controller`, injects the nodes the controller asked for, and returns the finished
node tree. FXML is the **View** of MVC; the controller is the Controller; `model/` is the Model.

**What breaks if this folder is missing.** Every screen becomes a hand-written Java method that
constructs thirty nodes, sets padding on each and wires its own handlers. Layout and behaviour end up in
one file, so a spacing change and a business-rule change produce the same kind of diff; nobody can see a
screen without running it; and there is no file Scene Builder can open.

**What this folder deliberately does not do.** No SQL, no service calls, no business rule, no data
access. An FXML file cannot express rule 5 ("check-in requires an `ACTIVE` membership") and must not try:
it shows the check-in button, and `AttendanceService` decides whether the check-in is allowed
(`docs/UML.md` rules 1–19). It also stores no colours — the palette arrives from the theme at runtime.

## 2. What you learn here (OOP concepts)

Concept names match `docs/LEARNING_GUIDE.md` → "OOP concept index".

| Concept | Where in this folder | Why the project needs it | How to apply it (one line) |
|---|---|---|---|
| Separation of concerns | the whole folder | Structure in XML, behaviour in Java: each changes without the other | If a change needs a rule, it is not an FXML change |
| Composition | the node tree (containers in containers) | A screen is assembled from small parts (`VBox` → `HBox` → controls) | Nest a container when a group needs its own spacing or alignment |
| Information hiding | `fx:id`, `styleClass`, `%key` | The view names the few nodes Java must touch and hides the other thirty | Give an `fx:id` only to nodes the controller reads |
| Reflection (framework-side) | `fx:controller`, `fx:id`, `onAction` | The loader instantiates and wires a class the XML names only as a string | Remember that reflection fails at runtime, never at compile time |
| Declarative vs imperative | the whole folder | The layout is data, so it can be diffed and reviewed without compiling | Prefer an attribute over a Java statement |
| Single responsibility | one file per screen | A screen lives in one file, so a bug has one place to be | Never let two screens share one FXML |
| Naming as contract | `fx:id` ↔ `@FXML` field, `onAction="#method"` | The link between XML and Java is a spelling, so both sides must match exactly | Copy the name; never retype it |

## 3. Design patterns in this layer

| Pattern | Where | Why this pattern | What goes wrong if used carelessly |
|---|---|---|---|
| MVC (View half) | every `.fxml` file | The view is passive data: it renders what the controller puts into it | A view that computes turns into a second controller |
| Composite | the node tree (`VBox`, `HBox`, `GridPane`, `TableView`, …) | One uniform structure: a container holds any node, including another container | Over-nesting to fix spacing produces a tree nobody can reason about |
| Observer | `onAction="#handler"` and the event bubbling that reaches it | The control announces "something happened"; the controller reacts, and the control never knows the controller | Two handlers for one action makes the effect order-dependent |
| Factory (reflective) | `fx:controller="com.gym.controller.LoginController"` | The shell loads a screen by file name and gets a wired controller back | A controller without a public no-arg constructor cannot be built at all |
| Template Method | **rejected** | A `BaseController` would be near-empty: `initialize()` differs in every controller (`DECISIONS.md` 36) | Introducing one "to share code" hides which controller does what |
| Singleton | **avoided** | A screen is created per load; a cached static screen would keep stale rows | A static `Parent` survives `SessionContext.clear()` and leaks the previous user's data |

## 4. Rules you must follow (and the bug each rule prevents)

1. **One FXML file per controller, file name derived from the controller name.** The convention is
   `CustomerListController` ↔ `customer-list.fxml`. The exact spelling is an **Open detail (decide + log
   in `DECISIONS.md`)** — pick kebab-case or camelCase, write it down, never mix. Two spellings mean the
   shell asks for a file that does not exist and fails with a `NullPointerException` from `getResource`.
2. **`fx:controller` appears exactly once, on the root, holding the fully-qualified class name.** Two
   screens may not share a controller class: the loader creates a fresh controller per load, so a shared
   class would receive two different node sets into the same fields.
3. **Every class you name needs its own `<?import ?>` line.** FXML has no wildcard imports. The loader
   fails at parse time with "Class not found" for a class that clearly exists — the real cause is the
   missing import. Import only what you use.
4. **Declare both namespaces on the root and use the `fx:` prefix for every FXML attribute.** JavaFX
   needs `xmlns="http://javafx.com/javafx/21"` (control names) and `xmlns:fx="http://javafx.com/fxml/1"`
   (FXML attributes). Without the `fx` binding, `fx:id` is an unbound prefix and the file fails to load.
5. **Keep the XML prolog and UTF-8.** `<?xml version="1.0" encoding="UTF-8"?>` on line 1. An accented
   label without the encoding declaration is read with the platform default and reaches the screen as
   mojibake.
6. **Give an `fx:id` to every node the controller reads, spelled exactly like the matching `@FXML`
   field.** Injection is by name through reflection: `fx:id="customerTable"` needs
   `@FXML private TableView<Customer> customerTable;`. A typo does not fail the build — the field stays
   `null` and the first use throws a `NullPointerException` whose stack trace points at the controller,
   not at the XML.
7. **Shared objects such as a `ToggleGroup` are created once with `<fx:define>` and referenced by
   `$id`.** `fxml/main.fxml` lines 100–106 do exactly this: `<fx:define><ToggleGroup
   fx:id="planGroup"/></fx:define>` then `toggleGroup="$planGroup"` on two radio buttons. `$` means "the
   object defined under this `fx:id`". A second inline `ToggleGroup` leaves two independent groups, and
   the buttons stop behaving as one set.
8. **Wire events with `onAction="#methodName"`, and only to a handler FXML can call.** The handler takes
   no arguments or a single `ActionEvent`. The `int`-parameter methods in `docs/UML.md` §5
   (`onOpenProfile(int customerId)`, `onVoid(int paymentId)`, …) cannot be wired from XML: the controller
   calls them itself with the selected row's id. A load-time signature mismatch is the symptom of
   forgetting this.
9. **Text is a `%key`, not a sentence.** A translatable label is `text="%login.title"`, resolved from a
   resource bundle passed to the loader. A literal `text="Save member"` is acceptable only in the
   controller-free showcase `main.fxml`. The bundle's file name and location is an **Open detail (decide
   + log in `DECISIONS.md`)** — see `src/main/resources/README.md` §4 rule 4.
10. **Style only with `styleClass` and theme lookups.** Add a class name to the node; never write
    `style="-fx-background-color: #6750A4"`. An inline style beats the theme's stylesheet, survives the
    light/dark switch, and produces one light panel in a dark window (`DECISIONS.md` 4).
11. **`disable`, `visible` and `text` may be bound in FXML; decisions may not.** Binding a node to a
    controller property for presentation is fine. A binding that needs a domain fact ("is this
    membership frozen?") must get that fact from a service through the controller.
12. **Load on the FX thread, with a leading `/`.** `FXMLLoader.load(getClass().getResource(
    "/fxml/customer-list.fxml"))`. Loading builds nodes, which is FX-thread-only work; the leading slash
    is the classpath root rule from `src/main/resources/README.md` §4 rule 1.
13. **A controller-free file stays controller-free.** `main.fxml` is the component gallery: no
    `fx:controller`, no handlers, and it must not grow either, or throwaway showcase code joins the app.
14. **A password is never rendered by a plain text field, and never kept after use.** The library this
    project themes ships no masked control (only `M3TextField`), so the password node is JavaFX's own
    `javafx.scene.control.PasswordField`, given a `styleClass` so an app-level stylesheet can theme it
    with `-md-sys-color-*` lookups. A password typed into an ordinary text field is readable by anyone
    standing behind the user. `PasswordUtils.hash`/`verify` take the plain text, so the controller clears
    the field as soon as it has hashed it (`passwordField.clear()`) and never logs, stores or re-displays
    the value.

## 5. The files in this folder

Read order: `main.fxml` first (the only file that exists, and the only one with a real tree), then the 16
screen files — `login.fxml` before everything else because it is the only screen reachable before login,
`dashboard.fxml` next because it is what the shell opens after login, then the rest per owner.

The 16 files are grouped by owner, matching the screen inventory in
`src/main/java/com/gym/controller/README.md`. Each entry gives the FXML-side contract only: file name,
`fx:controller` value, root container, the `fx:id`s the controller reads, and the handlers XML may wire.
The **`fx:id` names are the view's half of the contract and are not listed in `docs/UML.md`** (§5 lists
controller methods). Treat them as the agreed sketch: use the name given, or change it here in the same
commit — never use two different names in the XML and the Java field.

Entry template (the `docs/DOCS_STANDARD.md` §3 per-file template adapted to a view file): *Status ·
Controller · Canonical · What it shows · Root and why · fx:id · Handlers · How to write it · Sketch ·
Verify · Mistakes*.

### `main.fxml` — the m3fx component showcase (owner: Abdelrhman)

**Status:** exists (155 lines) — **to be replaced**. **Canonical:** `docs/UML.md` §5: it is not one of
the 16 screens. `MainApp` loads it today (`src/main/java/com/gym/MainApp.java` line 13).

**What it shows.** A two-tab window (`M3Tabs`, tabs "Members" and "Schedule") rendering every control of
the external M3 library so the team can eyeball variants and states: buttons, text fields, chips, icon
buttons, a search bar, sliders, progress indicators, pickers, FABs, a FAB menu.

**Root and why.** `M3Tabs` with `variant="PRIMARY"` and both namespaces on the root. Tabs are the
cheapest way to put 25 components in one window; the padding `VBox` inside each tab (line 44) is the
layout the real screens copy.

**fx:id / handlers.** None, deliberately: no `fx:controller`, no `fx:id`, no `onAction`. The loader
builds the tree and returns a `Parent` with nothing to wire.

**How to write it.** You do not — it is a finished throwaway. Two things are worth copying into real
screens: the header block (lines 1–39: prolog, one `<?import ?>` per class, a comment saying who loads
it) and the `<fx:define>`/`$id` `ToggleGroup` pattern (lines 100–106).

**Sketch.** The pattern, not the file:

```xml
<!-- Testing only: no fx:controller, no onAction handlers -->
<Tabs xmlns="http://javafx.com/javafx/21" xmlns:fx="http://javafx.com/fxml/1">
  <tabs>
    <Tab text="Members" closable="false">
      <content><VBox spacing="16">…one block per component…</VBox></content>
    </Tab>
  </tabs>
</Tabs>
```

**Verify.** `gradlew run` opens both tabs and nothing throws. Once the nav shell exists, this file must
no longer be what `MainApp` loads.

**Mistakes.** Adding `fx:controller` "so the buttons do something" (it is a gallery, not a screen);
letting screen code grow here because it already loads; treating its literal `text="Save member"` labels
as the project's text convention.

### Abdelrhman's screens

#### `login.fxml` — the login screen (owner: Abdelrhman) — **worked example**

**Status:** planned. **Controller:** `fx:controller="com.gym.controller.LoginController"`.
**Canonical:** `docs/UML.md` §5 (`LoginController`) and §2 step 2 (the login flow).

**What it shows.** Two fields and two buttons: username, password, "Sign in" (`onLogin()`) and "Forgot
password?" (`onForgotPassword()`). Nothing else — no navigation, no data.

**Root and why.** `VBox`, centred. A login form is one column, so a `VBox` is enough; a `GridPane` would
add column bookkeeping for no benefit (the profile screens, which do have label/editor pairs, use one).

**fx:id.** `usernameField` (the library's text field), `passwordField` (JavaFX `PasswordField` — masked,
see rule 14), `loginButton`. Each must match its `@FXML` field exactly.

**Handlers.** `onAction="#onLogin"` and `onAction="#onForgotPassword"` — both no-argument in §5, which is
exactly why they can be wired from XML (rule 8).

**Why the password is not an `M3TextField`.** The library ships no masked field, so the only safe node is
`javafx.scene.control.PasswordField`, themed through `styleClass` (rule 14). Never "fix" the look by
falling back to the library's text field: a login form that shows the password in clear text is a defect,
not a styling compromise.

**How to write it.** 1. Copy the header block from `main.fxml` lines 1–2, then add one `<?import ?>` per
class you name (root container, `Label`, `Insets`, the library button and text field, and JavaFX's
`PasswordField`). 2. Put `fx:controller` and both namespace attributes on the root. 3. Add labels and
fields top to bottom in reading order, with `fx:id` on each node the controller reads. 4. Use the masked
`PasswordField` for the password, with a `styleClass` the app-level stylesheet can theme (rule 14).
5. Wire the two buttons. 6. Use `%key` text and a `styleClass`; no colour, no inline `style=`. 7. Load it
once by hand, confirm both `@FXML` fields are non-`null`, and confirm that a failed sign-in leaves the
password field empty.

**Sketch.**

```xml
<?xml version="1.0" encoding="UTF-8"?>

<?import io.m3fx.controls.button.M3Button?>
<?import io.m3fx.controls.textfield.M3TextField?>
<?import javafx.geometry.Insets?>
<?import javafx.scene.control.Label?>
<?import javafx.scene.control.PasswordField?>
<?import javafx.scene.layout.VBox?>

<!-- The only screen reachable before login (docs/UML.md §2). -->
<VBox xmlns="http://javafx.com/javafx/21" xmlns:fx="http://javafx.com/fxml/1"
      fx:controller="com.gym.controller.LoginController"
      alignment="CENTER" spacing="16" styleClass="login-root">
  <padding><Insets top="32" right="32" bottom="32" left="32"/></padding>

  <Label text="%login.title" styleClass="m3-headline-small"/>
  <M3TextField fx:id="usernameField" labelText="%login.username"/>
  <!-- Masked input: the library has no masked field, so this is JavaFX's PasswordField,
       themed via styleClass instead of being replaced by a plain text field (rule 14).
       `app-password-field` is an app-level class, defined in the app stylesheet (theme/README.md). -->
  <PasswordField fx:id="passwordField" promptText="%login.password" styleClass="app-password-field"/>
  <M3Button fx:id="loginButton" text="%login.submit" onAction="#onLogin"/>
  <M3Button text="%login.forgot" variant="TEXT" onAction="#onForgotPassword"/>
</VBox>
```

The matching controller skeleton — the view's half only. What `onLogin()` does is in
`src/main/java/com/gym/controller/README.md`; here we only care that injection works:

```java
package com.gym.controller;

// Sketch — the view's half of the contract. Behaviour is documented in controller/README.md.
public class LoginController {

    @FXML private M3TextField usernameField;    // io.m3fx.controls.textfield.M3TextField
    @FXML private PasswordField passwordField;  // javafx.scene.control.PasswordField — masked (rule 14)

    // public + no-arg: FXMLLoader calls it after injection (docs/UML.md §5, DECISIONS.md 36)
    public void initialize() {
        // presentation only — no service calls, no SQL
    }

    @FXML private void onLogin() {
        /* validate → AdminService.authenticate(...) → passwordField.clear() → SessionContext → dashboard */
    }

    @FXML private void onForgotPassword() { /* open the reset flow */ }
}
```

**Open detail (decide + log in `DECISIONS.md`).** Masked input has two possible shapes and the team has
not chosen one:

| Option | What it means | Cost |
|---|---|---|
| (a) Style JavaFX `PasswordField` from an app-level stylesheet, using the theme's `-md-sys-color-*` lookups — **recommended** | No library change; one app stylesheet carries the field rules (see `theme/README.md`) | The field must be kept visually in step with the library's text field by hand |
| (b) Request an `M3PasswordField` from the library | One consistent control with the rest of the form | A new library feature with its own review; the app may not fake it with `M3TextField` |

Until the choice is logged, option (a) is what the sketch above shows.

**Verify.** Loading returns a `VBox` without an exception, `initialize()` has run, both fields are
non-`null`, signing in with empty fields shows a validation message rather than an exception, and after
any sign-in attempt the password field is empty — `PasswordUtils` hashed the text and nothing keeps it.

**Mistakes.** Field named `userName` while the XML says `usernameField` (NPE on the first `getText()`,
with nothing pointing at the XML); `Insets` not imported, so the `<padding>` element fails to parse;
`onAction="#onLogin()"` with parentheses, which names a method that cannot exist; a default password
value written into the XML; using the library's plain text field for the password, so it renders in clear
text (rule 14); leaving the typed password in the field after a failed attempt, where the next person at
the keyboard can read it.

#### `customer-list.fxml` — the searchable customer list (owner: Abdelrhman)

**Status:** planned. **Controller:** `com.gym.controller.CustomerListController`. **Canonical:**
`docs/UML.md` §5.

**What it shows.** A search row (text field, status filter, search, add) above a `TableView` of
customers, with open-profile and delete row actions.

**Root and why.** `BorderPane`: the search bar in `top`, the table in `center`. A `TableView` must
expand, so it belongs in `center` and never in a `VBox` that would let it collapse.

**fx:id.** `searchField`, `statusFilter`, `customerTable`, `nameColumn`, `phoneColumn`, `statusColumn`,
`trainerColumn`.

**Handlers.** `onAction="#onSearch"`, `onAction="#onAdd"`. `onOpenProfile(int customerId)` and
`onDelete(int customerId)` are called with the selected row's id (rule 8).

**How to write it.** 1. Declare the columns with `fx:id` and `%key` header text. 2. Leave
`cellValueFactory` to `initialize()` — it is a lambda and cannot live in XML. 3. Add a `<placeholder>`
label so an empty search explains itself.

**Sketch.**

```xml
<BorderPane xmlns="http://javafx.com/javafx/21" xmlns:fx="http://javafx.com/fxml/1"
            fx:controller="com.gym.controller.CustomerListController">
  <top><HBox spacing="8">
    <M3TextField fx:id="searchField" labelText="%customerList.search"/>
    <M3Button text="%customerList.searchBtn" onAction="#onSearch"/>
    <M3Button text="%customerList.add" onAction="#onAdd"/>
  </HBox></top>
  <center><TableView fx:id="customerTable">
    <columns>
      <TableColumn fx:id="nameColumn"  text="%customerList.col.name"/>
      <TableColumn fx:id="phoneColumn" text="%customerList.col.phone"/>
    </columns>
    <placeholder><Label text="%customerList.empty"/></placeholder>
  </TableView></center>
</BorderPane>
```

**Verify.** Headers render with zero rows; searching filters through `CustomerService`; "delete" sets
`INACTIVE` (rule 4) instead of removing the row.

**Mistakes.** Setting `items` in FXML (data comes from a service, and `items` is a list property anyway);
one `TableColumn<Customer, Object>` reused for every column, which breaks the cell typing; treating
"delete" as removing the row from the table's list.

#### `customer-profile.fxml` — the full member profile (owner: Abdelrhman)

**Status:** planned. **Controller:** `com.gym.controller.CustomerProfileController`. **Canonical:**
`docs/UML.md` §5.

**What it shows.** An edit form for the `Customer` fields plus sections for membership, trainer, workout
program, schedule, progress and a medical summary that opens `medical-profile.fxml`.

**Root and why.** `BorderPane` with a `TabPane` in `center`: several independent sections belong in tabs,
not in one long column, and each tab can be reloaded on its own.

**fx:id.** `fullNameField`, `phoneField`, `emailField`, `addressField`, `statusBox`, `membershipList`,
`programList`, `progressChart`, `medicalSummary`.

**Handlers.** `onAction="#onSave"`. `showCustomer(int customerId)` is called by the previous screen's
`onOpenProfile` and reloads the sections after a save.

**How to write it.** 1. Use a `GridPane` with label and editor columns so labels line up without
hand-tuned padding. 2. Mark read-only sections `editable="false"` rather than removing them — an empty
section tells the user nothing. 3. Keep the medical section a summary with a button to the medical
screen: the full record has its own screen and its own access rule.

**Sketch.**

```xml
<GridPane hgap="12" vgap="8">
  <Label text="%customer.name"  GridPane.columnIndex="0" GridPane.rowIndex="0"/>
  <M3TextField fx:id="fullNameField" GridPane.columnIndex="1" GridPane.rowIndex="0"/>
  <Label text="%customer.phone" GridPane.columnIndex="0" GridPane.rowIndex="1"/>
  <M3TextField fx:id="phoneField"    GridPane.columnIndex="1" GridPane.rowIndex="1"/>
</GridPane>
```

**Verify.** Opening a customer with no membership shows empty sections, not an exception; saving goes
through `CustomerService`, never through a DAO.

**Mistakes.** Offering a "freeze" toggle that contradicts rule 14 (a `SUSPENDED` customer implies frozen
memberships); forgetting an `fx:id` on a field the controller reads back; putting a trainer's *name* in a
field instead of resolving `trainerId` through `TrainerService`.

#### `medical-profile.fxml` — medical profile view/edit (owner: Abdelrhman)

**Status:** planned. **Controller:** `com.gym.controller.MedicalProfileController`. **Canonical:**
`docs/UML.md` §5, rule 3 (at most one profile per customer, created on demand).

**What it shows.** Blood type, conditions, allergies, medications, injuries, doctor name and phone,
notes, last-updated date, and save/delete actions.

**Root and why.** `VBox` of label/editor pairs (a `GridPane` if the labels need aligning). One record,
so no table and no master/detail split.

**fx:id.** `bloodTypeField`, `conditionsArea`, `allergiesArea`, `medicationsArea`, `injuriesArea`,
`doctorNameField`, `doctorPhoneField`, `notesArea`, `updatedLabel`.

**Handlers.** `onAction="#onSave"`, `onAction="#onDelete"`. `showProfile(int customerId)` is called by the
customer and trainer screens.

**How to write it.** 1. Use multi-line `TextArea`s for conditions/allergies/medications/injuries — one
record can hold several entries and a single-line field hides the rest. 2. Make the "no profile yet"
state explicit with a label and an add path, because "absent" is normal (rule 3). 3. Add no customer
selector: the screen is opened *for* a customer, and switching inside it is how a record is saved
against the wrong person.

**Sketch.**

```xml
<VBox spacing="12">
  <M3TextField fx:id="bloodTypeField" labelText="%medical.bloodType"/>
  <TextArea    fx:id="conditionsArea" promptText="%medical.conditions"/>
  <Label       fx:id="updatedLabel"   text="%medical.notRecorded"/>
  <HBox spacing="8">
    <M3Button text="%common.save"   onAction="#onSave"/>
    <M3Button text="%common.delete" variant="TEXT" onAction="#onDelete"/>
  </HBox>
</VBox>
```

**Verify.** Deleting the profile also clears the summary section on the profile screen; saving twice
does not create a second row for the same customer.

**Mistakes.** Treating "no profile" as an error dialog; giving a trainer edit rights (only the assigned
trainer may *read* it, checked in `MedicalProfileService.getForTrainer`); reusing the customer's
emergency-phone field for the doctor's phone.

#### `staff.fxml` — staff management and admin overrides (owner: Abdelrhman)

**Status:** planned. **Controller:** `com.gym.controller.StaffController`. **Canonical:** `docs/UML.md`
§5, rule 16 (`StaffRole.ADMIN` required for every operation and override).

**What it shows.** The staff roster with role and active flag, add/edit/delete/reset-password actions,
and the admin overrides: reassign trainer, adjust payment.

**Root and why.** `BorderPane`: toolbar in `top`, `staffTable` in `center`, the override actions in
`bottom` — separated so "Reassign trainer" is not mistaken for a routine row edit.

**fx:id.** `staffTable`, `fullNameColumn`, `usernameColumn`, `roleColumn`, `activeColumn`, `roleBox`,
`passwordField` (JavaFX `PasswordField`, reset only — masked and write-only; rule 14).

**Handlers.** `onAction="#onAddStaff"`, `onAction="#onReassign"`. `onEditStaff(int)`,
`onDeleteStaff(int)`, `onResetPassword(int)` and `onAdjustPayment(int)` are called with the selected row.

**How to write it.** 1. Guard the whole screen at the shell:
`SessionContext.requireRole(StaffRole.ADMIN)` — FXML cannot enforce a role. 2. Never render
`passwordHash`; the columns are full name, username, role, active. 3. Keep the reset-password field out
of the "add staff" form, and clear it as soon as `AdminService.resetPassword` returns: a leftover value
gets submitted with the next action, and `PasswordUtils` needs the plain text only for that one call
(rule 14).

**Sketch.**

```xml
<BorderPane fx:controller="com.gym.controller.StaffController"
            xmlns="http://javafx.com/javafx/21" xmlns:fx="http://javafx.com/fxml/1">
  <top><HBox spacing="8">
    <M3Button text="%staff.add" onAction="#onAddStaff"/>
    <M3Button text="%staff.override.reassign" variant="TONAL" onAction="#onReassign"/>
  </HBox></top>
  <center><TableView fx:id="staffTable">
    <columns>
      <TableColumn fx:id="fullNameColumn" text="%staff.col.name"/>
      <TableColumn fx:id="roleColumn"     text="%staff.col.role"/>
      <TableColumn fx:id="activeColumn"   text="%staff.col.active"/>
    </columns>
  </TableView></center>
</BorderPane>
```

**Verify.** With a non-admin signed in the screen is unreachable; the roster never shows a hash; deleting
a referenced staff row is refused by the service with a readable message.

**Mistakes.** Binding the role column to an editable cell (role changes are an `AdminService`
operation); rendering the boolean `active` as `1`/`0`; letting the screen call `onEditStaff(id)` for a
row that is no longer selected.

### Ziad's screens

#### `trainer-list.fxml` — the trainer roster (owner: Ziad)

**Status:** planned · **Controller:** `com.gym.controller.TrainerListController` · **Canonical:** `docs/UML.md` §5.
**Shows.** A `TableView` of trainers (name, specialization, experience, `hourlyRate`) with "Add" and per-row open/delete actions.
**Root and why.** `BorderPane`: toolbar in `top`, table in `center`, so the table can expand. The same shape as `customer-list.fxml`, on purpose — two list screens that look alike cost nothing and teach one layout.
**fx:id.** `trainerTable`, `nameColumn`, `specializationColumn`, `rateColumn`.
**Handlers.** `#onAdd`; `onOpenProfile(int trainerId)` and `onDelete(int trainerId)` are called with the selected id (rule 8).
**How to write it.** 1. Keep to the four columns `TrainerDao.findAll` can fill without a join. 2. Format `hourlyRate` from the `BigDecimal` in a `cellFactory`; never re-parse the cell text. 3. Add a `<placeholder>` for the empty roster.

```xml
<TableView fx:id="trainerTable">
  <columns>
    <TableColumn fx:id="nameColumn"           text="%trainer.col.name"/>
    <TableColumn fx:id="specializationColumn" text="%trainer.col.specialization"/>
    <TableColumn fx:id="rateColumn"           text="%trainer.col.rate"/>
  </columns>
</TableView>
```

**Verify.** The rate shows two decimals from a `BigDecimal`; opening a profile passes the row's `id`, not its name.
**Mistakes.** Showing availability here (that is `availability.fxml`, from a fresh `findByTrainerAndDate` query); an editable rate cell, which bypasses `TrainerService` validation; using the row index as the id.

#### `trainer-profile.fxml` — trainer details and assigned customers (owner: Ziad)

**Status:** planned · **Controller:** `com.gym.controller.TrainerProfileController` · **Canonical:** `docs/UML.md` §5, rule 15 (`Customer.trainerId` is the authoritative link).
**Shows.** Editable trainer fields (specialization, experience, certifications, hire date, rate), the customers assigned to this trainer, and a read-only medical section opened by `showMedicalProfile(int customerId)`.
**Root and why.** `BorderPane` with a `TabPane` in `center`: three unrelated sections (own fields, roster, medical) belong in tabs, and each can load on demand.
**fx:id.** `specializationField`, `experienceField`, `certificationsArea`, `rateField`, `assignedTable`, `medicalArea`.
**Handlers.** `#onSave`; `showTrainer(int trainerId)` and `showMedicalProfile(int customerId)` are called with an id.
**How to write it.** 1. Build the medical tab only when `MedicalProfileService.getForTrainer(...)` returns a profile — that method *is* the ownership check. 2. Keep that tab read-only: no editors, no save button. 3. Fill the roster from `CustomerService.findByTrainer(id)`, not from a `TrainerSubscription` query.

```xml
<TabPane>
  <tabs>
    <Tab text="%trainer.tab.details"><content><VBox>…fields…</VBox></content></Tab>
    <Tab text="%trainer.tab.customers"><content><TableView fx:id="assignedTable"/></content></Tab>
    <Tab text="%trainer.tab.medical"><content><TextArea fx:id="medicalArea" editable="false"/></content></Tab>
  </tabs>
</TabPane>
```

**Verify.** After reassigning a customer to another trainer, that customer leaves this trainer's roster *and* medical tab in the same session.
**Mistakes.** Deriving the roster from `TrainerSubscription` (that table records the paid period, not the assignment — rule 15); an editable medical section; hiding the medical tab with `visible="false"` instead of not building it, which leaves the data one CSS fix from being visible.

#### `availability.fxml` — trainer calendar and slot caps (owner: Ziad, screen)

**Status:** planned · **Controller:** `com.gym.controller.AvailabilityController` · **Canonical:** `docs/UML.md` §5, rule 8 (several slots per day; `maxCustomers` is per slot).
**Shows.** One trainer and day, that day's hourly slots with their cap and status, and the controls to publish slots and set a cap.
**Root and why.** `BorderPane`: trainer and date selection in `top`, the slot table in `center`, the publish controls in `bottom`.
**fx:id.** `datePicker`, `slotsTable`, `startTimeColumn`, `endTimeColumn`, `capColumn`, `statusColumn`, `startTimeField`, `endTimeField`, `slotCapField`.
**Handlers.** None can be wired: every §5 method takes parameters (`setCalendar(int, String)`, `setSlots(int, String, String)`, `setSlotCap(int, int)`). XML wires the controls by `fx:id`; the controller calls these methods from its own selection handlers and `initialize()`.
**How to write it.** 1. Query a list, not a row — `findByTrainerAndDate` returns several slots (rule 8). 2. Offer a per-slot cap and a separate daily cap that the service applies across the day. 3. Show `AvailabilityStatus.FULL` as computed and never editable (rule 13).

```xml
<TableView fx:id="slotsTable">
  <columns>
    <TableColumn fx:id="startTimeColumn" text="%availability.col.start"/>
    <TableColumn fx:id="capColumn"       text="%availability.col.cap"/>
    <TableColumn fx:id="statusColumn"    text="%availability.col.status"/>
  </columns>
</TableView>
```

**Verify.** Publishing two slots for one day gives two rows; a slot whose `CONFIRMED` bookings reach the cap shows `FULL` and leaves the booking list; cancelling a booking makes it bookable again (rule 17).
**Mistakes.** One row per day instead of one row per slot; a free-text status column; a locale-dependent time formatter instead of the `HH:mm` strings `DateUtils` owns (rule 9).

#### `membership.fxml` — plans: sell, renew, freeze, unfreeze, cancel (owner: Ziad)

**Status:** planned · **Controller:** `com.gym.controller.MembershipController` · **Canonical:** `docs/UML.md` §5, rules 7, 13, 14.
**Shows.** The selected customer's memberships (plan, price, start/end, status, frozen days) and the five actions.
**Root and why.** `BorderPane`: customer/plan selection in `top`, the membership table in `center`, the action row in `bottom`. The action row is separate so "Cancel" is deliberate, not a stray click next to a table row.
**fx:id.** `customerBox`, `membershipTable`, `planColumn`, `endDateColumn`, `statusColumn`, `frozenDaysColumn`, `planSelect`, `priceField`, `startDatePicker`.
**Handlers.** All five are no-argument in §5: `#onSell`, `#onRenew`, `#onFreeze`, `#onUnfreeze`, `#onCancel`.
**How to write it.** 1. Show `price` formatted from `BigDecimal` and let `MembershipService.sell` compute the amount for a `PlanType`; the view never multiplies. 2. Disable "Freeze" unless the row is `ACTIVE` and "Unfreeze" unless it is `FROZEN` (rule 7) — disable rather than hide, so the reason stays visible. 3. Show `frozenDays` read-only: written by `freeze`, cleared by `unfreeze`.

```xml
<HBox spacing="8">
  <M3Button text="%membership.sell"     onAction="#onSell"/>
  <M3Button text="%membership.freeze"   onAction="#onFreeze"/>
  <M3Button text="%membership.unfreeze" onAction="#onUnfreeze"/>
  <M3Button text="%membership.cancel" variant="TEXT" onAction="#onCancel"/>
</HBox>
```

**Verify.** Freeze then unfreeze shifts `endDate` by the frozen period and clears `frozenDays`; renewing a `CANCELLED` membership is refused with a readable message; a suspended customer's memberships show `FROZEN` (rule 14).
**Mistakes.** Computing `endDate` in the controller instead of through `DateUtils` (rule 9); offering "Cancel" as a delete (rule 4 — cancel keeps the row); letting the screen set `EXPIRED` by hand (rule 13).

### Yousef's screens

#### `workout.fxml` — program builder and reassignment (owner: Yousef)

**Status:** planned · **Controller:** `com.gym.controller.WorkoutController` · **Canonical:** `docs/UML.md` §5.
**Shows.** The customer's programs (name, version, creator, created date), the selected version's exercise text, and build/reassign/new-version/delete actions.
**Root and why.** `SplitPane`: program list left, exercises right. A split lets the user trade list width against text width, which a `VBox` cannot.
**fx:id.** `programTable`, `versionColumn`, `createdColumn`, `exercisesArea`, `nameField`, `creatorBox`.
**Handlers.** `#onBuild`, `#onReassign`, `#onNewVersion`, `#onDelete` — all no-argument in §5.
**How to write it.** 1. Show `version` as a value the service assigned; the user never types it. 2. Keep `exercises` in a wrapping `TextArea` — one text field in the model, not a table of sets and reps. 3. Make "New version" primary and "Delete" a text action, because deleting destroys history.

```xml
<SplitPane dividerPositions="0.4">
  <items>
    <TableView fx:id="programTable">
      <columns>
        <TableColumn fx:id="versionColumn" text="%workout.col.version"/>
        <TableColumn fx:id="createdColumn" text="%workout.col.created"/>
      </columns>
    </TableView>
    <TextArea fx:id="exercisesArea" wrapText="true"/>
  </items>
</SplitPane>
```

**Verify.** Building for a customer who has version 3 produces version 4; reassigning changes `creatorTrainerId` without changing the customer.
**Mistakes.** Editing an old version in place instead of creating a new one; a free-text creator field instead of trainer options from `TrainerService`; setting `createdDate` from the system clock in the view.

#### `schedule.fxml` — agenda, booking and subscriptions (owner: Yousef)

**Status:** planned · **Controller:** `com.gym.controller.ScheduleController` · **Canonical:** `docs/UML.md` §5, rules 1, 10, 11, 17, 18.
**Shows.** The agenda for a day (or a customer/trainer), the booking dialog, the session state actions (confirm, complete, no-show, cancel with reason, reserve) and the subscribe/renew/cancel-subscription flow.
**Root and why.** `BorderPane`: filters in `top`, `sessionTable` in `center`, the action toolbar in `bottom`. The booking dialog is a stage or overlay the controller builds, not a second FXML file, unless the team decides otherwise.
**fx:id.** `datePicker`, `trainerBox`, `customerBox`, `sessionTable`, `timeColumn`, `statusColumn`, `cancelReasonField`, `slotList`, `subscriptionTable`, `availableTrainerList`.
**Handlers.** The no-argument ones: `#onBook`, `#onRequest`, `#onReserve`, `#onCancel`, `#onSubscribe`, `#onRenewSubscription`, `#onCancelSubscription`, `#showAvailableTrainers`. `onConfirm(int sessionId)`, `onComplete(int sessionId)`, `onNoShow(int sessionId)` and `showTrainerSlots(int trainerId)` take an id.
**How to write it.** 1. Put the row-state actions in a `ContextMenu` and a toolbar; because `MenuItem`s need no-argument handlers, the controller installs a selection listener in `initialize()` and calls `onConfirm(sessionId)` itself — which is why `sessionTable` needs an `fx:id` even though XML wires nothing to it. 2. Disable the actions rule 18 forbids for the selected status; the service still enforces the state machine. 3. Show the 2-hour cancellation window as a hint and let `DateUtils.hoursUntil` decide (rule 10). 4. Keep the dialog's slot list and the agenda table as two different node trees.

```xml
<TableView fx:id="sessionTable">
  <columns>
    <TableColumn fx:id="timeColumn"   text="%schedule.col.time"/>
    <TableColumn fx:id="statusColumn" text="%schedule.col.status"/>
  </columns>
</TableView>
```

**Verify.** A subscription-covered session shows price zero; cancelling frees capacity in its slot (rule 17); "Complete" is disabled for a `REQUESTED` session (rule 18); reserve without `ADMIN` is refused (rule 16).
**Mistakes.** Setting a status in the view instead of calling the service (the state machine then exists twice); letting a membership payment and a subscription cover the same session (rule 11); building a date string in the dialog instead of passing it through `DateUtils` (rule 9).

#### `equipment.fxml` — inventory list and maintenance (owner: Yousef)

**Status:** planned · **Controller:** `com.gym.controller.EquipmentController` · **Canonical:** `docs/UML.md` §5; `DECISIONS.md` 30 (`Equipment` is a standalone aggregate).
**Shows.** The inventory table (name, category, purchase date, condition, status, last maintenance) with add, status change, condition change, log-maintenance and delete actions.
**Root and why.** `BorderPane`: filters and "Add" in `top`, `equipmentTable` in `center`, the change actions in `bottom`. Nothing here needs a customer or trainer selector, because `Equipment` has no foreign key.
**fx:id.** `equipmentTable`, `nameColumn`, `categoryColumn`, `conditionColumn`, `statusColumn`, `maintenanceColumn`, `conditionSelect`, `statusSelect`, `maintenanceDatePicker`.
**Handlers.** `#onAdd`; `onStatusChange(int, EquipmentStatus)`, `onConditionChange(int, EquipmentCondition)`, `onLogMaintenance(int, String)` and `onDelete(int)` take an id (and an enum value).
**How to write it.** 1. Fill the two selects from the `EquipmentStatus` and `EquipmentCondition` literals, so the UI cannot invent a value. 2. Restrict the maintenance date picker to today or earlier. 3. Show `RETIRED` items dimmed rather than removing them — the row is history.

```xml
<TableView fx:id="equipmentTable">
  <columns>
    <TableColumn fx:id="nameColumn"        text="%equipment.col.name"/>
    <TableColumn fx:id="conditionColumn"   text="%equipment.col.condition"/>
    <TableColumn fx:id="statusColumn"      text="%equipment.col.status"/>
    <TableColumn fx:id="maintenanceColumn" text="%equipment.col.lastMaintenance"/>
  </columns>
</TableView>
```

**Verify.** A status change goes through `EquipmentService` and survives a restart; a category icon resolves from `images/` or falls back to a text label.
**Mistakes.** Free-text condition/status cells; adding a customer column because other screens have one (there is no such link); deleting equipment instead of retiring it (rule 4).

### Abdel Raouf's screens

#### `dashboard.fxml` — the dashboard and the shell's landing screen (owner: Abdel Raouf)

**Status:** planned · **Controller:** `com.gym.controller.DashboardController` · **Canonical:** `docs/UML.md` §5 and §2 steps 3–4 (opened after login; logout clears `SessionContext`).
**Shows.** Summary cards — active members, today's sessions, a revenue snapshot — plus the navigation chrome and the logout action. It is the only screen that can end the session.
**Root and why.** `BorderPane`: navigation in `top` (or a rail at the left), cards in a `FlowPane` in `center` so they reflow when the window narrows instead of clipping.
**fx:id.** `activeMembersLabel`, `todaySessionsLabel`, `revenueLabel`, `logoutButton`, plus any navigation node the shell does not own.
**Handlers.** `onAction="#onLogout"` — no-argument in §5. `refreshData()` is called from `initialize()`, not wired as an event.
**How to write it.** 1. One card per figure, each holding a caption and a value label the controller writes; put no `TableView` here — the dashboard summarises, the other screens detail. 2. Format money from `BigDecimal` in the controller and pass a finished string to the label (`DECISIONS.md` 31); FXML does no arithmetic. 3. Keep the navigation chrome in the shell (`MainApp`) unless the team decides the dashboard owns it — a nav bar duplicated per screen is how "which screen am I on?" bugs start.

```xml
<FlowPane hgap="16" vgap="16">
  <M3Card variant="ELEVATED">
    <content><VBox spacing="4">
      <Label text="%dashboard.activeMembers"/>
      <Label fx:id="activeMembersLabel" text="—"/>
    </VBox></content>
  </M3Card>
</FlowPane>
```

**Verify.** The figures match what the services return for the same day; logout clears `SessionContext` and returns to `login.fxml`; cards are readable in both themes.
**Mistakes.** Loading whole tables into the view to count rows (ask the service for the count); rendering revenue as a `double`; adding delete actions here because the dashboard "shows everything anyway" (rule 4 keeps hard deletion admin-only, behind `GuardService`).

#### `attendance.fxml` — check-in/out and the daily report (owner: Abdel Raouf)

**Status:** planned · **Controller:** `com.gym.controller.AttendanceController` · **Canonical:** `docs/UML.md` §5, rules 5 (one record per customer per day) and 19 (never deleted).
**Shows.** A date and customer search, check-in and check-out actions, the day's attendance table (customer, check-in, check-out) and a correct-record action.
**Root and why.** `BorderPane`: date and search in `top`, the day's table in `center`, the action buttons in `bottom`.
**fx:id.** `datePicker`, `customerField`, `attendanceTable`, `customerColumn`, `checkInColumn`, `checkOutColumn`, `checkInButton`, `checkOutButton`.
**Handlers.** **None.** Every §5 method takes a parameter (`onCheckIn(int)`, `onCheckOut(int)`, `showDailyReport(String)`, `onCorrectRecord(int)`), so this file has `fx:id`s and no `onAction` at all: the controller attaches the button actions and the date listener in `initialize()`. This screen is the clearest example of why rule 8 exists.
**How to write it.** 1. Put `fx:id` on both buttons even though XML does not wire them — the controller needs the references. 2. Wire the date picker's value change to `showDailyReport(date)` in `initialize()`, so the user never presses "Reload". 3. Show "already checked in today" as information, not an error: rule 5 returns the existing row instead of inserting a second one.

```xml
<HBox spacing="8">
  <M3DatePicker fx:id="datePicker"/>
  <M3Button fx:id="checkInButton"  text="%attendance.checkIn"/>
  <M3Button fx:id="checkOutButton" text="%attendance.checkOut" variant="TONAL"/>
</HBox>
```

**Verify.** Checking the same customer in twice in one day leaves one row; a customer without an `ACTIVE` membership and a valid end date is refused with a readable message; a mis-keyed time is fixed through `AttendanceService.correctRecord`.
**Mistakes.** Deleting the wrong row (rule 19 forbids it — correct it); re-implementing the eligibility test in the view; formatting the check-in time with a locale formatter instead of the `HH:mm` string the model holds (rule 9).

#### `payment.fxml` — recording, voiding and receipts (owner: Abdel Raouf)

**Status:** planned · **Controller:** `com.gym.controller.PaymentController` · **Canonical:** `docs/UML.md` §5, rules 12 (XOR link) and 19 (never delete); `DECISIONS.md` 31 (`BigDecimal`).
**Shows.** A recording form (customer, the one thing being paid for, amount, method, date, receipt number), the payment history and the void / show-receipt actions.
**Root and why.** `BorderPane`: the form in `top` as a `GridPane` (label and editor columns — exactly the two-column shape a payment form is), `paymentsTable` in `center`.
**fx:id.** `customerBox`, `linkSelect` (membership / subscription / session), `membershipBox`, `subscriptionBox`, `sessionBox`, `amountField`, `methodSelect`, `datePicker`, `receiptField`, `paymentsTable`, `amountColumn`, `receiptColumn`, `voidedColumn`.
**Handlers.** `onAction="#onRecord"`; `onVoid(int paymentId)` and `showReceipt(String receiptNo)` take parameters.
**How to write it.** 1. Enable exactly one link widget at a time, mirroring rule 12 in the view — and still let `PaymentService.record` refuse a bad link: the view makes the rule hard to break, the service makes it impossible. 2. Read the amount as text and build it with `new BigDecimal(text)` (the `String` constructor) — never `new BigDecimal(45.00)`, which carries binary-float error, and never `Double.parseDouble` (`DECISIONS.md` 31). 3. Fill `methodSelect` from the `PaymentMethod` literals (CASH, CARD, TRANSFER). 4. Render voided payments dimmed, not hidden, so a voided receipt can still be found (rule 19).

```xml
<GridPane hgap="12" vgap="8">
  <Label text="%payment.amount" GridPane.columnIndex="0" GridPane.rowIndex="0"/>
  <M3TextField fx:id="amountField" GridPane.columnIndex="1" GridPane.rowIndex="0"/>
  <M3Button text="%payment.record" onAction="#onRecord"
            GridPane.columnIndex="1" GridPane.rowIndex="1"/>
</GridPane>
```

**Verify.** A payment with two non-null links is refused; a voided payment leaves the revenue totals and stays in the history; the receipt number can be looked up again with `findByReceipt`.
**Mistakes.** Building a `Payment` with the positional constructor and swapping `membershipId` with `subscriptionId` (use `forMembership`/`forSubscription`/`forSession`); deleting a wrong payment instead of voiding it and recording a new one; computing a balance in the view instead of calling `PaymentService.balance(int)` (rule 6).

#### `progress.fxml` — body-metric charts (owner: Abdel Raouf)

**Status:** planned · **Controller:** `com.gym.controller.ProgressController` · **Canonical:** `docs/UML.md` §5, §6 (`ChartUtils`), rule 13 (`ProgressMetric` selects the series).
**Shows.** A customer and metric selector, a chart of the selected measurement over time, the records table behind it, and save/delete actions for a record.
**Root and why.** `VBox`: selector row, then `progressChart`, then `recordsTable`. A `SplitPane` also works if the team wants the chart to grow with the window.
**fx:id.** `customerBox`, `metricSelect`, `progressChart`, `recordsTable`, `dateColumn`, `weightColumn`, `bodyFatColumn`, `targetField`, `notesArea`.
**Handlers.** `onAction="#onSaveRecord"`; `showProgress(int customerId)` and `onDeleteRecord(int recordId)` take parameters.
**How to write it.** 1. Fill `metricSelect` from the `ProgressMetric` literals (WEIGHT_KG, BODY_FAT_PCT) and let `ChartUtils` format the series the service returned — the view never computes one. 2. Treat `targetWeightKg` as optional: a record without a target leaves a gap, not a zero. 3. Keep the chart and the table on the same query result, so deleting a record updates both at once.

```xml
<HBox spacing="8"><M3Select fx:id="metricSelect" promptText="%progress.metric"/></HBox>
<LineChart fx:id="progressChart"/>
```

**Verify.** Switching the metric redraws the same records without a second save; a customer with one record draws one point, not a line to zero; deleting a record updates the chart.
**Mistakes.** Averages, unit conversion or interpolation done in the view; treating a null target as `0.0`; letting `ChartUtils` reach a DAO (`src/main/java/com/gym/util/README.md` forbids it — it formats values a service already fetched).

---

## 6. How this layer connects to the others

The call flow for one screen, in order:

1. `MainApp` (or `FxUtils` on a screen switch) resolves the file: `getResource("/fxml/<name>.fxml")`.
2. `FXMLLoader` parses the XML, resolves every `<?import ?>`, and instantiates the class named in
   `fx:controller` — public, no-arg constructor (`DECISIONS.md` 36).
3. The loader injects each node into the `@FXML` field whose name matches its `fx:id`, then calls
   `initialize()`.
4. `initialize()` fills lists and tables by calling a **service** (`docs/UML.md` §2 layer rules);
   `cellValueFactory` lambdas are set here, because they cannot be expressed in XML.
5. A user action calls an `onAction` handler — or, for an id-taking method, a listener the controller
   registered in step 4 — which calls the service.
6. The service returns entities; the controller writes them into the nodes it holds by `fx:id`.
7. A `DaoException` surfaces in the controller as a message string, shown with `FxUtils.error(...)`.
   The FXML takes no part in error handling.

| This folder | Depends on | Is used by |
|---|---|---|
| `fxml/*.fxml` (the screens) | `src/main/java/com/gym/controller/` (class names in `fx:controller`), the theme's colour names, the resource bundle | `MainApp`, `FxUtils`'s screen switching |
| `fxml/main.fxml` (showcase) | the external M3 library's controls | `MainApp` today; the git-ignored `M3Samples` test window later |
| the naming convention itself | `docs/DECISIONS.md` (once the spelling is logged) | every future screen and every code review |

**The seam.** The file name plus `fx:controller` is the whole contract between the shell and a screen, so
a screen can be renamed or replaced as long as those two stay in step. That is why the naming choice in
§4 rule 1 must be decided once and written down.

## 7. Common mistakes and review checklist

| Symptom | Cause | Fix |
|---|---|---|
| `LoadException: Class not found: …LoginControler` | Typo in `fx:controller`, or a missing `<?import ?>` for a control | Fix the spelling; add the import the message asks for |
| Window opens, one table stays empty, no error | `fx:id` and the `@FXML` field name differ | Compare the two names; the loader silently skips what it cannot match |
| `LoadException: … does not have a method named onLogin()` | `onAction="#onLogin"` with no such method, or a method that takes parameters | Add the no-argument handler; keep id-taking methods out of `onAction` |
| Radio buttons in one group all stay selected | Two `ToggleGroup`s, one per button | Define one group with `<fx:define>` and reference it with `$id` |
| One light-coloured panel in dark mode | An inline `style=` colour or a hardcoded hex | Move it to `styleClass` and a `-md-sys-color-*` lookup |
| Every `%key` shows literally as `%key` | No resource bundle passed to the loader | `FXMLLoader.load(url, ResourceBundle.getBundle(...))` |

**Review checklist — before approving a PR that adds or edits an FXML file:**

- [ ] File name follows the agreed controller ↔ FXML convention, and the decision is logged if it was new.
- [ ] `fx:controller` names a real controller with a public no-arg constructor.
- [ ] Every class used has an `<?import ?>`; no unused import left.
- [ ] `xmlns` and `xmlns:fx` on the root; prolog present; file is UTF-8.
- [ ] Every `fx:id` matches an `@FXML` field exactly; no field the controller reads is missing one.
- [ ] Every `onAction="#x"` names a no-argument (or single `ActionEvent`) method.
- [ ] No business rule, no SQL and no service call inside the file.
- [ ] Text uses `%key`; no colour literal; no inline `style=`.
- [ ] No password or `passwordHash` is rendered anywhere; a password node is a masked `PasswordField`
      (rule 14), and its controller clears the field as soon as the hash or verify call returns.
- [ ] Shared objects use `<fx:define>` + `$id`.
- [ ] The file was loaded once and every injected field was non-`null`.

## 8. Learning exercises (with answers)

1. **Read and answer.** Why does a typo in `fx:id` produce a `NullPointerException` in the controller
   rather than a compile error?
2. **Write this small piece.** Add an `<fx:define>`d `ToggleGroup` and attach two buttons to it, using
   `fxml/main.fxml` lines 100–106 as the model.
3. **Find the bug.** A screen has `onAction="#onDelete"` and the controller declares
   `public void onDelete(int equipmentId)`. What happens at load time, and what are the two fixes?
4. **Extend without breaking a rule.** You want the customer table to sort when the user clicks a column
   header. Where does that code go, and why can it not go in the XML?
5. **Decide and justify.** A teammate proposes a second FXML layout for `customer-profile.fxml` (tablet).
   What does that break, and what would you do instead?

<details>
<summary>Answers</summary>

1. Injection is by name through reflection. A name the loader cannot match is simply not injected and no
   exception is raised, so the field keeps its default `null`; the failure appears later, on first use,
   with a stack trace that names the controller line rather than the XML.
2.

```xml
<fx:define>
  <ToggleGroup fx:id="planGroup"/>
</fx:define>
<HBox spacing="16">
  <M3RadioButton text="%plan.monthly" toggleGroup="$planGroup" selected="true"/>
  <M3RadioButton text="%plan.yearly"  toggleGroup="$planGroup"/>
</HBox>
```

3. The loader looks for a method it can call for an `ActionEvent`, finds only a one-`int` method, and
   throws a `LoadException` naming the signature it wanted. Fix either by adding a no-argument handler
   (`onDeleteSelected()`) that reads the selected row and calls `onDelete(id)`, or by attaching the
   handler in `initialize()` from the button's `fx:id` when `onDelete(id)` is the real entry point.
4. In `initialize()`, on the column or the table (`sortOrder`, `setComparator`). A `Comparator` is a
   lambda, so it cannot be written in FXML — the same reason `cellValueFactory` lives in the controller.
   Rule 11 keeps decisions out of the view, and rule 12 keeps node building on the FX thread.
5. It breaks rules 1 and 2 together: the loader creates a *new* controller per load, so two layouts mean
   two controllers, each holding only the fields of its own file, and every `fx:id` must be kept in step
   by hand. Keep one FXML and change the layout at runtime (swap `GridPane` column constraints, move
   nodes between containers in the controller), or let the shell show the same file in a wider container.

</details>

## 9. Ownership and tasks

| Task | Owner | Status |
|------|-------|--------|
| FXML per owned screen (Weeks 2–5) | Each owner | To Do |
| Replace the `main.fxml` showcase with the real nav shell | Abdelrhman | To Do |

The owner of a screen's FXML is the owner of its controller — same screen, same person. See the screen
inventory in `src/main/java/com/gym/controller/README.md` and the per-area split in `docs/UML_TEAM.md`.

Rules: no hardcoded colors in FXML — reference `-md-sys-color-*` theme lookups; one file per controller,
matching names; `main.fxml` is testing-only until the nav shell replaces it.

**`swing` branch.** FXML does not exist there: Swing has no `FXMLLoader`, so each screen is rebuilt with
Swing containers. Keep the same file-per-screen granularity and the same controller ↔ view naming so the
two branches stay comparable, and port per vertical slice (Week 5) rather than all sixteen at the end.
That branch has **no `.java` files yet**.

**Definition of done for a screen FXML file:**

- [ ] It loads once without an exception, and every `@FXML` field is non-`null`.
- [ ] `fx:controller` and the file name follow the agreed convention.
- [ ] Every visible string is a `%key`, or is deliberately listed as not-yet-externalized.
- [ ] No colour literal, no inline `style=`, no business rule, no SQL.
- [ ] The matching controller README entry and this file's per-screen entry agree on the `fx:id` names.
- [ ] It renders correctly in both light and dark themes.
