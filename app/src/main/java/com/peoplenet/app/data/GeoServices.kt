package com.peoplenet.app.data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.provider.CalendarContract
import androidx.core.content.ContextCompat
import java.util.Calendar
import java.util.Locale
import kotlin.math.max
import kotlin.math.roundToLong

// ---------------- permissions ----------------

fun hasLocationPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

fun hasCalendarPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED

fun geoPermissionsGranted(context: Context): Boolean =
    hasLocationPermission(context) && hasCalendarPermission(context)

// ---------------- real location -> city ----------------

object GeoLocator {

    /** Best-effort current city from the device's last known location, reverse-geocoded. */
    fun currentCity(context: Context): String? {
        if (!hasLocationPermission(context)) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        val best = try {
            lm.allProviders.mapNotNull { p ->
                try {
                    @Suppress("MissingPermission") val l = lm.getLastKnownLocation(p)
                    l
                } catch (e: SecurityException) {
                    null
                }
            }.maxByOrNull { it.time }
        } catch (e: Exception) {
            null
        } ?: return null

        return try {
            @Suppress("DEPRECATION")
            val addrs = Geocoder(context, Locale.getDefault()).getFromLocation(best.latitude, best.longitude, 1)
            addrs?.firstOrNull()?.let { a ->
                // 合并多个行政级别字段（区/市/省），对反向地理编码的粒度抖动更稳。
                // 有意不含 countryName——只到国家级的粗定位返回 null，避免误判「离开城市」。
                listOfNotNull(a.locality, a.subLocality, a.subAdminArea, a.adminArea)
                    .distinct().joinToString(" ").ifBlank { null }
            }
        } catch (e: Exception) {
            null
        }
    }
}

// ---------------- real calendar -> upcoming trip ----------------

data class CalTrip(val dateLabel: String, val title: String, val location: String, val matchedToken: String)

object CalendarReader {

    /**
     * Scan the device calendar for the next upcoming event whose title or location contains
     * one of [cityTokens] (case-insensitive), i.e. an upcoming trip to a friend's city.
     */
    fun findUpcomingTrip(context: Context, cityTokens: List<String>, days: Int = 120): CalTrip? {
        if (!hasCalendarPermission(context) || cityTokens.isEmpty()) return null
        val now = System.currentTimeMillis()
        val until = now + days * 86_400_000L
        val proj = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.EVENT_LOCATION,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND,
            CalendarContract.Events.ALL_DAY
        )
        val sel = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? " +
            "AND (${CalendarContract.Events.DELETED} IS NULL OR ${CalendarContract.Events.DELETED} = 0)"
        val args = arrayOf(now.toString(), until.toString())
        val tokens = cityTokens.map { it.lowercase(Locale.getDefault()) }

        return try {
            context.contentResolver.query(
                CalendarContract.Events.CONTENT_URI, proj, sel, args,
                "${CalendarContract.Events.DTSTART} ASC"
            )?.use { c ->
                while (c.moveToNext()) {
                    val title = c.getString(0) ?: ""
                    val loc = c.getString(1) ?: ""
                    // 优先用「地点」字段判定目的地（更贴近真实目的地），标题只作兜底；
                    // 同一字段里出现多座城市时取「最后出现」的那座（从A飞B / A→B 的目的地约定），
                    // 避免出发地压过目的地。
                    val locHay = loc.lowercase(Locale.getDefault())
                    val titleHay = title.lowercase(Locale.getDefault())
                    val matched = lastMentioned(locHay, tokens) ?: lastMentioned(titleHay, tokens)
                    if (matched != null) {
                        val start = c.getLong(2)
                        val end = if (!c.isNull(3)) c.getLong(3) else start
                        return CalTrip(formatTrip(start, end), title, loc, matched)
                    }
                }
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    /** 在 [hay] 中出现的城市关键词里，取「最后出现」的那个（目的地约定：从A飞B 取 B）。 */
    private fun lastMentioned(hay: String, tokens: List<String>): String? =
        tokens.filter { it.isNotBlank() && hay.contains(it) }.maxByOrNull { hay.lastIndexOf(it) }

    private fun formatTrip(start: Long, end: Long): String {
        val cal = Calendar.getInstance().apply { timeInMillis = start }
        val month = cal.get(Calendar.MONTH) + 1
        val day = cal.get(Calendar.DAY_OF_MONTH)
        val spanMs = max(0L, end - start)
        val dayCount = max(1L, (spanMs.toDouble() / 86_400_000.0).roundToLong())
        return "${month}月${day}日 · $dayCount 天 · 来自日历"
    }
}
