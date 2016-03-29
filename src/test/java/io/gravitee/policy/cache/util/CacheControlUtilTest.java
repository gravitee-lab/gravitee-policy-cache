package io.gravitee.policy.cache.util;

import io.gravitee.policy.cache.CacheControl;
import io.gravitee.policy.cache.util.CacheControlUtil;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class CacheControlUtilTest {

    @Test
    public void shouldExtractCacheControl() {
        CacheControl cacheControl = CacheControlUtil.parseCacheControl("max-age=0, no-cache, no-store");
        Assert.assertEquals(0, cacheControl.getMaxAge());
        Assert.assertTrue(cacheControl.isNoCache());
    }

    @Test
    public void shouldExtractCacheControl2() {
        CacheControl cacheControl = CacheControlUtil.parseCacheControl("no-transform,public,max-age=300,s-maxage=900");
        Assert.assertEquals(300, cacheControl.getMaxAge());
        Assert.assertEquals(900, cacheControl.getSMaxAge());
        Assert.assertTrue(cacheControl.isNoTransform());
        Assert.assertTrue(cacheControl.isPublic());
    }
}
