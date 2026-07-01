package io.rocketpartners.hris.designsystem

import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingAndHtmlTest {

    @Test
    fun greeting_switchesByHour() {
        assertEquals("Good morning", Greeting.text(5))
        assertEquals("Good morning", Greeting.text(11))
        assertEquals("Good afternoon", Greeting.text(12))
        assertEquals("Good afternoon", Greeting.text(16))
        assertEquals("Good evening", Greeting.text(17))
        assertEquals("Good evening", Greeting.text(4))
    }

    @Test
    fun htmlToPlainText_stripsTagsScriptsAndDecodesEntities() {
        val html = "<p>Hello&nbsp;<b>world</b></p><script>alert('x')</script> &amp; more"
        assertEquals("Hello world & more", htmlToPlainText(html))
    }

    @Test
    fun htmlToPlainText_removesStyleBlocksAndCollapsesWhitespace() {
        val html = "<style>.a{color:red}</style>  Line   one\n\n  two "
        assertEquals("Line one two", htmlToPlainText(html))
    }

    @Test
    fun initials_derivesUpToTwoLetters() {
        assertEquals("AS", Initials.from("Angelo Soliveres"))
        assertEquals("M", Initials.from("Madonna"))
        assertEquals("?", Initials.from("   "))
        assertEquals("?", Initials.from(null))
    }
}
