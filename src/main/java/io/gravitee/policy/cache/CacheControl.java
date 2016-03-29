package io.gravitee.policy.cache;

/**
 * @author David BRASSELY (david at gravitee.io)
 * @author GraviteeSource Team
 */
public class CacheControl {

    private boolean noCache;
    private boolean noStore;
    private boolean noTransform;
    private boolean mustRevalidate;
    private boolean isPrivate;
    private boolean isPublic;
    private long maxAge = -1;
    private long sMaxAge = -1;

    public boolean isPrivate() {
        return isPrivate;
    }

    public void setPrivate(boolean aPrivate) {
        isPrivate = aPrivate;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean aPublic) {
        isPublic = aPublic;
    }

    public long getMaxAge() {
        return maxAge;
    }

    public void setMaxAge(long maxAge) {
        this.maxAge = maxAge;
    }

    public boolean isMustRevalidate() {
        return mustRevalidate;
    }

    public void setMustRevalidate(boolean mustRevalidate) {
        this.mustRevalidate = mustRevalidate;
    }

    public boolean isNoCache() {
        return noCache;
    }

    public void setNoCache(boolean noCache) {
        this.noCache = noCache;
    }

    public boolean isNoStore() {
        return noStore;
    }

    public void setNoStore(boolean noStore) {
        this.noStore = noStore;
    }

    public boolean isNoTransform() {
        return noTransform;
    }

    public void setNoTransform(boolean noTransform) {
        this.noTransform = noTransform;
    }

    public long getSMaxAge() {
        return sMaxAge;
    }

    public void setSMaxAge(long sMaxAge) {
        this.sMaxAge = sMaxAge;
    }
}
