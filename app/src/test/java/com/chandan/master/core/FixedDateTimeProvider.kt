package com.chandan.master.core

import com.chandan.master.core.time.DateTimeProvider
import java.time.Instant
import java.time.ZoneId

class FixedDateTimeProvider(var instant: Instant, var zoneId: ZoneId) : DateTimeProvider {
    override fun now(): Instant = instant
    override fun zone(): ZoneId = zoneId
}
