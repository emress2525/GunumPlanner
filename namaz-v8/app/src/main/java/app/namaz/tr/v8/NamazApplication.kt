package app.namaz.tr.v8

import android.app.Application

class NamazApplication : Application() {
    val graph: AppGraph by lazy { AppGraph(this) }
}
