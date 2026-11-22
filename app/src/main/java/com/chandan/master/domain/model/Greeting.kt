package com.chandan.master.domain.model

import java.time.LocalTime

enum class Greeting { MORNING, AFTERNOON, EVENING, NIGHT }

object GreetingResolver {
    /** 05:00–11:59 morning, 12:00–16:59 afternoon, 17:00–20:59 evening, otherwise night. */
    fun resolve(time: LocalTime): Greeting = when (time.hour) {
        in 5..11 -> Greeting.MORNING
        in 12..16 -> Greeting.AFTERNOON
        in 17..20 -> Greeting.EVENING
        else -> Greeting.NIGHT
    }
}
