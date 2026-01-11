package com.banking.auth.security;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * Input Sanitizer
 * Sanitizes user input to prevent injection attacks
 */
@Component
public class InputSanitizer {

    // Patterns for malicious input detection
    private static final Pattern SQL_INJECTION_PATTERN = Pattern.compile(
            "('.+--)|(--)|(;)|(\\b(ALTER|CREATE|DELETE|DROP|EXEC(UTE)?|INSERT( +INTO)?|MERGE|SELECT|UPDATE|UNION( +ALL)?)\\b)",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern XSS_PATTERN = Pattern.compile(
            "(<script>)|(</script>)|(<iframe>)|(</iframe>)|javascript:|onerror=|onload=|eval\\(|expression\\(",
            Pattern.CASE_INSENSITIVE
    );

    private static final Pattern PATH_TRAVERSAL_PATTERN = Pattern.compile(
            "(\\.\\./)|(\\.\\\\)",
            Pattern.CASE_INSENSITIVE
    );

    /**
     * Sanitize input string
     * Removes or escapes potentially malicious content
     */
    public String sanitize(String input) {
        if (input == null || input.isEmpty()) {
            return input;
        }

        // HTML encode special characters
        String sanitized = input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#x27;")
                .replace("/", "&#x2F;");

        return sanitized;
    }

    /**
     * Validate input against SQL injection patterns
     */
    public boolean isSqlInjectionSafe(String input) {
        if (input == null) {
            return true;
        }
        return !SQL_INJECTION_PATTERN.matcher(input).find();
    }

    /**
     * Validate input against XSS patterns
     */
    public boolean isXssSafe(String input) {
        if (input == null) {
            return true;
        }
        return !XSS_PATTERN.matcher(input).find();
    }

    /**
     * Validate input against path traversal patterns
     */
    public boolean isPathTraversalSafe(String input) {
        if (input == null) {
            return true;
        }
        return !PATH_TRAVERSAL_PATTERN.matcher(input).find();
    }

    /**
     * Comprehensive input validation
     * Checks for all common injection attacks
     */
    public boolean isInputSafe(String input) {
        return isSqlInjectionSafe(input) &&
               isXssSafe(input) &&
               isPathTraversalSafe(input);
    }

    /**
     * Validate and sanitize email input
     */
    public String sanitizeEmail(String email) {
        if (email == null) {
            return null;
        }

        // Remove whitespace
        email = email.trim().toLowerCase();

        // Basic email validation pattern
        Pattern emailPattern = Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
        if (!emailPattern.matcher(email).matches()) {
            throw new IllegalArgumentException("Invalid email format");
        }

        // Check for injection attempts
        if (!isInputSafe(email)) {
            throw new SecurityException("Email contains suspicious content");
        }

        return email;
    }

    /**
     * Validate and sanitize phone number
     */
    public String sanitizePhoneNumber(String phone) {
        if (phone == null) {
            return null;
        }

        // Remove all non-digit characters except + at the start
        String sanitized = phone.replaceAll("[^0-9+]", "");

        // Ensure only one + and it's at the start
        if (sanitized.indexOf('+') > 0) {
            throw new IllegalArgumentException("Invalid phone number format");
        }

        // Basic validation (10-15 digits)
        String digitsOnly = sanitized.replace("+", "");
        if (digitsOnly.length() < 10 || digitsOnly.length() > 15) {
            throw new IllegalArgumentException("Phone number must be 10-15 digits");
        }

        return sanitized;
    }

    /**
     * Sanitize alphanumeric input
     */
    public String sanitizeAlphanumeric(String input) {
        if (input == null) {
            return null;
        }

        // Remove all non-alphanumeric characters (except spaces, hyphens, underscores)
        return input.replaceAll("[^a-zA-Z0-9\\s\\-_]", "");
    }
}
