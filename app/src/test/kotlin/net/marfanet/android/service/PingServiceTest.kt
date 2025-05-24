package net.marfanet.android.service

import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import javax.inject.Inject
import dagger.hilt.android.testing.HiltTestApplication

@HiltAndroidTest
@RunWith(RobolectricTestRunner::class)
@Config(application = HiltTestApplication::class)
class PingServiceTest {

    class MyDummy @Inject constructor()

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @Inject
    lateinit var dummy: MyDummy

    @Before
    fun setUp() {
        hiltRule.inject()
        assertNotNull("Dummy should be injected by Hilt in setUp", dummy)
    }

    @Test
    fun `simple injection test`() {
        assertNotNull("Dummy should remain injected in test", dummy)
    }
}
