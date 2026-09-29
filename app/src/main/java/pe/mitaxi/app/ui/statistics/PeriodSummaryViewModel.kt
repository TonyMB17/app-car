package pe.mitaxi.app.ui.statistics

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.mitaxi.app.data.repository.PeriodSummary
import pe.mitaxi.app.data.repository.PeriodSummaryRepository
import pe.mitaxi.app.domain.model.SummaryPeriod

data class PeriodSummaryState(val period: SummaryPeriod, val summary: PeriodSummary? = null,
    val loading: Boolean = true, val error: Boolean = false)

class PeriodSummaryViewModel(private val repository: PeriodSummaryRepository, private val saved: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(PeriodSummaryState(SummaryPeriod.valueOf(saved["period"] ?: "TODAY")))
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    fun select(period: SummaryPeriod) {
        saved["period"] = period.name
        mutableState.update { it.copy(period = period) }
        refresh()
    }
    fun refresh() {
        observation?.cancel()
        mutableState.update { it.copy(summary = null, loading = true, error = false) }
        observation = viewModelScope.launch {
            repository.observe(state.value.period).catch { error ->
                if (error is CancellationException) throw error
                Log.e("PeriodSummary", "Unable to load summary", error)
                mutableState.update { it.copy(loading = false, error = true) }
            }.collect { summary -> mutableState.update { it.copy(summary = summary, loading = false, error = false) } }
        }
    }
    fun stop() { observation?.cancel() }
    companion object {
        fun factory(repository: PeriodSummaryRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { PeriodSummaryViewModel(repository, createSavedStateHandle()) }
        }
    }
}
