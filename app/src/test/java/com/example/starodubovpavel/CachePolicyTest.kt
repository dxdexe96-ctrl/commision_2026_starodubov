package com.example.starodubovpavel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CachePolicyTest {
    private val hour = 60L * 60L * 1000L

    @Test
    fun freshCacheIsUsedOffline() {
        assertTrue(CachePolicy.isFresh(updatedAt = 10 * hour, ttlHours = 6, now = 15 * hour))
    }

    @Test
    fun expiredCacheTriggersBackgroundRefresh() {
        assertFalse(CachePolicy.isFresh(updatedAt = 10 * hour, ttlHours = 6, now = 17 * hour))
        assertFalse(CachePolicy.isFresh(updatedAt = 10 * hour, ttlHours = 0, now = 10 * hour))
    }
}
