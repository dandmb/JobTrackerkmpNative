package com.dmb.joblog

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class GreetingTest {

    @Test
    fun sayHello_givenName_returnsGreetingWithThatName() {
        assertEquals("Hello, Android!", sayHello("Android"))
    }

    @Test
    fun sayHello_emptyName_stillReturnsWellFormedGreeting() {
        assertEquals("Hello, !", sayHello(""))
    }

    @Test
    fun greet_usesThePlatformName() {
        val greeting = Greeting().greet()

        assertEquals("Hello, ${getPlatform().name}!", greeting)
        assertTrue(greeting.startsWith("Hello, "))
    }

    @Test
    fun getPlatform_returnsAPlatformWithANonEmptyName() {
        assertTrue(getPlatform().name.isNotEmpty())
    }
}
