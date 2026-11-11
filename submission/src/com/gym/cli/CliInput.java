package com.gym.cli;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;
import java.util.Scanner;

// Safe console input helpers using java.util.Scanner
public class CliInput {

    private final Scanner scanner;

    public CliInput(Scanner scanner) {
        this.scanner = scanner;
    }

    public String readLine(String prompt) {
        System.out.print(prompt);
        if (!scanner.hasNextLine()) {
            return null;
        }
        return scanner.nextLine().trim();
    }

    public String readRequiredString(String prompt) {
        while (true) {
            String value = readLine(prompt);
            if (value == null) {
                return null;
            }
            if (!value.isEmpty()) {
                return value;
            }
            System.out.println("[!] Input cannot be empty (or enter a valid value).");
        }
    }

    // Allows submitting an empty line directly when deliberately testing validation errors
    public String readRawString(String prompt) {
        return readLine(prompt);
    }

    public String readOptionalString(String prompt, String defaultValue) {
        String suffix = defaultValue != null && !defaultValue.isEmpty() ? " [" + defaultValue + "]" : "";
        String value = readLine(prompt + suffix + ": ");
        if (value == null) {
            return null;
        }
        if (value.isEmpty()) {
            return defaultValue;
        }
        return value;
    }

    public Integer readInt(String prompt, int minValue) {
        while (true) {
            String raw = readLine(prompt);
            if (raw == null) {
                return null;
            }
            try {
                int parsed = Integer.parseInt(raw);
                if (parsed < minValue) {
                    System.out.println("[!] Please enter a number >= " + minValue + ".");
                    continue;
                }
                return parsed;
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid integer. Please enter digits only.");
            }
        }
    }

    public int readIntOrDefault(String prompt, int defaultValue) {
        while (true) {
            String raw = readLine(prompt + " [" + defaultValue + "]: ");
            if (raw == null || raw.isEmpty()) {
                return defaultValue;
            }
            try {
                return Integer.parseInt(raw);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid integer. Try again or press Enter for " + defaultValue + ".");
            }
        }
    }

    public Double readDouble(String prompt, Double defaultValue) {
        while (true) {
            String suffix = defaultValue != null ? " [" + defaultValue + "]" : "";
            String raw = readLine(prompt + suffix + ": ");
            if (raw == null) {
                return null;
            }
            if (raw.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            if (raw.isEmpty()) {
                System.out.println("[!] A value is required. Example: 72.5");
                continue;
            }
            try {
                return Double.parseDouble(raw);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid decimal number. Example: 72.5");
            }
        }
    }

    public Double readOptionalDouble(String prompt, Double currentValue) {
        String suffix = currentValue != null ? " [" + currentValue + "]" : " [optional, Enter to skip]";
        while (true) {
            String raw = readLine(prompt + suffix + ": ");
            if (raw == null || raw.isEmpty()) {
                return currentValue;
            }
            if ("-".equals(raw)) {
                return null;
            }
            try {
                return Double.parseDouble(raw);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid decimal number. Example: 68.0 (or '-' to clear).");
            }
        }
    }

    public BigDecimal readBigDecimal(String prompt, BigDecimal defaultValue) {
        while (true) {
            String suffix = defaultValue != null ? " [" + defaultValue.toPlainString() + "]" : "";
            String raw = readLine(prompt + suffix + ": ");
            if (raw == null) {
                return null;
            }
            if (raw.isEmpty() && defaultValue != null) {
                return defaultValue;
            }
            if (raw.isEmpty()) {
                System.out.println("[!] A value is required. Example: 45.00");
                continue;
            }
            try {
                return new BigDecimal(raw);
            } catch (NumberFormatException e) {
                System.out.println("[!] Invalid money amount. Example: 45.00");
            }
        }
    }

    public String readIsoDate(String prompt, String defaultDate) {
        while (true) {
            String suffix = defaultDate != null ? " [" + defaultDate + "]" : " (yyyy-MM-dd)";
            String raw = readLine(prompt + suffix + ": ");
            if (raw == null) {
                return null;
            }
            if (raw.isEmpty() && defaultDate != null) {
                return defaultDate;
            }
            if (raw.isEmpty()) {
                System.out.println("[!] A date is required. Expected yyyy-MM-dd (e.g. 2026-11-09).");
                continue;
            }
            try {
                LocalDate.parse(raw);
                return raw;
            } catch (DateTimeParseException e) {
                System.out.println("[!] Invalid date format. Expected yyyy-MM-dd (e.g. 2026-11-09).");
            }
        }
    }

    public String readIsoTime(String prompt, String defaultTime) {
        while (true) {
            String suffix = defaultTime != null ? " [" + defaultTime + "]" : " (HH:mm)";
            String raw = readLine(prompt + suffix + ": ");
            if (raw == null) {
                return null;
            }
            if (raw.isEmpty() && defaultTime != null) {
                return defaultTime;
            }
            if (raw.isEmpty()) {
                System.out.println("[!] A time is required. Expected HH:mm (e.g. 14:30).");
                continue;
            }
            try {
                LocalTime.parse(raw);
                return raw;
            } catch (DateTimeParseException e) {
                System.out.println("[!] Invalid time format. Expected HH:mm (e.g. 14:30).");
            }
        }
    }

    public boolean readYesNo(String prompt) {
        while (true) {
            String raw = readLine(prompt + " (y/n): ");
            if (raw == null) {
                return false;
            }
            if ("y".equalsIgnoreCase(raw) || "yes".equalsIgnoreCase(raw)) {
                return true;
            }
            if ("n".equalsIgnoreCase(raw) || "no".equalsIgnoreCase(raw)) {
                return false;
            }
            System.out.println("[!] Enter y or n.");
        }
    }

    public void pause() {
        System.out.print("\nPress [ENTER] to continue...");
        if (scanner.hasNextLine()) {
            scanner.nextLine();
        }
    }
}

