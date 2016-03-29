package io.gravitee.policy.cache.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public final class ExpiresUtil {

    // The format is an absolute date and time as defined by HTTP-date in section 3.3.1;
    // it MUST be in RFC 1123 date format (example: Thu, 01 Dec 1994 16:00:00 GMT)
    private final static DateTimeFormatter EXPIRES_HEADER_FORMATTER =
            DateTimeFormatter.ofPattern("EEE, dd MMM yyyy HH:mm:ss z").withLocale(Locale.ENGLISH);

    private ExpiresUtil() {}

    public static Instant parseExpires(String expires) {
        if (expires == null) {
            return null;
        }

        try {
            return LocalDateTime.parse(expires, EXPIRES_HEADER_FORMATTER).toInstant(ZoneOffset.UTC);
        } catch (DateTimeParseException dtpe) {
            return null;
        }
    }
}
