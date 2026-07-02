package io.rocketpartners.hris.feature.calendar

import org.junit.Assert.assertEquals
import org.junit.Test

class EventDisplayTitleTest {

    @Test
    fun holiday_getsPhFlagPrefix() {
        assertEquals("🇵🇭 Independence Day — Company Holiday", eventDisplayTitle("holiday", "Independence Day — Company Holiday"))
    }

    @Test
    fun holiday_matchIsCaseInsensitive() {
        assertEquals("🇵🇭 New Year", eventDisplayTitle("HOLIDAY", "New Year"))
    }

    @Test
    fun nonHoliday_titleUnchanged() {
        assertEquals("Team Sync", eventDisplayTitle("meeting", "Team Sync"))
        assertEquals("Some Event", eventDisplayTitle(null, "Some Event"))
    }
}
