package io.rocketpartners.hris.feature.common

import io.rocketpartners.hris.model.UploadFile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentPickerTest {

    @Test
    fun imageExtension_pngStaysPngOtherwiseJpg() {
        assertEquals("png", imageExtension("image/png"))
        assertEquals("jpg", imageExtension("image/jpeg"))
        assertEquals("jpg", imageExtension("image/heic"))
        assertEquals("jpg", imageExtension(null))
    }

    @Test
    fun maxAttachments_isFive() {
        assertEquals(5, MAX_ATTACHMENTS)
    }

    @Test
    fun uploadFile_enforcesTenMegabyteCap() {
        val ok = UploadFile("a.jpg", "image/jpeg", ByteArray(UploadFile.MAX_BYTES))
        val tooBig = UploadFile("b.jpg", "image/jpeg", ByteArray(UploadFile.MAX_BYTES + 1))
        assertTrue(ok.isWithinSizeLimit)
        assertFalse(tooBig.isWithinSizeLimit)
    }
}
