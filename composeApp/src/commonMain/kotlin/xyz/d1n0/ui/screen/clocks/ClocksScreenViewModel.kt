package xyz.d1n0.ui.screen.clocks

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import xyz.d1n0.lib.model.Watch
import xyz.d1n0.lib.model.requestClocks


class ClocksScreenViewModel: ViewModel(), KoinComponent {
    val watch: Watch by inject()
//
//    init {
//        if (watch.clocks.isInitialized.value == false)
//            requestClocks()
//    }

//    fun requestClocks() = watch.scope.launch { watch.requestClocks() }
    fun requestClocks() = watch.scope.launch { watch.requestClocks() }

//    fun syncTime() = watch.scope.launch { watch.adjustTime() }
}