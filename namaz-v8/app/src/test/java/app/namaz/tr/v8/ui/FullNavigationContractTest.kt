package app.namaz.tr.v8.ui

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FullNavigationContractTest {
    @Test
    fun quranLearnAndWorshipAreRealScreensNotPhasePlaceholders() {
        val root = File(System.getProperty("user.dir"))
        val candidates = listOf(
            File(root, "app/src/main/java/app/namaz/tr/v8/ui/NamazApp.kt"),
            File(root, "namaz-v8/app/src/main/java/app/namaz/tr/v8/ui/NamazApp.kt"),
        )
        val source = candidates.firstOrNull { it.isFile }?.readText()
            ?: error("NamazApp.kt not found from ${root.absolutePath}")

        assertTrue(source.contains("QuranScreen()"))
        assertTrue(source.contains("LearnScreen()"))
        assertTrue(source.contains("WorshipScreen()"))
        assertFalse(source.contains("sonraki fazda"))
    }
}
