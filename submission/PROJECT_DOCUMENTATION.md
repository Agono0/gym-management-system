# Gym Management System - Project Report

Prepared by: Abdelrhman, Ziad, Yousef, Abdel Raouf

## The idea

A gym is full of small information. Who is a member, which plan they bought, when it ends,
who trains them, did they pay, did they come today. In a lot of gyms this info lives in a
notebook at the front desk or in some Excel file that three people edit at the same time.
It works, more or less, until it doesn't. Someone forgets to write a payment. A member whose
plan expired last week checks in and nobody notices. Somebody deletes a customer and his
payments become numbers with no name attached.

So we wanted to build the thing that gyms actually need: one place for all this data, with
the gym's rules built into it. Not just storage. Storage is the easy part. The important
part is that the system itself refuses the wrong things. It doesn't let a suspended member
check in. It doesn't let a deactivated account log in. It doesn't delete a customer who
still has payments connected to him. The rule stops being "the receptionist should remember
this" and becomes part of the code.

That was the whole spirit of the project. The rest of this report explains what we built,
how we designed it, what went wrong while building it, and what is still missing.

## What the system can do

The system covers the daily work of a gym from the staff side. Here is what it handles.

**Members.** Every member has a record: name, contact info, address, birth date, gender,
and an emergency contact. Phone numbers have to be real Egyptian mobile numbers, 11 digits
starting with 010, 011, 012 or 015. We made this a shared rule because every phone field in
the system follows it. Each member also has a status (active, suspended, inactive) and can
be assigned to one trainer.

**Medical profile.** A member can have one medical record: blood type, chronic conditions,
allergies, medications, injuries, doctor name and phone. It gets created only when someone
actually needs it, not automatically for every member. The member's assigned trainer can
read it (that's a safety thing, you want to know your client has a heart condition before
pushing him), other staff cannot.

**Trainers.** Coaches are stored with their specialization, experience, certifications, hire
date and hourly rate. The member's assigned trainer is what builds the trainer's roster,
so moving a member to another trainer moves him from one roster to the other.

**Memberships.** Monthly, quarterly or yearly plans, with start date, end date and status.
Staff can sell, renew, cancel, freeze and unfreeze. Freezing is the feature we get asked
about most, so here is how it works: a member travels for a month or gets injured, the plan
is paused for a number of days, and when it comes back the end date moves forward by exactly
those days. The member doesn't lose a single day he paid for. No refund, no penalty, just
the date shifting.

**Sessions and booking.** Trainers publish availability slots (hour ranges with a maximum
number of customers per slot). A member can book a published slot directly, or request a
session at another time and wait for the trainer to confirm, or in special cases an admin
reserves it by hand. After that the session lives its life: requested, confirmed, then
completed, no-show or cancelled.

**Trainer subscriptions.** Separately from the gym membership, a member can pay for a
period of training with a specific coach. A session covered by a subscription costs zero,
so the member never pays twice for the same hour.

**Workout programs.** The trainer writes a routine for the member. When the routine changes
we don't overwrite it, we store a new version. The old versions stay, because training
history is useful.

**Attendance.** Check-in and check-out. A member can only check in if his account is active
and he holds a membership that hasn't expired. One row per member per day, a second check-in
the same day just updates the row instead of duplicating it.

**Payments.** Every payment has a unique receipt number and is linked to exactly one thing:
a membership, a subscription, or a session. When a payment is wrong we don't delete it.
We void it with a written reason and record a replacement, so the money history stays
complete. The system can also calculate the outstanding balance: memberships and confirmed
sessions that were given but not paid yet.

**Equipment.** The machines and gear with their condition (new, good, fair, poor), their
status (available, in maintenance, retired) and last maintenance date.

**Progress tracking.** Weight, body fat and measurements recorded over time, so the member
sees a trend and not just single numbers.

**Staff accounts.** Three roles: ADMIN, MANAGER, STAFF. Only ADMIN creates accounts or
changes roles, and an account that was deactivated cannot log in at all. The system can
remember the last username between runs so nobody types it every morning. It stores the
username only, never the password.

## How we built it

We split the code into layers, which is probably the most standard advice in software
engineering, but in our case it really paid off.

The model layer is plain Java classes for the things in the gym: Customer, Trainer,
Membership, Session, Payment, Attendance, Equipment, MedicalProfile, ProgressRecord,
Administrator, TrainerSubscription, TrainerAvailability, WorkoutProgram. 13 entities and
11 enums for the statuses. Nothing clever in them, just fields and getters and setters.
Two decisions here we argued about a bit. Money is `BigDecimal` and never `double`,
because money needs exact rounding and doubles don't give you that. Dates are strings in
`yyyy-MM-dd` format, which sounds lazy but is actually practical: this format sorts
correctly even as plain text, and SQLite stores it fine.

The DAO layer (data access objects) is one class per entity, and all the SQL lives here
and nowhere else. Every insert and update passes through a validate method first. The
application layer never writes SQL, it only calls the DAOs. When we reviewed each other's
code, this made things much simpler: if you want to know if a rule is enforced, you look
in one place.

Then there is a small utilities layer (the database connection and the phone validator)
and an exceptions layer with two checked exceptions: `ValidationException` for bad input
or a broken rule, and `DatabaseException` for storage problems. Wrong data gets rejected
before it touches the database, and a storage problem never crashes the whole program,
it just shows a message.

## The database

SQLite, because it needs no server and the whole database is one file that gets created
automatically. 13 tables, one per entity:

| Table | What it stores |
|---|---|
| customers | Members: contact info, status, assigned trainer |
| trainers | Coaches: specialization, experience, rate |
| memberships | Plans: type, price, dates, status, frozen days |
| sessions | Training sessions: date, time, status, price, cancel reason |
| trainer_availability | Published time slots with a customer cap |
| trainer_subscriptions | Paid training periods with a coach |
| workout_programs | Workout routines, versioned per member |
| attendance | One check-in/check-out row per member per day |
| payments | Amount, method, receipt number, void info |
| progress_records | Weight, body fat and measurements over time |
| medical_profiles | Health record, one per member |
| administrators | Staff accounts, roles, active flag |
| equipment | Machines and gear: condition, status, maintenance date |

Some columns are protected by the database itself, not just by our code. The username and
the receipt number are UNIQUE at the database level, and the medical profile table allows
only one row per member. So even if two operations run at the same time and both try to
use the same username, the second one fails with a clean message and not with a raw SQL error.

## The rules we enforce

Before writing the code we sat and wrote the business rules as a list. It ended up being
19 rules. Here are the important ones in normal words.

About memberships: only an active plan can be frozen, and freezing stores the number of
days. Unfreezing shifts the end date forward by those days and clears the counter. If a
member gets suspended, all his active plans freeze automatically. Renewing extends the end
date, and renewing a cancelled plan is refused.

About sessions: a session can only move forward in its life. A request becomes confirmed,
a confirmed one becomes completed, no-show or cancelled. Nothing jumps backwards. Cancelling
2 hours or more before the start is free, later cancellations are still recorded with the
reason. A no-show is a real state and it counts in the trainer's statistics. Slot occupancy
only counts confirmed sessions, so when someone cancels, the seat frees up for the next person.

About payments: a payment belongs to exactly one of membership, subscription or session,
and it must belong to that item's customer. Corrections go through void and replace, never
through delete. The outstanding balance counts unpaid memberships, subscriptions and
confirmed sessions only. We actually had a bug here at some point where requested sessions
were counted too, which means the balance could show money owed for sessions that were
never confirmed. Fixed.

About data integrity: deleting a customer or trainer is refused while other records still
point to them. The normal way to remove someone is to deactivate the account. And only
ADMIN manages staff accounts.

## Validation

Every write passes validation first. Names are required. Phones must be the Egyptian
11-digit format. Emails need a basic correct shape. Dates must be real yyyy-MM-dd dates,
and birth dates can't be in the future. Experience years, hourly rates and slot capacities
can't be negative.

One funny thing we discovered: Java accepts the words "NaN" and "Infinity" as numbers.
So someone could type Infinity as his body fat percentage and Java would happily store it.
We block those two words explicitly now.

When validation fails, nothing is written and the message says what was wrong. This was a
deliberate choice: fail early, fail with a clear message, write nothing.

## Bugs we faced

Some of these bugs taught us more than the features did, so they deserve their own section.

Old dates stored in a wrong format used to crash the program the next time they were read.
Now a bad stored value shows a clean message instead.

Pressing Enter on empty input in some number fields made the program ask again and again,
forever. An infinite loop from the simplest input you can imagine. Fixed.

The void and replace flow used to void the old payment first and only then validate the
replacement. If the replacement failed, you ended up with a voided payment and nothing in
its place. Now the replacement is validated first, and only then the old one is voided.
Order matters more than we thought.

SQLite sometimes refused writes with "database is locked" when another process held the
file. We set the connection to wait up to 5 seconds instead of failing instantly. The error
almost disappeared.

An admin could change his own role to a lower one and leave the gym with no admin at all.
The system refuses that now.

And a general one: some delete operations used to say "deleted" even when nothing was
actually deleted. Every update and delete now checks that a row really changed.

## How we tested

We built a fixed sample dataset that covers every state the gym can be in: active, suspended
and inactive members; active, expired, frozen and cancelled plans; confirmed, requested,
completed and no-show sessions; a voided payment with its replacement; equipment in
maintenance. Because the dataset is always the same, testing a rule gives the same result
every time.

During development we also wrote an automated pass of 22 test cases, one group for each
family of rules: field validation, login and roles, slot capacity, subscriptions, the session
life cycle, check-in, freezing, medical profiles, program versions, payments, delete guards.
All 22 passed against a separate test database, not against the real one.

Some things we check by hand: check-in refused for an expired plan, unfreezing a plan frozen
for 10 days moves its end date exactly 10 days forward, cancelling less than 2 hours before
the start still gets recorded with a reason, "123" as a phone number gets rejected with a
clear message, a deactivated staff account can't log in.

## What is still missing

We prefer to say this openly.

There are no multi-table transactions. Most writes touch one table so it's fine, but a flow
like void-and-replace touches two, and a crash in the middle could leave it half done.
Passwords are stored as plain text because this is a demo. A real gym must hash them, we
know that. Two overlapping availability slots can both be booked, the cap is per slot and
there is no check across slots. A member can hold more than one active membership at the
same time and the system just uses the latest one. And if a status value in the database
gets corrupted somehow, the system falls back to a default instead of complaining about it.

## Who did what

We split the features vertically, meaning each one of us owned his features from the data
model down to the finished behavior. Nobody was "the SQL guy" or "the UI guy".

| Member | Owned |
|---|---|
| Abdelrhman | Project foundation, database schema, customers and profiles, medical profiles, staff accounts and login |
| Ziad | Trainers and trainer assignment, memberships and plans |
| Yousef | Workout programs, sessions and scheduling, equipment, trainer subscriptions and availability |
| Abdel Raouf | Attendance, payments and the outstanding balance, progress tracking |

## Final words

The system does what we wanted it to do. The data is in one database, the rules are in the
code, mistakes come back as clear messages instead of crashes or silent data loss.

The biggest lesson for me was how much easier everything gets when validation lives in one
layer. Late in the project we had to change one of the shared rules, and the whole change
was in a single file, one small class. If the checks had been scattered around the code,
the same change would have taken a day, and I'm sure we would have missed a field
somewhere.
