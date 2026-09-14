package app.namaz.tr.v8.ui

import app.namaz.tr.v8.model.AppDestination
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FullNavigationContractTest {
    @Test
    fun quranLearnAndWorshipAreRealScreensNotPhasePlaceholders() {
        val root = File(System.getProperty("user.dir") ?: ".")
        val relative = "src/main/java/app/namaz/tr/v8/ui/NamazApp.kt"
        val candidates = listOf(
            File(root, relative),
            File(root, "app/$relative"),
            File(root, "namaz-v8/app/$relative"),
            root.parentFile?.let { File(it, "app/$relative") },
        ).filterNotNull()
        val source = candidates.firstOrNull { it.isFile }?.readText()
            ?: error("NamazApp.kt not found from ${root.absolutePath}")

        assertTrue(source.contains("QuranScreen()"))
        assertTrue(source.contains("LearnScreen()"))
        assertTrue(source.contains("WorshipScreen()"))
        assertFalse(source.contains("sonraki fazda"))
    }

    @Test
    fun rootTabsKeepExactlyTheDestinationTheUserSelected() {
        var state = RootTabState()

        AppDestination.entries.forEach { destination ->
            state = state.select(destination)
            assertEquals(destination, state.selected)
            assertNull(state.detail)
        }
    }

    @Test
    fun closingADetailScreenDoesNotResetTheRootTabToToday() {
        val quran = RootTabState().select(AppDestination.QURAN)
        val afterBack = quran.open(RootDetail.HEALTH).closeDetail()

        assertEquals(AppDestination.QURAN, afterBack.selected)
        assertNull(afterBack.detail)
    }
}
