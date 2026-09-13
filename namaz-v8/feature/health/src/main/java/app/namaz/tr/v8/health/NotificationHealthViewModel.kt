package app.namaz.tr.v8.health

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class NotificationHealthViewModel(
    private val inspector: NotificationHealthInspector,
) {
    private val _items = MutableStateFlow<List<HealthItem>>(emptyList())
    val items: StateFlow<List<HealthItem>> = _items.asStateFlow()

    fun refresh() {
        _items.value = inspector.inspect()
    }
}
