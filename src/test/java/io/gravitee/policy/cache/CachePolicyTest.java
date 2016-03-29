package io.gravitee.policy.cache;

import io.gravitee.common.http.HttpHeaders;
import io.gravitee.gateway.api.ClientResponse;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;

import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
@RunWith(MockitoJUnitRunner.class)
public class CachePolicyTest {

    @Mock
    protected ClientResponse response;

    @Before
    public void init() {
        initMocks(this);
    }

    @Test
    public void should_usecachecontrol_smaxage() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAll(new HashMap<String, String>() {
            {
                put(HttpHeaders.CACHE_CONTROL, "max-age=600, no-cache, no-store, smax-age=300");
                put(HttpHeaders.EXPIRES, "Thu, 01 Dec 1994 16:00:00 GMT");
            }
        });

        when(response.headers()).thenReturn(headers);

        long timeToLive = CachePolicy.timeToLiveFromResponse(response);
        Assert.assertEquals(300, timeToLive);
    }

    @Test
    public void should_usecachecontrol_maxage() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAll(new HashMap<String, String>() {
            {
                put(HttpHeaders.CACHE_CONTROL, "max-age=600, no-cache, no-store");
                put(HttpHeaders.EXPIRES, "Thu, 01 Dec 1994 16:00:00 GMT");
            }
        });

        when(response.headers()).thenReturn(headers);

        long timeToLive = CachePolicy.timeToLiveFromResponse(response);
        Assert.assertEquals(600, timeToLive);
    }

    @Test
    public void should_usecachecontrol_expires_past() {
        final HttpHeaders headers = new HttpHeaders();
        headers.setAll(new HashMap<String, String>() {
            {
                put(HttpHeaders.CACHE_CONTROL, "no-cache, no-store");
                put(HttpHeaders.EXPIRES, "Thu, 01 Dec 1994 16:00:00 GMT");
            }
        });

        when(response.headers()).thenReturn(headers);

        long timeToLive = CachePolicy.timeToLiveFromResponse(response);
        Assert.assertEquals(-1, timeToLive);
    }
}
