package net.marfanet.benchmark

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * MarFaNet Cold Start Benchmark
 * Target: ≤ 1.2s cold start time (vs 1.8s baseline)
 */
@RunWith(AndroidJUnit4::class)
class ColdStartBenchmark {
    
    @get:Rule
    val benchmarkRule = MacrobenchmarkRule()

    @Test
    fun coldStartupFromScratch() = benchmarkRule.measureRepeated(
        packageName = "net.marfanet.android",
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        iterations = 10,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.DEFAULT
    ) {
        pressHome()
        startActivityAndWait()
        
        // Wait for app to fully load
        waitForMainScreen()
    }

    @Test
    fun coldStartupOptimized() = benchmarkRule.measureRepeated(
        packageName = "net.marfanet.android",
        metrics = listOf(StartupTimingMetric()),
        iterations = 15,
        startupMode = StartupMode.COLD,
        compilationMode = CompilationMode.Full()
    ) {
        pressHome()
        startActivityAndWait()
        
        // Measure time to interactive state
        waitForInteractiveState()
    }

    @Test
    fun warmStartupBenchmark() = benchmarkRule.measureRepeated(
        packageName = "net.marfanet.android",
        metrics = listOf(StartupTimingMetric(), FrameTimingMetric()),
        iterations = 8,
        startupMode = StartupMode.WARM,
        compilationMode = CompilationMode.DEFAULT
    ) {
        pressHome()
        startActivityAndWait()
        waitForMainScreen()
    }

    private fun MacrobenchmarkScope.waitForMainScreen() {
        // Wait for main screen elements to appear
        device.wait(
            Until.hasObject(By.text("MarFaNet").pkg("net.marfanet.android")),
            5000
        )
        
        // Wait for connection status indicator
        device.wait(
            Until.hasObject(By.res("net.marfanet.android", "connection_status")),
            3000
        )
    }

    private fun MacrobenchmarkScope.waitForInteractiveState() {
        // Wait for app to be fully interactive
        device.wait(
            Until.hasObject(By.clickable(true).pkg("net.marfanet.android")),
            5000
        )
        
        // Ensure VPN toggle is ready
        device.wait(
            Until.hasObject(By.res("net.marfanet.android", "vpn_toggle")),
            2000
        )
    }
}