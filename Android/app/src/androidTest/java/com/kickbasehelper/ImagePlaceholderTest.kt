package com.kickbasehelper

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ImagePlaceholderTest {
    @get:Rule
    val composeRule = createAndroidComposeRule(MainActivity::class.java)

    private lateinit var server: MockWebServer

    @Before
    fun setup() {
        server = MockWebServer()
        server.start()
        // TODO: configure app/networking to point image base URL to server.url("/")
    }

    @After
    fun teardown() {
        server.shutdown()
    }

    @Test
    fun image404_showsPlaceholder() {
        server.enqueue(MockResponse().setResponseCode(404))

        // TODO: Launch view that loads image from server and assert placeholder visible
        // This test is a skeleton to be expanded — implementing full Compose assertions requires wiring the image URL in tests
    }
}
