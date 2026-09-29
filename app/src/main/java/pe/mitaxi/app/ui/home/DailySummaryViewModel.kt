package pe.mitaxi.app.ui.home

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
import pe.mitaxi.app.data.repository.DailySummaryRepository
import pe.mitaxi.app.domain.model.DailySummary
import pe.mitaxi.app.domain.model.ServiceInput

data class DailySummaryState(
    val summary: DailySummary? = null,
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val fuelOpen: Boolean = false,
    val amount: String = "",
    val invalidAmount: Boolean = false,
    val saveError: Boolean = false,
    val undoError: Boolean = false,
    val busy: Boolean = false,
    val lastFuelId: Long? = null,
)

class DailySummaryViewModel(private val repository: DailySummaryRepository, private val saved: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(DailySummaryState(
        fuelOpen = saved["fuelOpen"] ?: false, amount = saved["fuelAmount"] ?: "",
    ))
    val state = mutableState.asStateFlow()
    private var observation: Job? = null

    fun refresh() {
        observation?.cancel()
        mutableState.update { it.copy(loading = true, loadError = false) }
        observation = viewModelScope.launch {
            repository.observe().catch { error ->
                if (error is CancellationException) throw error
                Log.e("DailySummary", "Unable to load daily summary", error)
                mutableState.update { it.copy(loading = false, loadError = true) }
            }.collect { summary ->
                mutableState.update { it.copy(summary = summary, loading = false, loadError = false) }
            }
        }
    }

    fun stop() {
        observation?.cancel()
        // Once another screen can edit this expense, the original undo is stale.
        mutableState.update { it.copy(lastFuelId = null, undoError = false) }
    }

    fun openFuel() {
        if (state.value.busy) return
        saved["fuelOpen"] = true
        saved["fuelAmount"] = ""
        mutableState.update { it.copy(fuelOpen = true, amount = "", invalidAmount = false, saveError = false) }
    }

    fun changeAmount(value: String) {
        if (state.value.busy) return
        saved["fuelAmount"] = value
        mutableState.update { it.copy(amount = value, invalidAmount = false, saveError = false) }
    }

    fun closeFuel() {
        if (state.value.busy) return
        saved["fuelOpen"] = false
        mutableState.update { it.copy(fuelOpen = false) }
    }

    fun saveFuel() {
        if (state.value.busy || !state.value.fuelOpen) return
        val amount = ServiceInput.amountCents(state.value.amount)
        if (amount == null) { mutableState.update { it.copy(invalidAmount = true) }; return }
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                val id = repository.addFuel(amount)
                saved["fuelOpen"] = false
                saved["fuelAmount"] = ""
                mutableState.update { it.copy(fuelOpen = false, amount = "", lastFuelId = id, undoError = false) }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                Log.e("DailySummary", "Unable to save fuel", error)
                mutableState.update { it.copy(saveError = true) }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    fun undoFuel() {
        val id = state.value.lastFuelId ?: return
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                repository.undoFuel(id)
                mutableState.update { it.copy(lastFuelId = null, undoError = false) }
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                Log.e("DailySummary", "Unable to undo fuel", error)
                mutableState.update { it.copy(undoError = true) }
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    companion object {
        fun factory(repository: DailySummaryRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { DailySummaryViewModel(repository, createSavedStateHandle()) }
        }
    }
}
