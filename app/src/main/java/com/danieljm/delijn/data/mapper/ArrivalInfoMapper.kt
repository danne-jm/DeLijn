package com.danieljm.delijn.data.mapper

import com.danieljm.delijn.data.remote.dto.RealTimeDoorkomstDto
import com.danieljm.delijn.data.remote.dto.ScheduledDoorkomstDto
import com.danieljm.delijn.domain.model.ArrivalInfo
import com.danieljm.delijn.domain.model.ServedLine
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object ArrivalInfoMapper {
    private val isoFormatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun fromRealTimeDto(dto: RealTimeDoorkomstDto, servedLines: List<ServedLine>): ArrivalInfo? {
        val lineId = dto.lijnnummer
        val destination = dto.bestemming.ifBlank { "-" }
        val realTimeStr = dto.realTimeTijdstip
        val scheduledTimeStr = dto.dienstregelingTijdstip ?: realTimeStr

        if (realTimeStr.isNullOrBlank() || lineId.isBlank() || scheduledTimeStr.isNullOrBlank()) return null

        val realTime = try { LocalDateTime.parse(realTimeStr, isoFormatter) } catch (e: Exception) { return null }
        val scheduledTime = try { LocalDateTime.parse(scheduledTimeStr, isoFormatter) } catch (e: Exception) { realTime }

        val now = LocalDateTime.now()
        var remaining = Duration.between(now, realTime).toMinutes()
        if (remaining < 0) remaining = 0

        val omschrijving = servedLines.find { it.lineId == lineId }?.omschrijving ?: ""

        return ArrivalInfo(
            lineId = lineId,
            destination = destination,
            scheduledTime = scheduledTime.format(timeFormatter),
            time = realTime.format(timeFormatter),
            remainingMinutes = remaining,
            omschrijving = omschrijving,
            expectedArrivalTime = try { scheduledTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() } catch (e: Exception) { 0L },
            realArrivalTime = try { realTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() } catch (e: Exception) { 0L },
            isScheduleOnly = false
        )
    }

    fun fromScheduledDto(dto: ScheduledDoorkomstDto, servedLines: List<ServedLine>): ArrivalInfo? {
        val lineId = dto.lijnnummer
        val destination = dto.bestemming.ifBlank { "-" }
        val scheduledTimeStr = dto.dienstregelingTijdstip
        if (lineId.isBlank() || scheduledTimeStr.isBlank()) return null
        val scheduledTime = try { LocalDateTime.parse(scheduledTimeStr, isoFormatter) } catch (e: Exception) { return null }
        val now = LocalDateTime.now()
        val remaining = Duration.between(now, scheduledTime).toMinutes().coerceAtLeast(0)
        val omschrijving = servedLines.find { it.lineId == lineId }?.omschrijving ?: ""
        return ArrivalInfo(
            lineId = lineId,
            destination = destination,
            scheduledTime = scheduledTime.format(timeFormatter),
            time = scheduledTime.format(timeFormatter),
            remainingMinutes = remaining,
            omschrijving = omschrijving,
            expectedArrivalTime = try { scheduledTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() } catch (e: Exception) { 0L },
            realArrivalTime = try { scheduledTime.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli() } catch (e: Exception) { 0L },
            isScheduleOnly = true
        )
    }
}
