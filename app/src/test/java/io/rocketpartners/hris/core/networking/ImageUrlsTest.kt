package io.rocketpartners.hris.core.networking

import io.rocketpartners.hris.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ImageUrlsTest {

    @Test
    fun resolve_nullAndBlankReturnNull() {
        assertNull(ImageUrls.resolve(null))
        assertNull(ImageUrls.resolve(""))
        assertNull(ImageUrls.resolve("   "))
    }

    @Test
    fun resolve_absoluteUrlUnchanged() {
        assertEquals("https://cdn.example.com/a.jpg", ImageUrls.resolve("https://cdn.example.com/a.jpg"))
        assertEquals("http://x/y.png", ImageUrls.resolve("http://x/y.png"))
    }

    @Test
    fun resolve_relativePathJoinedOntoBase() {
        assertEquals("${BuildConfig.BASE_URL}/uploads/images/x.jpg", ImageUrls.resolve("/uploads/images/x.jpg"))
    }

    @Test
    fun resolve_addsLeadingSlashWhenMissing() {
        assertEquals("${BuildConfig.BASE_URL}/uploads/x.jpg", ImageUrls.resolve("uploads/x.jpg"))
    }
}
