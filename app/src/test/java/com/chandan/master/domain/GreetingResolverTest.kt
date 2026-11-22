package com.chandan.master.domain

import com.chandan.master.domain.model.Greeting
import com.chandan.master.domain.model.GreetingResolver
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class GreetingResolverTest {
    private fun at(h: Int, m: Int = 0) = GreetingResolver.resolve(LocalTime.of(h, m))

    @Test fun boundaries() {
        assertEquals(Greeting.NIGHT, at(4, 59))
        assertEquals(Greeting.MORNING, at(5, 0))
        assertEquals(Greeting.MORNING, at(11, 59))
        assertEquals(Greeting.AFTERNOON, at(12, 0))
        assertEquals(Greeting.AFTERNOON, at(16, 59))
        assertEquals(Greeting.EVENING, at(17, 0))
        assertEquals(Greeting.EVENING, at(20, 59))
        assertEquals(Greeting.NIGHT, at(21, 0))
        assertEquals(Greeting.NIGHT, at(0, 0))
        assertEquals(Greeting.NIGHT, at(23, 59))
    }

    @Test fun everyMinuteOfTheDayResolves() {
        for (h in 0..23) for (m in 0..59) GreetingResolver.resolve(LocalTime.of(h, m))
    }
}
