package com.example

import com.example.model.AppBuildConfig
import com.example.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleRobolectricTest {

    @Test
    fun testAppBuildConfig() {
        val config = AppBuildConfig(
            appName = "WebToAPK",
            sourceType = SourceType.URL,
            url = "https://example.com"
        )
        assertEquals("WebToAPK", config.appName)
        assertEquals(SourceType.URL, config.sourceType)
        assertTrue(config.enableDownloads)
        assertTrue(config.enableShare)
        assertTrue(config.enablePrint)
    }

    @Test
    fun testLocalHtmlConfig() {
        val config = AppBuildConfig(
            appName = "LocalApp",
            sourceType = SourceType.HTML_CODE,
            htmlCode = "<h1>Hello</h1>"
        )
        assertEquals(SourceType.HTML_CODE, config.sourceType)
        assertEquals("<h1>Hello</h1>", config.htmlCode)
    }
}
