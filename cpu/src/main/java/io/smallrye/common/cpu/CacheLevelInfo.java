package io.smallrye.common.cpu;

/**
 * Information about a CPU cache level.
 */
public final class CacheLevelInfo {
    private final int cacheLevel;
    private final CacheType cacheType;
    private final int cacheLevelSizeKB;
    private final int cacheLineSize;

    CacheLevelInfo(final int cacheLevel, final CacheType cacheType, final int cacheLevelSizeKB, final int cacheLineSize) {
        this.cacheLevel = cacheLevel;
        this.cacheType = cacheType;
        this.cacheLevelSizeKB = cacheLevelSizeKB;
        this.cacheLineSize = cacheLineSize;
    }

    /**
     * Get the level index. For example, the level of L1 cache will be "1", L2 will be "2", etc. If the level is
     * not known, 0 is returned.
     *
     * @return the level index, or 0 if unknown
     */
    public int getCacheLevel() {
        return cacheLevel;
    }

    /**
     * Get the type of cache. If the type is unknown, {@link CacheType#UNKNOWN} is returned.
     *
     * @return the type of cache (not {@code null})
     */
    public CacheType getCacheType() {
        return cacheType;
    }

    /**
     * Get the size of this cache level in kilobytes. If the size is unknown, 0 is returned.
     *
     * @return the size of this cache level in kilobytes, or 0 if unknown
     */
    public int getCacheLevelSizeKB() {
        return cacheLevelSizeKB;
    }

    /**
     * Get the cache line size in bytes. If the size is unknown, 0 is returned.
     *
     * @return the cache line size in bytes, or 0 if unknown
     */
    public int getCacheLineSize() {
        return cacheLineSize;
    }
}
