package com.gym.cli;

import java.util.List;

// Pure ASCII formatting helpers for Windows CMD terminal screens
public class CliTheme {

    public static final int LINE_WIDTH = 80;

    private CliTheme() {
    }

    public static void printDoubleLine() {
        System.out.println("+" + repeat('=', LINE_WIDTH - 2) + "+");
    }

    public static void printSingleLine() {
        System.out.println("+" + repeat('-', LINE_WIDTH - 2) + "+");
    }

    public static void printHeader(String title, String userLabel, String dbUrl, String breadcrumb) {
        System.out.println();
        printDoubleLine();
        String left = " " + safeText(title);
        String right = safeText(userLabel) + " | " + shortDbName(dbUrl) + " ";
        int gap = (LINE_WIDTH - 2) - left.length() - right.length();
        if (gap < 1) {
            String combined = truncate(left + " | " + right, LINE_WIDTH - 2);
            System.out.println("|" + padRight(combined, LINE_WIDTH - 2) + "|");
        } else {
            System.out.println("|" + left + repeat(' ', gap) + right + "|");
        }
        String pathLine = " Path: " + safeText(breadcrumb);
        System.out.println("|" + padRight(truncate(pathLine, LINE_WIDTH - 2), LINE_WIDTH - 2) + "|");
        printDoubleLine();
    }

    public static void printSectionTitle(String title) {
        System.out.println();
        System.out.println("--- " + safeText(title) + " ---");
    }

    public static void printFooterNav(boolean showHome) {
        printSingleLine();
        if (showHome) {
            System.out.println("  [0] Back    [H] Main Menu    [L] Logout/Switch User");
        } else {
            System.out.println("  [0] Exit");
        }
        printSingleLine();
    }

    public static void printSuccess(String message) {
        System.out.println();
        System.out.println("[OK] " + safeText(message));
    }

    public static void printValidationError(String message) {
        System.out.println();
        System.out.println("[! VALIDATION ERROR] " + safeText(message));
    }

    public static void printDatabaseError(String message) {
        System.out.println();
        System.out.println("[! DATABASE ERROR] " + safeText(message));
    }

    public static void printNotice(String message) {
        System.out.println("[INFO] " + safeText(message));
    }

    public static void printTable(String[] headers, List<String[]> rows, int[] widths) {
        String divider = buildTableDivider(widths);
        System.out.println(divider);
        System.out.println(formatRow(headers, widths));
        System.out.println(divider);
        if (rows == null || rows.isEmpty()) {
            System.out.println("| (No records found)");
            System.out.println(divider);
            return;
        }
        for (int i = 0; i < rows.size(); i++) {
            System.out.println(formatRow(rows.get(i), widths));
        }
        System.out.println(divider);
    }

    private static String buildTableDivider(int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append("+");
        for (int i = 0; i < widths.length; i++) {
            sb.append(repeat('-', widths[i] + 2));
            sb.append("+");
        }
        return sb.toString();
    }

    private static String formatRow(String[] cols, int[] widths) {
        StringBuilder sb = new StringBuilder();
        sb.append("|");
        for (int i = 0; i < widths.length; i++) {
            String cell = i < cols.length ? safeText(cols[i]) : "";
            sb.append(" ");
            sb.append(padRight(truncate(cell, widths[i]), widths[i]));
            sb.append(" |");
        }
        return sb.toString();
    }

    public static String padRight(String text, int width) {
        String value = safeText(text);
        if (value.length() >= width) {
            return value.substring(0, width);
        }
        return value + repeat(' ', width - value.length());
    }

    public static String truncate(String text, int maxLen) {
        String value = safeText(text);
        if (maxLen <= 3 || value.length() <= maxLen) {
            return value;
        }
        return value.substring(0, maxLen - 3) + "...";
    }

    public static String safeText(String value) {
        if (value == null) {
            return "";
        }
        return value.replace('\r', ' ').replace('\n', ' ');
    }

    public static String repeat(char ch, int count) {
        if (count <= 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(ch);
        }
        return sb.toString();
    }

    private static String shortDbName(String dbUrl) {
        if (dbUrl == null || dbUrl.isEmpty()) {
            return "gym.db";
        }
        int prefix = dbUrl.lastIndexOf(':');
        if (prefix >= 0 && prefix < dbUrl.length() - 1) {
            return dbUrl.substring(prefix + 1);
        }
        return dbUrl;
    }
}

