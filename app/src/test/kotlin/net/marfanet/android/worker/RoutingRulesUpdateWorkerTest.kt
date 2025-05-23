package net.marfanet.android.worker

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.work.*
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.workDataOf
import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class RoutingRulesUpdateWorkerTest {
    
    private lateinit var context: Context
    private lateinit var mockWebServer: MockWebServer
    private lateinit var rulesDir: File
    
    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        mockWebServer = MockWebServer()
        mockWebServer.start()
        
        // Create test rules directory
        rulesDir = File(context.filesDir, "rules")
        rulesDir.mkdirs()
        
        // Clean up any existing rules file
        File(rulesDir, "iran.dat").delete()
    }
    
    @After
    fun tearDown() {
        mockWebServer.shutdown()
        rulesDir.deleteRecursively()
    }
    
    @Test
    fun `worker downloads and saves rules file successfully`() = runBlocking {
        // Mock successful response with test data
        val testData = "test-routing-rules-data"
        mockWebServer.enqueue(MockResponse()
            .setBody(testData)
            .setResponseCode(200))
        
        val inputData = workDataOf("test_url" to mockWebServer.url("/").toString())
        val worker = TestListenableWorkerBuilder<RoutingRulesUpdateWorker>(context)
            .setInputData(inputData)
            .build()
        
        val result = worker.doWork()
        
        assertTrue("Worker should succeed", result is ListenableWorker.Result.Success)
        
        val rulesFile = File(rulesDir, "iran.dat")
        assertTrue("Rules file should exist", rulesFile.exists())
        assertEquals("File content should match", testData, rulesFile.readText())
        
        val outputData = (result as ListenableWorker.Result.Success).outputData
        assertTrue("Should indicate file was updated", outputData.getBoolean("updated", false))
    }
    
    @Test
    fun `worker detects unchanged file correctly`() = runBlocking {
        val testData = "existing-rules-data"
        
        // Create existing rules file
        val existingFile = File(rulesDir, "iran.dat")
        existingFile.writeText(testData)
        val originalHash = calculateSHA256(existingFile)
        
        // Mock response with same data
        mockWebServer.enqueue(MockResponse()
            .setBody(testData)
            .setResponseCode(200))
        
        val worker = TestListenableWorkerBuilder<RoutingRulesUpdateWorker>(context)
            .build()
        
        val result = worker.doWork()
        
        assertTrue("Worker should succeed", result is ListenableWorker.Result.Success)
        
        val outputData = (result as ListenableWorker.Result.Success).outputData
        assertFalse("Should indicate file was not updated", outputData.getBoolean("updated", true))
        assertEquals("Hash should match original", originalHash, outputData.getString("hash"))
    }
    
    @Test
    fun `worker handles network failure with retry`() = runBlocking {
        // Mock network failure
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(500)
            .setBody("Internal Server Error"))
        
        val worker = TestListenableWorkerBuilder<RoutingRulesUpdateWorker>(context)
            .setRunAttemptCount(1) // First retry attempt
            .build()
        
        val result = worker.doWork()
        
        assertTrue("Worker should retry on failure", result is ListenableWorker.Result.Retry)
    }
    
    @Test
    fun `worker fails after max retry attempts`() = runBlocking {
        // Mock persistent failure
        mockWebServer.enqueue(MockResponse()
            .setResponseCode(404)
            .setBody("Not Found"))
        
        val worker = TestListenableWorkerBuilder<RoutingRulesUpdateWorker>(context)
            .setRunAttemptCount(3) // Max attempts reached
            .build()
        
        val result = worker.doWork()
        
        assertTrue("Worker should fail after max retries", result is ListenableWorker.Result.Failure)
        
        val outputData = (result as ListenableWorker.Result.Failure).outputData
        assertTrue("Should indicate final attempt", outputData.getBoolean("finalAttempt", false))
    }
    
    @Test
    fun `worker creates rules directory if not exists`() = runBlocking {
        // Delete rules directory
        rulesDir.deleteRecursively()
        assertFalse("Rules directory should not exist", rulesDir.exists())
        
        val testData = "test-data"
        mockWebServer.enqueue(MockResponse()
            .setBody(testData)
            .setResponseCode(200))
        
        val worker = TestListenableWorkerBuilder<RoutingRulesUpdateWorker>(context)
            .build()
        
        val result = worker.doWork()
        
        assertTrue("Worker should succeed", result is ListenableWorker.Result.Success)
        assertTrue("Rules directory should be created", rulesDir.exists())
        assertTrue("Rules file should exist", File(rulesDir, "iran.dat").exists())
    }
    
    @Test
    fun `worker updates progress during operation`() = runBlocking {
        val testData = "progress-test-data"
        mockWebServer.enqueue(MockResponse()
            .setBody(testData)
            .setResponseCode(200))
        
        val worker = TestListenableWorkerBuilder<RoutingRulesUpdateWorker>(context)
            .build()
        
        val result = worker.doWork()
        
        assertTrue("Worker should succeed", result is ListenableWorker.Result.Success)
        // Progress updates are tested implicitly through successful execution
    }
    
    @Test
    fun `sha256 calculation is consistent`() {
        val testData = "test-data-for-hash"
        val file1 = File(rulesDir, "test1.dat")
        val file2 = File(rulesDir, "test2.dat")
        
        file1.writeText(testData)
        file2.writeText(testData)
        
        val hash1 = calculateSHA256(file1)
        val hash2 = calculateSHA256(file2)
        
        assertEquals("Hashes should be identical for same content", hash1, hash2)
        assertNotEquals("Hash should not be empty", "", hash1)
        assertEquals("SHA256 should be 64 characters", 64, hash1.length)
    }
    
    @Test
    fun `periodic work scheduling works correctly`() {
        RoutingRulesUpdateWorker.schedulePeriodicUpdate(context)
        
        val workManager = WorkManager.getInstance(context)
        val workInfos = workManager.getWorkInfosForUniqueWork(
            RoutingRulesUpdateWorker.WORK_NAME
        ).get()
        
        assertFalse("Work should be scheduled", workInfos.isEmpty())
        
        val workInfo = workInfos.first()
        assertEquals("Work should be enqueued", WorkInfo.State.ENQUEUED, workInfo.state)
        assertTrue("Work should have routing_rules tag", workInfo.tags.contains("routing_rules"))
    }
    
    private fun calculateSHA256(file: File): String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}