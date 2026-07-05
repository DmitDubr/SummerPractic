package network

import com.volna.app.core.network.isBrowserFetchFailure
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class VolnaApiClientFailureMappingTest {
    @Test
    fun browserFetchFailuresAreDetected() {
        assertTrue("Fail to fetch".isBrowserFetchFailure())
        assertTrue("TypeError: Failed to fetch".isBrowserFetchFailure())
        assertTrue("Load failed".isBrowserFetchFailure())
        assertFalse("invalid code".isBrowserFetchFailure())
        assertFalse(null.isBrowserFetchFailure())
    }
}
