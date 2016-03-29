package io.gravitee.policy.cache.util;

import org.junit.Assert;
import org.junit.Test;

import java.time.Instant;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class ExpiresUtilTest {

    @Test
    public void shouldParseExpiresHeader() {
        Instant instant = ExpiresUtil.parseExpires("Thu, 01 Dec 1994 16:00:00 GMT");
        Assert.assertNotNull(instant);
    }

    @Test
    public void shouldNotParseExpiresHeader() {
        Instant instant = ExpiresUtil.parseExpires("Thu, 01 Dc 1994 16:00:00 GMT");
        Assert.assertNull(instant);
    }

    @Test
    public void shouldNotParseNullExpiresHeader() {
        Instant instant = ExpiresUtil.parseExpires(null);
        Assert.assertNull(instant);
    }
}
