package pe.mitaxi.app.ui.settings

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
import pe.mitaxi.app.data.local.entity.FareEntity
import pe.mitaxi.app.data.repository.FareRepository
import pe.mitaxi.app.domain.model.ServiceInput

data class FaresState(val fares: List<FareEntity> = emptyList(), val loading: Boolean = true,
    val loadError: Boolean = false, val busy: Boolean = false, val error: Boolean = false,
    val open: Boolean = false, val id: Long? = null, val amount: String = "",
    val invalid: Boolean = false, val duplicate: Boolean = false, val pendingDelete: FareEntity? = null)

class FaresViewModel(private val repository: FareRepository, private val saved: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow(FaresState(open = saved["open"] ?: false, id = saved["id"], amount = saved["amount"] ?: ""))
    val state = mutable.asStateFlow()
    private var observation: Job? = null
    init { reload() }
    fun reload() {
        observation?.cancel()
        mutable.update { it.copy(loading = true, loadError = false) }
        observation = viewModelScope.launch {
            repository.fares.catch { error ->
                if (error is CancellationException) throw error
                Log.e("Fares", "Unable to load fares", error)
                mutable.update { it.copy(loading = false, loadError = true) }
            }.collect { rows -> mutable.update { it.copy(fares = rows, loading = false, loadError = false) } }
        }
    }
    fun edit(fare: FareEntity? = null) {
        if (state.value.busy) return
        saved["open"] = true; saved["id"] = fare?.id
        saved["amount"] = fare?.let { ServiceInput.editableAmount(it.amountCents) } ?: ""
        mutable.update { it.copy(open = true, id = fare?.id, amount = saved["amount"] ?: "", invalid = false, duplicate = false, error = false) }
    }
    fun amount(value: String) {
        if (state.value.busy) return
        saved["amount"] = value
        mutable.update { it.copy(amount = value, invalid = false, duplicate = false, error = false) }
    }
    fun close() { if (!state.value.busy) dismissEditor() }
    private fun dismissEditor() { saved["open"] = false; mutable.update { it.copy(open = false) } }
    fun save() {
        val current = state.value
        if (current.busy || !current.open) return
        val amount = ServiceInput.amountCents(current.amount)
        if (amount == null) { mutable.update { it.copy(invalid = true) }; return }
        if (current.fares.any { it.id != current.id && it.amountCents == amount }) {
            mutable.update { it.copy(duplicate = true) }; return
        }
        change { repository.save(current.id, amount); dismissEditor() }
    }
    fun requestDelete(fare: FareEntity) { if (!state.value.busy) mutable.update { it.copy(pendingDelete = fare, error = false) } }
    fun cancelDelete() { if (!state.value.busy) mutable.update { it.copy(pendingDelete = null) } }
    fun delete() {
        val fare = state.value.pendingDelete ?: return
        change { repository.delete(fare.id); mutable.update { it.copy(pendingDelete = null) } }
    }
    fun move(id: Long, direction: Int) = change { repository.move(id, direction) }
    private fun change(action: suspend () -> Unit) {
        if (state.value.busy) return
        mutable.update { it.copy(busy = true, error = false) }
        viewModelScope.launch {
            try { action() }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { Log.e("Fares", "Unable to update fares", error); mutable.update { it.copy(error = true) } }
            finally { mutable.update { it.copy(busy = false) } }
        }
    }
    companion object {
        fun factory(repository: FareRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { FaresViewModel(repository, createSavedStateHandle()) }
        }
    }
}
