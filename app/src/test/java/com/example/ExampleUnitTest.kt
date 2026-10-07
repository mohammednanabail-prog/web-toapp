package com.example

import com.example.model.AppBuildConfig
import com.example.model.BuildStep
import com.example.model.SampleTemplates
import com.example.model.SourceType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testAppBuildConfigDefaults() {
        val config = AppBuildConfig(
            appName = "WebToAPK",
            sourceType = SourceType.URL,
            url = "https://example.com"
        )
        assertEquals("WebToAPK", config.appName)
        assertEquals(SourceType.URL, config.sourceType)
        assertEquals("https://example.com", config.url)
        assertTrue(config.enableDownloads)
        assertTrue(config.enableShare)
        assertTrue(config.enablePrint)
        assertTrue(config.enableOfflineCache)
    }

    @Test
    fun testSampleTemplates() {
        assertNotNull(SampleTemplates.MINI_GAME)
        assertTrue(SampleTemplates.MINI_GAME.contains("<!DOCTYPE html>"))
        assertTrue(SampleTemplates.MINI_GAME.contains("Coin Clicker"))

        assertNotNull(SampleTemplates.BUSINESS_PORTFOLIO)
        assertTrue(SampleTemplates.BUSINESS_PORTFOLIO.contains("<!DOCTYPE html>"))
        assertTrue(SampleTemplates.BUSINESS_PORTFOLIO.contains("window.print()"))
    }

    @Test
    fun testBuildStepsOrder() {
        val steps = BuildStep.values()
        assertTrue(steps.isNotEmpty())
        assertEquals(BuildStep.INITIALIZING, steps.first())
        assertEquals(BuildStep.COMPLETED, steps.last())
    }
}
