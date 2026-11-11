package com.gym.cli;

import com.gym.dao.MembershipDao;
import com.gym.dao.PaymentDao;
import com.gym.exception.DatabaseException;
import com.gym.exception.ValidationException;
import com.gym.model.Membership;
import com.gym.model.MembershipStatus;
import com.gym.model.Payment;
import com.gym.model.PaymentMethod;
import com.gym.model.PlanType;
import com.gym.util.DatabaseConnection;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

// Terminal screens for Memberships and Payments / Billing Ledger
public class MembershipBillingCliMenu {

    private final CliInput input;
    private final CliSession session;
    private final GymOperations ops = new GymOperations();
    private final MembershipDao membershipDao = new MembershipDao();
    private final PaymentDao paymentDao = new PaymentDao();

    public MembershipBillingCliMenu(CliInput input, CliSession session) {
        this.input = input;
        this.session = session;
    }

    public void openMembershipsMenu() {
        while (!session.shouldStopSubMenu()) {
            try {
                List<Membership> list = membershipDao.findAll();
                CliTheme.printHeader("MEMBERSHIPS & PLAN LIFECYCLE",
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Memberships");

                String today = LocalDate.now().toString();
                String[] headers = {"ID", "Member", "Plan", "Price", "Start", "End", "Effective", "Frozen"};
                int[] widths = {4, 7, 10, 8, 10, 10, 10, 6};
                List<String[]> rows = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    Membership m = list.get(i);
                    MembershipStatus eff = ops.effectiveMembershipStatus(m, today);
                    rows.add(new String[]{
                        String.valueOf(m.getId()),
                        "#" + m.getCustomerId(),
                        m.getPlan().name(),
                        "$" + m.getPrice().toPlainString(),
                        m.getStartDate(),
                        m.getEndDate(),
                        eff.name(),
                        String.valueOf(m.getFrozenDays())
                    });
                }
                CliTheme.printTable(headers, rows, widths);

                System.out.println("  [1] Sell New Membership Plan");
                System.out.println("  [2] Freeze ACTIVE Membership");
                System.out.println("  [3] Unfreeze FROZEN Membership -> Shifts endDate");
                System.out.println("  [4] Renew Membership -> Refuses CANCELLED");
                System.out.println("  [5] Cancel Membership");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                if ("1".equals(choice)) {
                    sellMembership();
                    input.pause();
                } else if ("2".equals(choice)) {
                    Integer mid = input.readInt("Membership ID to freeze: ", 1);
                    Integer days = input.readInt("Freeze days (>0): ", 0);
                    if (mid != null && days != null) {
                        ops.freezeMembership(mid.intValue(), days.intValue());
                        CliTheme.printSuccess("Membership #" + mid + " frozen for " + days + " day(s).");
                    }
                    input.pause();
                } else if ("3".equals(choice)) {
                    Integer mid = input.readInt("Membership ID to unfreeze: ", 1);
                    if (mid != null) {
                        Membership updated = ops.unfreezeMembership(mid.intValue());
                        CliTheme.printSuccess("Unfrozen Membership #" + updated.getId()
                                + " - new endDate: " + updated.getEndDate());
                    }
                    input.pause();
                } else if ("4".equals(choice)) {
                    Integer mid = input.readInt("Membership ID to renew: ", 1);
                    if (mid != null) {
                        Membership renewed = ops.renewMembership(mid.intValue());
                        CliTheme.printSuccess("Renewed Membership #" + renewed.getId()
                                + " - new endDate: " + renewed.getEndDate());
                    }
                    input.pause();
                } else if ("5".equals(choice)) {
                    Integer mid = input.readInt("Membership ID to cancel: ", 1);
                    if (mid != null) {
                        Membership m = membershipDao.findById(mid.intValue());
                        if (m == null) {
                            CliTheme.printValidationError("Membership not found: #" + mid);
                        } else {
                            m.setStatus(MembershipStatus.CANCELLED);
                            membershipDao.update(m);
                            CliTheme.printSuccess("Cancelled Membership #" + m.getId());
                        }
                    }
                    input.pause();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    public void openPaymentsMenu() {
        while (!session.shouldStopSubMenu()) {
            try {
                List<Payment> list = paymentDao.findAll();
                CliTheme.printHeader("PAYMENTS & POS BILLING LEDGER",
                        session.userLabel(), DatabaseConnection.getDbUrl(),
                        "Main Menu > Payments");

                String[] headers = {"ID", "Cust", "Target", "Amount", "Method", "Date", "Receipt", "Status"};
                int[] widths = {4, 5, 8, 8, 8, 10, 10, 6};
                List<String[]> rows = new ArrayList<>();
                for (int i = 0; i < list.size(); i++) {
                    Payment p = list.get(i);
                    String target = "-";
                    if (p.getMembershipId() != null) {
                        target = "MEM#" + p.getMembershipId();
                    } else if (p.getSubscriptionId() != null) {
                        target = "SUB#" + p.getSubscriptionId();
                    } else if (p.getSessionId() != null) {
                        target = "SES#" + p.getSessionId();
                    }
                    rows.add(new String[]{
                        String.valueOf(p.getId()),
                        "#" + p.getCustomerId(),
                        target,
                        "$" + p.getAmount().toPlainString(),
                        p.getMethod().name(),
                        p.getDate(),
                        p.getReceiptNo(),
                        p.isVoided() ? "VOID" : "PAID"
                    });
                }
                CliTheme.printTable(headers, rows, widths);

                System.out.println("  [1] Record Payment (one target link + customer match + unique receipt)");
                System.out.println("  [2] Record Payment with Manual Links (every insert is validated)");
                System.out.println("  [3] Customer Outstanding Balance Breakdown");
                System.out.println("  [4] Lookup Receipt Number");
                System.out.println("  [5] Void & Issue Replacement Payment");
                CliTheme.printFooterNav(true);

                String choice = input.readLine("Select option > ");
                if (choice == null || "0".equals(choice)) {
                    return;
                }
                if ("H".equalsIgnoreCase(choice)) {
                    session.setReturnHome(true);
                    return;
                }
                if ("L".equalsIgnoreCase(choice)) {
                    session.setLogoutRequested(true);
                    return;
                }

                if ("1".equals(choice)) {
                    recordValidPayment();
                    input.pause();
                } else if ("2".equals(choice)) {
                    recordManualPayment();
                    input.pause();
                } else if ("3".equals(choice)) {
                    Integer cid = input.readInt("Enter Customer ID: ", 1);
                    if (cid != null) {
                        BigDecimal balance = ops.computeOutstandingBalance(cid.intValue());
                        CliTheme.printSuccess("Outstanding Balance for Customer #" + cid
                                + " = $" + balance.toPlainString());
                    }
                    input.pause();
                } else if ("4".equals(choice)) {
                    String rec = input.readRequiredString("Receipt Number (e.g. REC-1002): ");
                    if (rec != null) {
                        Payment found = paymentDao.findByReceiptNo(rec);
                        if (found == null) {
                            CliTheme.printValidationError("No payment found for receipt: " + rec);
                        } else {
                            CliTheme.printSuccess(found.toString()
                                    + (found.isVoided() ? " | Void Reason: " + found.getVoidReason() : ""));
                        }
                    }
                    input.pause();
                } else if ("5".equals(choice)) {
                    voidAndReplaceFlow();
                    input.pause();
                } else {
                    System.out.println("[!] Unknown option: " + choice);
                }
            } catch (ValidationException e) {
                CliTheme.printValidationError(e.getMessage());
                input.pause();
            } catch (DatabaseException e) {
                CliTheme.printDatabaseError(e.getMessage());
                input.pause();
            }
        }
    }

    private void sellMembership() throws DatabaseException, ValidationException {
        Integer cid = input.readInt("Customer ID: ", 1);
        if (cid == null) {
            return;
        }
        System.out.println("  Plan Types: [1] MONTHLY ($60 / 30d)  [2] QUARTERLY ($150 / 90d)  [3] YEARLY ($500 / 365d)");
        String pChoice = input.readLine("Select Plan Type [1-3]: ");
        PlanType plan = PlanType.MONTHLY;
        BigDecimal defPrice = new BigDecimal("60.00");
        if ("2".equals(pChoice)) {
            plan = PlanType.QUARTERLY;
            defPrice = new BigDecimal("150.00");
        } else if ("3".equals(pChoice)) {
            plan = PlanType.YEARLY;
            defPrice = new BigDecimal("500.00");
        }
        BigDecimal price = input.readBigDecimal("Price", defPrice);
        String start = input.readIsoDate("Start Date", LocalDate.now().toString());
        if (start == null || price == null) {
            return;
        }
        String suggestedEnd = LocalDate.parse(start).plusDays(ops.daysForPlan(plan)).toString();
        String end = input.readIsoDate("End Date", suggestedEnd);
        if (end == null) {
            return;
        }
        Membership m = membershipDao.insert(new Membership(0, cid.intValue(), plan, price,
                start, end, MembershipStatus.ACTIVE, 0));
        CliTheme.printSuccess("Sold Membership #" + m.getId() + " (" + plan + ") for Customer #" + cid);
    }

    private void recordValidPayment() throws DatabaseException, ValidationException {
        Integer cid = input.readInt("Customer ID: ", 1);
        if (cid == null) {
            return;
        }
        System.out.println("  Link Target (exactly one): [1] Membership  [2] TrainerSubscription  [3] Session");
        String targetType = input.readLine("Target Type [1-3]: ");
        Integer targetId = input.readInt("Target Record ID: ", 1);
        BigDecimal amount = input.readBigDecimal("Amount (>0)", new BigDecimal("45.00"));
        PaymentMethod method = readPaymentMethod();
        String receipt = input.readRawString("Unique Receipt No (e.g. REC-" + System.currentTimeMillis() % 10000 + "): ");
        if (targetId == null || amount == null || receipt == null) {
            return;
        }
        String today = LocalDate.now().toString();
        Payment p;
        if ("2".equals(targetType)) {
            p = Payment.forSubscription(cid.intValue(), targetId.intValue(), amount, method, today, receipt);
        } else if ("3".equals(targetType)) {
            p = Payment.forSession(cid.intValue(), targetId.intValue(), amount, method, today, receipt);
        } else {
            p = Payment.forMembership(cid.intValue(), targetId.intValue(), amount, method, today, receipt);
        }
        Payment saved = paymentDao.insert(p);
        CliTheme.printSuccess("Recorded Payment #" + saved.getId() + " Receipt=" + saved.getReceiptNo());
    }

    private void recordManualPayment() throws DatabaseException, ValidationException {
        CliTheme.printSectionTitle("Record Payment (Manual Links - validated insert)");
        Integer cid = input.readInt("Customer ID: ", 1);
        Integer memId = readNullableInt("Membership ID (Enter for null): ");
        Integer subId = readNullableInt("Subscription ID (Enter for null): ");
        Integer sesId = readNullableInt("Session ID (Enter for null): ");
        BigDecimal amount = input.readBigDecimal("Amount", new BigDecimal("50.00"));
        String receipt = input.readRawString("Receipt No: ");
        if (cid == null || amount == null) {
            return;
        }
        Payment p = new Payment(0, cid.intValue(), memId, subId, sesId,
                amount, PaymentMethod.CARD, LocalDate.now().toString(), receipt, false);
        Payment saved = paymentDao.insert(p);
        CliTheme.printSuccess("Inserted Payment #" + saved.getId());
    }

    private void voidAndReplaceFlow() throws DatabaseException, ValidationException {
        Integer pid = input.readInt("Payment ID to void: ", 1);
        if (pid == null) {
            return;
        }
        String reason = input.readRawString("Void Reason (required): ");
        boolean replace = input.readYesNo("Issue replacement payment now?");
        BigDecimal newAmt = null;
        PaymentMethod newMethod = PaymentMethod.CARD;
        String newReceipt = null;
        if (replace) {
            newAmt = input.readBigDecimal("Replacement Amount", new BigDecimal("50.00"));
            newMethod = readPaymentMethod();
            // Receipt is required when a replacement was promised, so the success
            // message below always means a replacement payment was really created
            newReceipt = input.readRequiredString("New Unique Receipt No: ");
        }
        Payment res = ops.voidAndReplacePayment(pid.intValue(), reason, LocalDate.now().toString(),
                newAmt, newMethod, newReceipt);
        CliTheme.printSuccess("Void completed. Updated/Created Payment #" + res.getId()
                + " (Receipt " + res.getReceiptNo() + ")");
    }

    private PaymentMethod readPaymentMethod() {
        System.out.println("  Method: [1] CASH  [2] CARD  [3] TRANSFER");
        String m = input.readLine("Select [1-3]: ");
        if ("2".equals(m)) {
            return PaymentMethod.CARD;
        }
        if ("3".equals(m)) {
            return PaymentMethod.TRANSFER;
        }
        return PaymentMethod.CASH;
    }

    private Integer readNullableInt(String prompt) {
        while (true) {
            String raw = input.readLine(prompt);
            if (raw == null || raw.isEmpty()) {
                return null;
            }
            try {
                return Integer.valueOf(raw);
            } catch (NumberFormatException e) {
                System.out.println("[!] Enter digits or press Enter for null.");
            }
        }
    }
}
