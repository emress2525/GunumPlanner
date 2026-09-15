package app.namaz.tr.v8.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.icu.util.IslamicCalendar
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import app.namaz.tr.v8.MainActivity
import app.namaz.tr.v8.R
import app.namaz.tr.v8.today.TodayUiState

data class WidgetPrayerSnapshot(
    val city: String,
    val method: String,
    val nextName: String,
    val nextTime: String,
    val remaining: String,
    val prayerLines: String,
    val completedCount: Int,
    val hijri: String,
    val quranResume: String,
)

data class WidgetAppearance(
    val textScale: Float = 1f,
    val showFooter: Boolean = true,
)

object WidgetAppearanceStore {
    private const val PREFS = "widget_appearance_v8"
    fun load(context: Context): WidgetAppearance {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return WidgetAppearance(
            textScale = p.getFloat("text_scale", 1f).coerceIn(0.85f, 1.4f),
            showFooter = p.getBoolean("show_footer", true),
        )
    }
    fun save(context: Context, appearance: WidgetAppearance) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putFloat("text_scale", appearance.textScale.coerceIn(0.85f, 1.4f))
            .putBoolean("show_footer", appearance.showFooter)
            .apply()
        WidgetPrayerSnapshotStore.updateAll(context)
    }
}

object WidgetPrayerSnapshotStore {
    private const val PREFS = "widget_snapshot_v8"

    fun save(context: Context, state: TodayUiState) {
        val next = state.nextPrayer
        val quranHistory = context.getSharedPreferences("quran_progress_v8", Context.MODE_PRIVATE)
            .getString("history", "").orEmpty().split('|').firstOrNull { it.isNotBlank() } ?: "Henüz başlanmadı"
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString("city", state.city)
            .putString("method", state.methodLabel)
            .putString("nextName", next?.name ?: "Bugün tamamlandı")
            .putString("nextTime", next?.timeText.orEmpty())
            .putString("remaining", next?.remainingText ?: "—")
            .putString("prayerLines", state.trackablePrayers.joinToString("  •  ") { "${it.title} ${it.timeText}" })
            .putInt("completedCount", state.completed.size)
            .putString("hijri", hijriToday())
            .putString("quranResume", quranHistory)
            .apply()
        updateAll(context)
    }

    fun load(context: Context): WidgetPrayerSnapshot {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        return WidgetPrayerSnapshot(
            city = p.getString("city", "Namaz V8").orEmpty(),
            method = p.getString("method", "Vakitler uygulama açılınca güncellenir").orEmpty(),
            nextName = p.getString("nextName", "Vakit hazırlanıyor").orEmpty(),
            nextTime = p.getString("nextTime", "").orEmpty(),
            remaining = p.getString("remaining", "—").orEmpty(),
            prayerLines = p.getString("prayerLines", "Uygulamayı açarak vakitleri hazırla").orEmpty(),
            completedCount = p.getInt("completedCount", 0),
            hijri = p.getString("hijri", hijriToday()).orEmpty(),
            quranResume = p.getString("quranResume", "Henüz başlanmadı").orEmpty(),
        )
    }

    fun updateAll(context: Context) {
        val manager = AppWidgetManager.getInstance(context)
        providerClasses.forEach { cls ->
            val component = ComponentName(context, cls)
            val ids = manager.getAppWidgetIds(component)
            if (ids.isNotEmpty()) {
                context.sendBroadcast(Intent(context, cls).apply {
                    action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
                })
            }
        }
    }

    private fun hijriToday(): String {
        val c = IslamicCalendar()
        val months = listOf("Muharrem", "Safer", "Rebiülevvel", "Rebiülahir", "Cemaziyelevvel", "Cemaziyelahir", "Recep", "Şaban", "Ramazan", "Şevval", "Zilkade", "Zilhicce")
        return "${c.get(IslamicCalendar.DAY_OF_MONTH)} ${months.getOrElse(c.get(IslamicCalendar.MONTH)) { "" }} ${c.get(IslamicCalendar.YEAR)}"
    }

    val providerClasses: List<Class<out AppWidgetProvider>> = listOf(
        NextPrayerWidgetProvider::class.java,
        CountdownWidgetProvider::class.java,
        PrayerTimesWidgetProvider::class.java,
        PrayerTrackerWidgetProvider::class.java,
        DailyAyahWidgetProvider::class.java,
        HijriDateWidgetProvider::class.java,
        QuranResumeWidgetProvider::class.java,
    )
}

enum class WidgetKind { NEXT, COUNTDOWN, TIMES, TRACKER, AYAH, HIJRI, QURAN_RESUME }

abstract class NamazWidgetProvider(private val kind: WidgetKind) : AppWidgetProvider() {
    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        ids.forEach { id -> manager.updateAppWidget(id, views(context)) }
    }

    private fun views(context: Context): RemoteViews {
        val s = WidgetPrayerSnapshotStore.load(context)
        val appearance = WidgetAppearanceStore.load(context)
        val v = RemoteViews(context.packageName, R.layout.widget_card)
        val text = when (kind) {
            WidgetKind.NEXT -> arrayOf("Sıradaki Namaz", "${s.nextName}  ${s.nextTime}", "${s.remaining} kaldı", s.city)
            WidgetKind.COUNTDOWN -> arrayOf("Büyük Geri Sayım", s.remaining, "${s.nextName} • ${s.nextTime}", s.city)
            WidgetKind.TIMES -> arrayOf("Bugünün Vakitleri", s.prayerLines, s.hijri, s.city)
            WidgetKind.TRACKER -> arrayOf("Namaz Takibi", "${s.completedCount} / 5", "Bugünkü farz takibi", "Uygulamadan işaretle")
            WidgetKind.AYAH -> arrayOf("Günün Ayeti", dailyReference(), "Meal ve tefsir için dokun", "Kur’an Pro")
            WidgetKind.HIJRI -> arrayOf("Hicrî Tarih", s.hijri, s.city, "Takvim hesabı resmî ilana göre ±1 gün değişebilir")
            WidgetKind.QURAN_RESUME -> arrayOf("Kur’an’da Kaldığın Yer", s.quranResume, "Okumaya devam etmek için dokun", "Namaz V8")
        }
        v.setTextViewText(R.id.widget_title, text[0])
        v.setTextViewText(R.id.widget_primary, text[1])
        v.setTextViewText(R.id.widget_secondary, text[2])
        v.setTextViewText(R.id.widget_footer, text[3])
        v.setTextViewTextSize(R.id.widget_title, TypedValue.COMPLEX_UNIT_SP, 13f * appearance.textScale)
        v.setTextViewTextSize(R.id.widget_primary, TypedValue.COMPLEX_UNIT_SP, 20f * appearance.textScale)
        v.setTextViewTextSize(R.id.widget_secondary, TypedValue.COMPLEX_UNIT_SP, 12f * appearance.textScale)
        v.setTextViewTextSize(R.id.widget_footer, TypedValue.COMPLEX_UNIT_SP, 10f * appearance.textScale)
        v.setViewVisibility(R.id.widget_footer, if (appearance.showFooter) View.VISIBLE else View.GONE)
        val intent = Intent(context, MainActivity::class.java)
        val pending = PendingIntent.getActivity(context, kind.ordinal + 700, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        v.setOnClickPendingIntent(R.id.widget_root, pending)
        return v
    }

    private fun dailyReference(): String {
        val refs = listOf("Bakara 2:286", "Ra'd 13:28", "İnşirah 94:5-6", "Zümer 39:53", "Tâhâ 20:114", "Hucurât 49:13", "İsrâ 17:24")
        return refs[java.time.LocalDate.now().dayOfYear % refs.size]
    }
}

class NextPrayerWidgetProvider : NamazWidgetProvider(WidgetKind.NEXT)
class CountdownWidgetProvider : NamazWidgetProvider(WidgetKind.COUNTDOWN)
class PrayerTimesWidgetProvider : NamazWidgetProvider(WidgetKind.TIMES)
class PrayerTrackerWidgetProvider : NamazWidgetProvider(WidgetKind.TRACKER)
class DailyAyahWidgetProvider : NamazWidgetProvider(WidgetKind.AYAH)
class HijriDateWidgetProvider : NamazWidgetProvider(WidgetKind.HIJRI)
class QuranResumeWidgetProvider : NamazWidgetProvider(WidgetKind.QURAN_RESUME)
