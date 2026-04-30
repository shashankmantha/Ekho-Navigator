package com.ekhonavigator.core.model

import java.time.Instant

fun CalendarEvent.isPast(now: Instant = Instant.now()): Boolean = endTime <= now
