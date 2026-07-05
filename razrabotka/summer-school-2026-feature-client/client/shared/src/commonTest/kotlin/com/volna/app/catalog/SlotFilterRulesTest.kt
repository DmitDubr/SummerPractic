package catalog

import com.volna.app.catalog.SlotFilters
import com.volna.app.catalog.hasActiveFilters
import com.volna.app.domain.model.InstructorId
import com.volna.app.domain.model.RouteType
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SlotFilterRulesTest {
    @Test
    fun defaultFiltersAreInactive() {
        assertFalse(SlotFilters().hasActiveFilters())
    }

    @Test
    fun nonDefaultFiltersAreActive() {
        assertTrue(SlotFilters(onlyAvailable = true).hasActiveFilters())
        assertTrue(SlotFilters(routeTypes = setOf(RouteType.Novice)).hasActiveFilters())
        assertTrue(SlotFilters(instructorIds = setOf(InstructorId("inst-1"))).hasActiveFilters())
    }
}
