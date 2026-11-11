import re
from pathlib import Path

SRC = Path(r"C:\Users\pc\Desktop\JAVA-Project\submission\src")

# (old, new) exact fragment replacements — applied to every .java file, all occurrences
MAP = [
    # ---------- GymCliApp ----------
    ('  Seeded Accounts (admin / manager active, oldstaff inactive per Rule 16):',
     '  Seeded Accounts (admin / manager active, oldstaff inactive):'),
    ('  [V] Validation & Business Rules Suite (1-19)',
     '  [V] Validation & Exception Test Suite'),
    # ---------- MembershipBillingCliMenu ----------
    ('  [2] Freeze ACTIVE Membership (Rule 7)', '  [2] Freeze ACTIVE Membership'),
    ('  [3] Unfreeze FROZEN Membership -> Shifts endDate (Rule 7)', '  [3] Unfreeze FROZEN Membership -> Shifts endDate'),
    ('  [4] Renew Membership -> Refuses CANCELLED (Rule 7)', '  [4] Renew Membership -> Refuses CANCELLED'),
    ('  [1] Record Payment (enforces Rule 12 XOR link + Customer Match + Unique Receipt)',
     '  [1] Record Payment (one target link + customer match + unique receipt)'),
    ('  [2] Record Payment with Manual Links (Rule 12 validates every insert)',
     '  [2] Record Payment with Manual Links (every insert is validated)'),
    ('  [3] Customer Outstanding Balance Breakdown (Rule 6)', '  [3] Customer Outstanding Balance Breakdown'),
    ('  [5] Void & Issue Replacement Payment (Rule 19)', '  [5] Void & Issue Replacement Payment'),
    ('"Rule 6 Outstanding Balance for Customer #"', '"Outstanding Balance for Customer #"'),
    ('  Link Target (Rule 12 XOR): [1] Membership', '  Link Target (exactly one): [1] Membership'),
    ('Record Payment (Manual Links - Rule 12 validates the insert)', 'Record Payment (Manual Links - validated insert)'),
    ('            // Receipt is required when a replacement was promised, so "Rule 19 completed"\n'
     '            // below always means a replacement payment was really created',
     '            // Receipt is required when a replacement was promised, so the success\n'
     '            // message below always means a replacement payment was really created'),
    ('"Rule 19 completed. Updated/Created Payment #"', '"Void completed. Updated/Created Payment #"'),
    # ---------- GymRulesHelper (becomes GymOperations) ----------
    ('// Coordinates cross-entity business rules 1-19 using the 13 concrete DAOs',
     '// Coordinates cross-entity operations using the 13 concrete DAOs'),
    ('// Rule 16: Authenticate user and reject inactive accounts', '// Authenticate user and reject inactive accounts'),
    ("\"Rule 16: Account '\"", "\"Account '\""),
    ('// Rule 16: Enforce ADMIN role for restricted actions', '// Enforce ADMIN role for restricted actions'),
    ('"Rule 16: StaffRole.ADMIN required (current role: "', '"ADMIN role required for this action (current role: "'),
    ('// Rule 4: Soft delete customer sets INACTIVE', '// Soft delete customer sets INACTIVE (references are never touched)'),
    ('// Rule 4: Admin-only hard delete refuses while child references exist', '// Admin-only hard delete refuses while child references exist'),
    ('"Rule 4: Hard delete refused - customer #"', '"Hard delete refused - customer #"'),
    ('// Rule 4: Admin-only trainer hard delete refuses while references exist', '// Admin-only trainer hard delete refuses while references exist'),
    ('"Rule 4: Hard delete refused - trainer #"', '"Hard delete refused - trainer #"'),
    ('// Rule 14: Suspending a customer freezes all active memberships', '// Suspending a customer freezes all active memberships'),
    ('// Rule 7: Only ACTIVE membership can be frozen', '// Only ACTIVE membership can be frozen'),
    ('"Rule 7: Only ACTIVE memberships can be frozen (current: "', '"Only ACTIVE memberships can be frozen (current: "'),
    ('// Rule 7: Unfreeze shifts endDate forward by frozenDays and clears frozenDays', '// Unfreeze shifts endDate forward by frozenDays and clears frozenDays'),
    ('"Rule 7: Only FROZEN memberships can be unfrozen (current: "', '"Only FROZEN memberships can be unfrozen (current: "'),
    ('"Rule 7: Membership #"', '"Membership #"'),
    ('// Rule 7: Renew extends endDate for ACTIVE or EXPIRED plans; refuses CANCELLED', '// Renew extends endDate for ACTIVE or EXPIRED plans; refuses CANCELLED'),
    ('"Rule 7: Renewing a CANCELLED membership is refused"', '"Renewing a CANCELLED membership is refused"'),
    ('// Rule 13: Derived membership status (EXPIRED if endDate < today while stored ACTIVE)', '// Derived membership status (EXPIRED if endDate < today while stored ACTIVE)'),
    ('// Rule 17: Occupancy counts CONFIRMED sessions only', '// Occupancy counts CONFIRMED sessions only'),
    ('// Rule 13 & 17: Derived FULL status when CONFIRMED occupancy reaches maxCustomers', '// Derived FULL status when CONFIRMED occupancy reaches maxCustomers'),
    ('// Rule 1, 11, 13, 17: Book a published OPEN availability slot directly as CONFIRMED', '// Book a published OPEN availability slot directly as CONFIRMED'),
    ('"Rule 5: Booking denied because member status is "', '"Booking denied because member status is "'),
    ('"Rule 1: Cannot book a CLOSED slot"', '"Cannot book a CLOSED slot"'),
    ('"Rule 1: Cannot book a slot in the past ("', '"Cannot book a slot in the past ("'),
    ('"Rule 17: Slot #"', '"Slot #"'),
    ('// Rule 1: Request session outside published slots -> REQUESTED (default 60m)', '// Request session outside published slots -> REQUESTED (default 60m)'),
    ('// Rule 1 & 16: Admin override booking ignores slot cap/status -> CONFIRMED', '// Admin override booking ignores slot cap/status -> CONFIRMED (ADMIN only)'),
    ('// Rule 18 & 17: Confirm a REQUESTED session (checking slot cap if attached to an availability slot)',
     '// Confirm a REQUESTED session (checking slot cap if attached to an availability slot)'),
    ('"Rule 18: Only REQUESTED sessions can be confirmed (current: "', '"Only REQUESTED sessions can be confirmed (current: "'),
    ('"Rule 17: Cannot confirm session - linked slot is FULL"', '"Cannot confirm session - linked slot is FULL"'),
    ('// Rule 18: Complete a CONFIRMED session', '// Complete a CONFIRMED session'),
    ('"Rule 18: Only CONFIRMED sessions can be completed (current: "', '"Only CONFIRMED sessions can be completed (current: "'),
    ('// Rule 2 & 18: Mark CONFIRMED session as NO_SHOW', '// Mark CONFIRMED session as NO_SHOW (counted in trainer stats)'),
    ('"Rule 18: Only CONFIRMED sessions can be marked NO_SHOW (current: "', '"Only CONFIRMED sessions can be marked NO_SHOW (current: "'),
    ('// Rule 10 & 18: Cancel CONFIRMED session, computing 2h notice status', '// Cancel CONFIRMED session, computing the 2h notice status'),
    ('"Cancel reason is required (Rule 10)"', '"Cancel reason is required"'),
    ('"Rule 10: Free cancellation (>= 2h notice, "', '"Free cancellation (>= 2h notice, "'),
    ('"Rule 10: Late cancellation (< 2h before start). Marked CANCELLED with reason recorded."',
     '"Late cancellation (< 2h before start). Marked CANCELLED with reason recorded."'),
    ('// Rule 2: Count trainer NO_SHOW sessions for trainer statistics', '// Count trainer NO_SHOW sessions for trainer statistics'),
    ('// Rule 5: Check-in validates ACTIVE status + unexpired plan and reuses today\'s existing row',
     '// Check-in validates ACTIVE status + unexpired plan and reuses today\'s existing row'),
    ('"Rule 5: Check-in denied because member status is "', '"Check-in denied because member status is "'),
    ('"Rule 5: Check-in denied - no ACTIVE unexpired membership on "', '"Check-in denied - no ACTIVE unexpired membership on "'),
    ('// Rule 15 & 3: Medical profile read check for assigned trainer', '// Medical profile read check, allowed for the assigned trainer only'),
    ('"Rule 15: Access denied - trainer #"', '"Access denied - trainer #"'),
    ('// Rule 6: Outstanding unpaid balance across memberships, subscriptions, and non-zero ad-hoc sessions',
     '// Outstanding unpaid balance across memberships, subscriptions, and non-zero ad-hoc sessions'),
    ('// Rule 19: Void an existing payment and optionally create a replacement payment', '// Void an existing payment and optionally create a replacement payment'),
    ('"Rule 19: Payment #"', '"Payment #"'),
    ('"Rule 19: Void reason is required"', '"Void reason is required"'),
    ('"Rule 19: Replacement amount must be greater than zero"', '"Replacement amount must be greater than zero"'),
    ('"Rule 19: Replacement payment method is required"', '"Replacement payment method is required"'),
    ('"Rule 19: Replacement receipt number already exists: "', '"Replacement receipt number already exists: "'),
    # ---------- TrainerCliMenu ----------
    ('  [1] List All Coaches (with Rate & Rule 2 NO_SHOW Count)', '  [1] List All Coaches (with Rate & NO_SHOW Count)'),
    ('  [4] Trainer Availability Slots (Rules 8, 13 & 17)', '  [4] Trainer Availability Slots'),
    ('  [5] Trainer Subscriptions (Rule 11 & 13)', '  [5] Trainer Subscriptions'),
    (' (Rule 2) | Assigned Roster: ', ' | Assigned Roster: '),
    ('"Assigned Members (Rule 15)"', '"Assigned Members"'),
    ('  [2] Read Member Medical Profile (Rule 15 assigned trainer check)', '  [2] Read Member Medical Profile (assigned trainer check)'),
    ('  [3] Hard-Delete Coach [ADMIN Only + Ref Guard] (Rule 4 & 16)', '  [3] Hard-Delete Coach [ADMIN Only + Ref Guard]'),
    ('"Rule 15 GRANTED: "', '"Access GRANTED: "'),
    ('  [2] Toggle OPEN / CLOSED Status (Rule 13)', '  [2] Toggle OPEN / CLOSED Status'),
    ('  [1] Create Trainer Subscription (enforces Rule 11 assigned trainer match)', "  [1] Create Trainer Subscription (must match the member's assigned trainer)"),
    ("Trainer ID (must match member's assigned trainerId per Rule 11): ", "Trainer ID (must match member's assigned trainerId): "),
    # ---------- MemberCliMenu ----------
    (' outstanding (Rule 6)', ' outstanding'),
    ('Not created yet (Rule 3)', 'Not created yet'),
    ('  [1] Edit Member Details / Reassign Trainer (Rule 15)', '  [1] Edit Member Details / Reassign Trainer'),
    ('  [2] Soft-Delete Member -> Set INACTIVE (Rule 4)', '  [2] Soft-Delete Member -> Set INACTIVE'),
    ('  [3] Hard-Delete Member [ADMIN Only + Ref Guard] (Rule 4 & 16)', '  [3] Hard-Delete Member [ADMIN Only + Ref Guard]'),
    ('  [4] Suspend Member (Cascades Freeze to Active Plans - Rule 14) / Reactivate', '  [4] Suspend Member (Cascades Freeze to Active Plans) / Reactivate'),
    ('  [5] Medical Profile Manager (Rule 3 & 15)', '  [5] Medical Profile Manager'),
    ('"Rule 14: Member #"', '"Member #"'),
    ('  No medical profile on file yet (Rule 3: created on demand).', '  No medical profile on file yet (created on demand).'),
    ('  [1] Create On-Demand Medical Profile (Rule 3)', '  [1] Create On-Demand Medical Profile'),
    ('  [3] Trainer Medical Read Check (Rule 15)', '  [3] Trainer Medical Read Check'),
    ('"Rule 15 GRANTED for Trainer #"', '"Access GRANTED for Trainer #"'),
    # ---------- ScheduleWorkoutCliMenu ----------
    ('  [1] Book Published OPEN Slot -> CONFIRMED (Rules 1, 11, 13, 17)', '  [1] Book Published OPEN Slot -> CONFIRMED'),
    ('  [2] Request Session Outside Slots -> REQUESTED (Rule 1)', '  [2] Request Session Outside Slots -> REQUESTED'),
    ('  [3] Admin Override Reserve -> CONFIRMED [ADMIN Only] (Rule 1 & 16)', '  [3] Admin Override Reserve -> CONFIRMED [ADMIN Only]'),
    ('  [4] Confirm REQUESTED Session (Rule 18)', '  [4] Confirm REQUESTED Session'),
    ('  [5] Complete CONFIRMED Session (Rule 18)', '  [5] Complete CONFIRMED Session'),
    ('  [6] Mark CONFIRMED Session as NO_SHOW (Rule 2 & 18)', '  [6] Mark CONFIRMED Session as NO_SHOW'),
    ('  [7] Cancel CONFIRMED Session (Rule 10 2h Check & Rule 18)', '  [7] Cancel CONFIRMED Session (2h Notice Check)'),
    ('Session Price (Rule 11 requires 0 for subscription)', 'Session Price (0 if covered by subscription)'),
    # ---------- FacilityAdminCliMenu ----------
    ("  [1] Fast Check-In (Rule 5: ACTIVE member + unexpired plan; reuses today's row)",
     "  [1] Fast Check-In (ACTIVE member + unexpired plan; reuses today's row)"),
    ('  [4] Correct Attendance Record Time (Rule 19: correct, never hard-delete)', '  [4] Correct Attendance Record Time (correct, never hard-delete)'),
    ('STAFF & ADMIN CONTROLS [RULE 16 RBAC]', 'STAFF & ADMIN CONTROLS (Role-Based Access)'),
    ('  [2] Activate / Deactivate Account [ADMIN Only] (Rule 16)', '  [2] Activate / Deactivate Account [ADMIN Only]'),
    # ---------- DAOs ----------
    ('"Rule 12: Payment must link to exactly one target (membership, subscription, or session)"',
     '"Payment must link to exactly one target (membership, subscription, or session)"'),
    ('"Rule 12: Linked record does not exist in "', '"Linked record does not exist in "'),
    ('"Rule 12: Payment customerId ("', '"Payment customerId ("'),
    ('"Rule 3: Customer "', '"Customer "'),
    ('"Rule 5: Member already checked in on "', '"Member already checked in on "'),
    ('            // Rule 5 also applies when correcting a row: moving a record to a date that',
     '            // The same-day check also applies when correcting a row: moving a record to a date that'),
    ('"Rule 5: Member already has another attendance row on "', '"Member already has another attendance row on "'),
    ('"Rule 5: Check-in blocked - customer status is "', '"Check-in blocked - customer status is "'),
    ('"Rule 5: Check-in blocked - no ACTIVE unexpired membership for "', '"Check-in blocked - no ACTIVE unexpired membership for "'),
    ('"Rule 7: Cancelled membership cannot be renewed or reactivated"', '"Cancelled membership cannot be renewed or reactivated"'),
    ('// Rule 4 second layer of defense: refuse deleting a trainer that other tables still reference,',
     '// Second layer of defense against orphan rows: refuse deleting a trainer that other tables still reference,'),
    ('"Rule 4: Trainer #"', '"Trainer #"'),
    ('// Rule 4 second layer of defense: refuse deleting a customer that other tables still reference,',
     '// Second layer of defense against orphan rows: refuse deleting a customer that other tables still reference,'),
    ('"Rule 4: Customer #"', '"Customer #"'),
    ('"Rule 8: Slot start time must precede end time"', '"Slot start time must precede end time"'),
    ('"Rule 8: Slot maxCustomers must be greater than zero"', '"Slot maxCustomers must be greater than zero"'),
    ('"Rule 11: Subscription trainerId must match customer\'s assigned trainerId"', '"Subscription trainerId must match customer\'s assigned trainerId"'),
    ('"Session date must be in yyyy-MM-dd format (Rule 9)"', '"Session date must be in yyyy-MM-dd format"'),
    ('"Session start time must be in HH:mm format (Rule 9)"', '"Session start time must be in HH:mm format"'),
    ('"Rule 11: Session covered by a subscription must have price == 0"', '"Session covered by a subscription must have price == 0"'),
    ('"Rule 18: Only REQUESTED sessions may become CONFIRMED"', '"Only REQUESTED sessions may become CONFIRMED"'),
    ('"Rule 18: Only CONFIRMED sessions may become "', '"Only CONFIRMED sessions may become "'),
    ('"Rule 18: Cannot revert a session back to REQUESTED"', '"Cannot revert a session back to REQUESTED"'),
    ('"Rule 11: Session customer/trainer must match the linked subscription"', '"Session customer/trainer must match the linked subscription"'),
    # ---------- SampleDataSeeder ----------
    ('// 1. Staff and administrators (includes active + inactive accounts for Rule 16)', '// 1. Staff and administrators (includes active + inactive accounts)'),
    ('// 5. Medical Profile (on-demand for c1; others have none yet per Rule 3)', '// 5. Medical Profile (on-demand for c1; others have none yet)'),
    ('// 9. Trainer Subscriptions (matches c1.trainerId == t1.getId() per Rule 11)', '// 9. Trainer Subscriptions (matches c1.trainerId == t1.getId())'),
    ('// 13. Payments (includes a voided + replacement pair per Rule 19, and leaves c2 ad-hoc sessions unpaid for Rule 6)',
     '// 13. Payments (includes a voided + replacement pair, and leaves c2 ad-hoc sessions unpaid)'),
    # ---------- ValidationSuiteRunner ----------
    ('// Automated and interactive suite verifying all entities, business rules 1-19, and error handling.',
     '// Automated and interactive suite verifying all entities, validations, and error handling.'),
    ('"VALIDATION, BUSINESS RULES & EXCEPTION SUITE"', '"VALIDATION & EXCEPTION SUITE"'),
    ('  [2]  Login Inactive Block & ADMIN RBAC (Rule 16)', '  [2]  Login Inactive Block & ADMIN RBAC'),
    ('  [3]  Availability Cap, Open Slot Booking & Derived FULL (Rules 1, 8, 13, 17)', '  [3]  Availability Cap, Open Slot Booking & Derived FULL'),
    ('  [4]  Subscription Trainer Match & Zero-Price Session (Rule 11)', '  [4]  Subscription Trainer Match & Zero-Price Session'),
    ('  [5]  Session State Machine, Cancel 2h Check & NO_SHOW (Rules 2, 10, 18)', '  [5]  Session State Machine, Cancel 2h Check & NO_SHOW'),
    ('  [6]  Check-In Active Plan Guard & Same-Day Row Reuse (Rule 5)', '  [6]  Check-In Active Plan Guard & Same-Day Row Reuse'),
    ('  [7]  Freeze / Unfreeze Date Shift, Cancelled Renew Block & Suspend Cascade (Rules 7, 14)',
     '  [7]  Freeze / Unfreeze Date Shift, Cancelled Renew Block & Suspend Cascade'),
    ('  [8]  Medical Profile Single Record & Trainer Access (Rules 3, 15)', '  [8]  Medical Profile Single Record & Trainer Access'),
    ('  [10] Payment XOR, Customer Match, Void/Replace & Balance (Rules 6, 12, 19)', '  [10] Payment XOR, Customer Match, Void/Replace & Balance'),
    ('  [11] Hard-Delete Guard (Rule 4) + Serialization .ser & DatabaseException Handling', '  [11] Hard-Delete Guard + Serialization .ser & DatabaseException Handling'),
    ('"Inactive login should fail (Rule 16)"', '"Inactive login should fail"'),
    ('"Non-ADMIN adminReserve should fail (Rule 16)"', '"Non-ADMIN adminReserve should fail"'),
    ('"Start time >= End time should fail (Rule 8)"', '"Start time >= End time should fail"'),
    ('"Overflow booking into FULL slot should fail (Rule 17)"', '"Overflow booking into FULL slot should fail"'),
    ('// Customer #1 is assigned to Trainer #1; attempting Trainer #2 breaks Rule 11', '// Customer #1 is assigned to Trainer #1; attempting Trainer #2 breaks the trainer-match validation'),
    ('"Trainer mismatch in subscription should fail (Rule 11)"', '"Trainer mismatch in subscription should fail"'),
    ('"Subscription session with price > 0 should fail (Rule 11)"', '"Subscription session with price > 0 should fail"'),
    ('"REQUESTED -> COMPLETED without CONFIRMED should fail (Rule 18)"', '"REQUESTED -> COMPLETED without CONFIRMED should fail"'),
    ('"Trainer #2 NO_SHOW count should reflect marked session (Rule 2)"', '"Trainer #2 NO_SHOW count should reflect marked session"'),
    ('"Rule 18 illegal transition caught & Trainer #2 NO_SHOWs="', '"Illegal session transition caught & Trainer #2 NO_SHOWs="'),
    ('// Slot #2 is FULL; adminReserve succeeds anyway per Rule 1 & 16', '// Slot #2 is FULL; adminReserve succeeds anyway (admin override)'),
    ('" (Rule 1 & 16)");', '" (admin override)");'),
    ('"Expired membership check-in should fail (Rule 5)"', '"Expired membership check-in should fail"'),
    ('" (Rule 5)");', '" (same-day reuse)");'),
    ('"Renewing CANCELLED membership should fail (Rule 7)"', '"Renewing CANCELLED membership should fail"'),
    ('"Expected Customer #2 suspension to freeze active Membership #2 (Rule 14)"', '"Expected Customer #2 suspension to freeze active Membership #2"'),
    (' active membership (Rule 14)");', ' active membership");'),
    ('"Second medical profile for same customer should fail (Rule 3)"', '"Second medical profile for same customer should fail"'),
    ('"Unassigned trainer read medical profile should fail (Rule 15)"', '"Unassigned trainer read medical profile should fail"'),
    ('"Rule 3 single profile enforced & Rule 15 trainer access ok (Blood="', '"Single medical profile enforced & trainer access check ok (Blood="'),
    ('// 0 target links breaks Rule 12 XOR', '// 0 target links breaks the exactly-one-link payment validation'),
    ('"0-link payment should fail Rule 12 XOR"', '"0-link payment should be rejected"'),
    ('// Mismatched customerId breaks Rule 12 (Membership #2 belongs to Customer #2, not #1)', '// Mismatched customerId breaks the owner check (Membership #2 belongs to Customer #2, not #1)'),
    ('"Mismatched owner payment should fail Rule 12"', '"Mismatched owner payment should be rejected"'),
    ('// Void existing Payment #4 and check Rule 6 balance increases', '// Void existing Payment #4 and check the outstanding balance increases'),
    ('"Voided payment must not count toward balance (Rule 6 & 19)"', '"Voided payment must not count toward balance"'),
    ('"Rule 12 XOR + Rule 19 Void + Rule 6 Balance verified ($"', '"Payment link + void + balance verified ($"'),
    ('"Hard delete of Customer #1 with child records should fail (Rule 4)"', '"Hard delete of Customer #1 with child records should fail"'),
    # ---------- CliTheme ----------
    ('printRuleNotice', 'printNotice'),
]

changed_files = 0
for path in sorted(SRC.rglob("*.java")):
    text = path.read_text(encoding="utf-8")
    original = text
    for old, new in MAP:
        if old in text:
            text = text.replace(old, new)
    # class + identifier renames
    text = text.replace("GymRulesHelper", "GymOperations")
    text = re.sub(r"\brules\b", "ops", text)
    if text != original:
        path.write_text(text, encoding="utf-8")
        changed_files += 1

# rename the class file itself
old_file = SRC / "com" / "gym" / "cli" / "GymRulesHelper.java"
new_file = SRC / "com" / "gym" / "cli" / "GymOperations.java"
if old_file.exists():
    old_file.rename(new_file)
    print("renamed GymRulesHelper.java -> GymOperations.java")

print("files changed:", changed_files)

# leftover audit
leftover = []
for path in sorted(SRC.rglob("*.java")):
    for i, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if re.search(r"rule", line, re.IGNORECASE):
            leftover.append(f"{path.relative_to(SRC)}:{i}: {line.strip()}")
if leftover:
    print("LEFTOVERS (%d):" % len(leftover))
    for l in leftover:
        print("  " + l)
else:
    print("NO leftover 'rule' wording — clean.")
