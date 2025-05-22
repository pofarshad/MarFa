package net.marfanet.gfwknocker

import org.junit.Test
import org.junit.Assert.*

class KnockerConfigTest {

    @Test
    fun `default config has expected values`() {
        val config = KnockerConfig()
        
        assertTrue("Should be enabled by default", config.enabled)
        assertEquals("Default sensitivity should be MEDIUM", KnockerSensitivity.MEDIUM, config.sensitivity)
        assertEquals("Default interval should be 1000ms", 1000L, config.knockInterval)
        assertEquals("Default max retries should be 3", 3, config.maxRetries)
        assertEquals("Default timeout should be 5000ms", 5000L, config.timeout)
        assertEquals("Should have default target hosts", 3, config.targetHosts.size)
        assertTrue("Should contain 8.8.8.8", config.targetHosts.contains("8.8.8.8"))
        assertEquals("Should have default knock ports", 4, config.knockPorts.size)
        assertTrue("Should contain port 53", config.knockPorts.contains(53))
        assertEquals("Default detect method should be ADAPTIVE", DetectionMethod.ADAPTIVE, config.detectMethod)
    }

    @Test
    fun `toJsonString creates valid JSON`() {
        val config = KnockerConfig(
            enabled = true,
            sensitivity = KnockerSensitivity.HIGH,
            knockInterval = 2000,
            maxRetries = 5
        )
        
        val json = config.toJsonString()
        
        assertTrue("JSON should contain enabled", json.contains("\"enabled\":true"))
        assertTrue("JSON should contain sensitivity", json.contains("\"sensitivity\":\"HIGH\""))
        assertTrue("JSON should contain knockInterval", json.contains("\"knockInterval\":2000"))
        assertTrue("JSON should contain maxRetries", json.contains("\"maxRetries\":5"))
    }

    @Test
    fun `fromJsonString reconstructs config correctly`() {
        val originalConfig = KnockerConfig(
            enabled = false,
            sensitivity = KnockerSensitivity.LOW,
            knockInterval = 3000,
            maxRetries = 1,
            timeout = 10000
        )
        
        val json = originalConfig.toJsonString()
        val reconstructedConfig = KnockerConfig.fromJsonString(json)
        
        assertEquals("Enabled should match", originalConfig.enabled, reconstructedConfig.enabled)
        assertEquals("Sensitivity should match", originalConfig.sensitivity, reconstructedConfig.sensitivity)
        assertEquals("Knock interval should match", originalConfig.knockInterval, reconstructedConfig.knockInterval)
        assertEquals("Max retries should match", originalConfig.maxRetries, reconstructedConfig.maxRetries)
        assertEquals("Timeout should match", originalConfig.timeout, reconstructedConfig.timeout)
    }

    @Test
    fun `fromJsonString handles malformed JSON gracefully`() {
        val malformedJson = "{\"invalid\":true"
        
        try {
            KnockerConfig.fromJsonString(malformedJson)
            fail("Should throw exception for malformed JSON")
        } catch (e: Exception) {
            // Expected behavior
        }
    }

    @Test
    fun `fromJsonString uses defaults for missing fields`() {
        val partialJson = "{\"enabled\":false}"
        val config = KnockerConfig.fromJsonString(partialJson)
        
        assertFalse("Should use provided enabled value", config.enabled)
        assertEquals("Should use default sensitivity", KnockerSensitivity.MEDIUM, config.sensitivity)
        assertEquals("Should use default interval", 1000L, config.knockInterval)
    }

    @Test
    fun `custom target hosts and ports work correctly`() {
        val customHosts = listOf("1.1.1.1", "9.9.9.9")
        val customPorts = listOf(80, 443)
        
        val config = KnockerConfig(
            targetHosts = customHosts,
            knockPorts = customPorts
        )
        
        assertEquals("Should have custom hosts", customHosts, config.targetHosts)
        assertEquals("Should have custom ports", customPorts, config.knockPorts)
        
        val json = config.toJsonString()
        val reconstructed = KnockerConfig.fromJsonString(json)
        
        assertEquals("Custom hosts should survive serialization", customHosts, reconstructed.targetHosts)
        assertEquals("Custom ports should survive serialization", customPorts, reconstructed.knockPorts)
    }

    @Test
    fun `all sensitivity levels are supported`() {
        KnockerSensitivity.values().forEach { sensitivity ->
            val config = KnockerConfig(sensitivity = sensitivity)
            val json = config.toJsonString()
            val reconstructed = KnockerConfig.fromJsonString(json)
            
            assertEquals("Sensitivity $sensitivity should survive serialization", sensitivity, reconstructed.sensitivity)
        }
    }

    @Test
    fun `all detection methods are supported`() {
        DetectionMethod.values().forEach { method ->
            val config = KnockerConfig(detectMethod = method)
            val json = config.toJsonString()
            val reconstructed = KnockerConfig.fromJsonString(json)
            
            assertEquals("Detection method $method should survive serialization", method, reconstructed.detectMethod)
        }
    }
}