package com.gym.util;

import com.gym.exception.ValidationException;

// Shared validator for Egyptian mobile numbers used by all DAOs and controllers.
// Rule: exactly 11 digits starting with 010, 011, 012 or 015 (e.g. 01012345678).
public final class PhoneUtil {

    // Egyptian mobile format: prefix 01 followed by 0/1/2/5, then 8 more digits
    public static final String EGYPT_MOBILE_PATTERN = "01[0125]\\d{8}";

    private PhoneUtil() {
    }

    // Returns true when the value is a valid Egyptian mobile number
    public static boolean isValid(String phone) {
        return phone != null && phone.trim().matches(EGYPT_MOBILE_PATTERN);
    }

    // Throws when the value is blank or not a valid Egyptian mobile number
    public static void requireValid(String phone, String fieldLabel) throws ValidationException {
        String trimmed = phone == null ? "" : phone.trim();
        if (trimmed.isEmpty()) {
            throw new ValidationException(fieldLabel + " is required");
        }
        if (!trimmed.matches(EGYPT_MOBILE_PATTERN)) {
            throw new ValidationException(fieldLabel
                    + " must be a valid Egyptian mobile number: 11 digits starting with 010, 011, 012 or 015 (got: "
                    + trimmed + ")");
        }
    }
}
