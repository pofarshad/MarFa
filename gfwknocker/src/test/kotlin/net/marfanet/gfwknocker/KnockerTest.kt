package net.marfanet.gfwknocker

import org.junit.Test
import org.junit.Assert.*
import org.junit.Before
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class KnockerTest {

    @Before
    fun setUp() {
        // Reset knocker state before each test
    }

    @Test
    fun `start with valid config returns true when library not loaded`() {
        val config = KnockerConfig(
            enabled = true,
            sensitivity = KnockerSensitivity.MEDIUM
        )
        
        val result = Knocker.start(config)
        assertTrue("Should return true with stub implementation", result)
    }

    @Test
    fun `stop returns true when library not loaded`() {
        val result = Knocker.stop()
        assertTrue("Should return true with stub implementation", result)
    }

    @Test
    fun `isRunning returns false when library not loaded`() {
        val result = Knocker.isRunning()
        assertFalse("Should return false with stub implementation", result)
    }

    @Test
    fun `getStatus returns valid status when library not loaded`() {
        val status = Knocker.getStatus()
        
        assertFalse("Should not be running", status.isRunning)
        assertEquals("Should have zero packets knocked", 0L, status.packetsKnocked)
        assertEquals("Should have zero last knock time", 0L, status.lastKnockTime)
        assertEquals("Should contain error message", "Native library not loaded", status.errorMessage)
    }

    @Test
    fun `multiple start calls are handled gracefully`() {
        val config = KnockerConfig()
        
        val result1 = Knocker.start(config)
        val result2 = Knocker.start(config)
        
        assertTrue("First start should succeed", result1)
        assertTrue("Second start should succeed", result2)
    }

    @Test
    fun `config with different sensitivities works`() {
        val configs = listOf(
            KnockerConfig(sensitivity = KnockerSensitivity.LOW),
            KnockerConfig(sensitivity = KnockerSensitivity.MEDIUM),
            KnockerConfig(sensitivity = KnockerSensitivity.HIGH)
        )
        
        configs.forEach { config ->
            val result = Knocker.start(config)
            assertTrue("Should handle ${config.sensitivity} sensitivity", result)
        }
    }

    @Test
    fun `status reflects knocker state correctly`() {
        // Initially not running
        assertFalse("Should not be running initially", Knocker.isRunning())
        
        // Start knocker
        val config = KnockerConfig()
        Knocker.start(config)
        
        // Check status
        val status = Knocker.getStatus()
        assertNotNull("Status should not be null", status)
        assertEquals("Should have zero active targets initially", 0, status.activeTargets)
        assertEquals("Success rate should be 0.0", 0.0f, status.successRate, 0.01f)
    }

    @Test
    fun `concurrent operations are thread safe`() {
        val config = KnockerConfig()
        val threads = mutableListOf<Thread>()
        val results = mutableListOf<Boolean>()
        
        repeat(10) {
            val thread = Thread {
                synchronized(results) {
                    results.add(Knocker.start(config))
                }
            }
            threads.add(thread)
            thread.start()
        }
        
        threads.forEach { it.join() }
        
        assertEquals("All operations should complete", 10, results.size)
        assertTrue("All operations should succeed", results.all { it })
    }
}