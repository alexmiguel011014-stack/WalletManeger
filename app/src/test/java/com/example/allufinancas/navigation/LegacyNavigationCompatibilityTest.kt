package com.example.allufinancas.navigation

import org.junit.Assert.assertEquals
import org.junit.Test

class LegacyNavigationCompatibilityTest {
  @Test
  fun restoredAssistantRouteSelectsHome() {
    assertEquals(Page.HOME, selectedTabForRoute(Page.ASSISTANT.route, Page.ASSISTANT))
  }
}
