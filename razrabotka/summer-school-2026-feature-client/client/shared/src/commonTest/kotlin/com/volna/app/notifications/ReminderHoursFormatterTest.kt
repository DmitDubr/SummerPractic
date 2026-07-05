package notifications

import com.volna.app.notifications.toReminderLeadText
import kotlin.test.Test
import kotlin.test.assertEquals

class ReminderHoursFormatterTest {
    @Test
    fun formatsSingleAndMultipleHours() {
        assertEquals("заранее", emptyList<Int>().toReminderLeadText())
        assertEquals("за 24 ч до старта", listOf(24).toReminderLeadText())
        assertEquals("за 24 и 2 ч до старта", listOf(24, 2).toReminderLeadText())
    }
}
