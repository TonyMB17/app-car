package pe.mitaxi.app.ui.services

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import java.math.BigDecimal
import java.time.ZoneId
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pe.mitaxi.app.data.repository.ServiceRepository
import pe.mitaxi.app.domain.model.ServiceInput
import pe.mitaxi.app.domain.model.TaxiService

enum class ServiceNotice { Added, Updated, Deleted, DeleteFailed, QuickFailed, Undone }

data class RegisteredService(val id: Long, val amountCents: Long)

data class ServiceEditor(
    val id: Long? = null,
    val originalOccurredAt: Long? = null,
    val zone: String = ZoneId.systemDefault().id,
    val amount: String = "",
    val note: String = "",
    val date: String = "",
    val time: String = "",
    val amountError: Boolean = false,
    val dateTimeError: Boolean = false,
    val saveError: Boolean = false,
)

data class ServicesState(
    val fares: List<pe.mitaxi.app.data.local.entity.FareEntity> = emptyList(),
    val faresLoading: Boolean = true,
    val faresError: Boolean = false,
    val services: List<TaxiService> = emptyList(),
    val total: BigDecimal = BigDecimal.ZERO,
    val loading: Boolean = true,
    val loadError: Boolean = false,
    val editor: ServiceEditor? = null,
    val pendingDelete: TaxiService? = null,
    val busy: Boolean = false,
    val quickLocked: Boolean = false,
    val notice: ServiceNotice? = null,
    val noticeVersion: Long = 0,
    val lastRegistered: RegisteredService? = null,
)

class ServicesViewModel(
    private val repository: ServiceRepository,
    private val savedState: SavedStateHandle,
    private val fareRepository: pe.mitaxi.app.data.repository.FareRepository,
) : ViewModel() {
    private val mutableState = MutableStateFlow(ServicesState(editor = restoreEditor()))
    val state = mutableState.asStateFlow()
    private var observeJob: Job? = null

    private var faresJob: Job? = null
    init { reload(); reloadFares() }

    fun reloadFares() {
        faresJob?.cancel()
        mutableState.update { it.copy(faresLoading = true, faresError = false) }
        faresJob = viewModelScope.launch {
            fareRepository.fares.catch { error ->
                if (error is CancellationException) throw error
                Log.e("ServicesViewModel", "Unable to load fares", error)
                mutableState.update { it.copy(faresLoading = false, faresError = true, fares = emptyList()) }
            }.collect { rows -> mutableState.update { it.copy(fares = rows, faresLoading = false, faresError = false) } }
        }
    }

    fun reload() {
        observeJob?.cancel()
        mutableState.update { it.copy(loading = true, loadError = false) }
        observeJob = viewModelScope.launch {
            repository.services.catch { error ->
                if (error is CancellationException) throw error
                Log.e("ServicesViewModel", "Unable to load services", error)
                mutableState.update { it.copy(loading = false, loadError = true) }
            }.collect { services ->
                val total = services.fold(BigDecimal.ZERO) { sum, service ->
                    sum + BigDecimal.valueOf(service.amountCents, 2)
                }
                mutableState.update { it.copy(services = services, total = total, loading = false, loadError = false) }
            }
        }
    }

    fun add() { if (!state.value.busy) setEditor(ServiceEditor()) }

    fun registerQuick(amountCents: Long) {
        val fare = state.value.fares.firstOrNull { it.amountCents == amountCents } ?: return
        if (state.value.busy || state.value.quickLocked || state.value.faresLoading || state.value.faresError) return
        mutableState.update { it.copy(busy = true, quickLocked = true) }
        viewModelScope.launch {
            try {
                val id = fareRepository.register(fare.id, amountCents)
                mutableState.update { it.copy(lastRegistered = RegisteredService(id, amountCents)) }
                notify(ServiceNotice.Added)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("ServicesViewModel", "Unable to register quick service", error)
                notify(ServiceNotice.QuickFailed)
            } finally {
                // Also absorb an accidental double tap after a fast SQLite write.
                mutableState.update { it.copy(busy = false) }
                try { delay(600) } finally { mutableState.update { it.copy(quickLocked = false) } }
            }
        }
    }

    fun undoRegistration(id: Long) {
        if (state.value.busy || state.value.lastRegistered?.id != id) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                repository.delete(id)
                mutableState.update { it.copy(lastRegistered = null) }
                notify(ServiceNotice.Undone)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("ServicesViewModel", "Unable to undo registration", error)
                notify(ServiceNotice.DeleteFailed)
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }

    fun edit(service: TaxiService) {
        if (state.value.busy) return
        val zone = ZoneId.systemDefault()
        setEditor(ServiceEditor(
            id = service.id,
            originalOccurredAt = service.occurredAt,
            zone = zone.id,
            amount = ServiceInput.editableAmount(service.amountCents),
            note = service.note.orEmpty(),
            date = ServiceInput.date(service.occurredAt, zone),
            time = ServiceInput.time(service.occurredAt, zone),
        ))
    }

    fun changeAmount(value: String) = changeEditor { copy(amount = value, amountError = false, saveError = false) }
    fun changeNote(value: String) = changeEditor { copy(note = value, saveError = false) }
    fun changeDate(value: String) = changeEditor { copy(date = value, dateTimeError = false, saveError = false) }
    fun changeTime(value: String) = changeEditor { copy(time = value, dateTimeError = false, saveError = false) }
    fun closeEditor() { if (!state.value.busy) setEditor(null) }

    private fun changeEditor(change: ServiceEditor.() -> ServiceEditor) {
        if (!state.value.busy) state.value.editor?.let { setEditor(it.change()) }
    }

    fun save() {
        val current = state.value
        val editor = current.editor ?: return
        if (current.busy) return
        val amount = ServiceInput.amountCents(editor.amount)
        val zone = ZoneId.of(editor.zone)
        val original = editor.originalOccurredAt
        val occurredAt = if (editor.id == null) null else if (original != null &&
            editor.date == ServiceInput.date(original, zone) && editor.time == ServiceInput.time(original, zone)
        ) original else ServiceInput.occurredAt(editor.date, editor.time, zone)
        if (amount == null || (editor.id != null && occurredAt == null)) {
            setEditor(editor.copy(amountError = amount == null, dateTimeError = editor.id != null && occurredAt == null))
            return
        }
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                if (editor.id == null) {
                    val id = repository.add(amount, "")
                    mutableState.update { it.copy(lastRegistered = RegisteredService(id, amount)) }
                }
                else repository.update(editor.id, amount, editor.note, checkNotNull(occurredAt))
                setEditor(null)
                notify(if (editor.id == null) ServiceNotice.Added else ServiceNotice.Updated)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("ServicesViewModel", "Unable to save service", error)
                setEditor(editor.copy(saveError = true))
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    fun requestDelete(service: TaxiService) {
        if (!state.value.busy) mutableState.update { it.copy(pendingDelete = service) }
    }

    fun cancelDelete() {
        if (!state.value.busy) mutableState.update { it.copy(pendingDelete = null) }
    }

    fun confirmDelete() {
        val service = state.value.pendingDelete ?: return
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                repository.delete(service.id)
                mutableState.update { it.copy(pendingDelete = null) }
                notify(ServiceNotice.Deleted)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("ServicesViewModel", "Unable to delete service", error)
                mutableState.update { it.copy(pendingDelete = null) }
                notify(ServiceNotice.DeleteFailed)
            } finally {
                mutableState.update { it.copy(busy = false) }
            }
        }
    }

    private fun notify(notice: ServiceNotice) {
        mutableState.update { it.copy(notice = notice, noticeVersion = it.noticeVersion + 1) }
    }

    fun clearNotice(version: Long) {
        mutableState.update { if (it.noticeVersion == version) it.copy(notice = null) else it }
    }

    private fun setEditor(editor: ServiceEditor?) {
        savedState["editorOpen"] = editor != null
        savedState["editorId"] = editor?.id
        savedState["editorOriginal"] = editor?.originalOccurredAt
        savedState["editorZone"] = editor?.zone
        savedState["editorAmount"] = editor?.amount
        savedState["editorNote"] = editor?.note
        savedState["editorDate"] = editor?.date
        savedState["editorTime"] = editor?.time
        mutableState.update { it.copy(editor = editor) }
    }

    private fun restoreEditor(): ServiceEditor? = if (savedState.get<Boolean>("editorOpen") == true) ServiceEditor(
        id = savedState["editorId"], originalOccurredAt = savedState["editorOriginal"],
        zone = savedState["editorZone"] ?: ZoneId.systemDefault().id,
        amount = savedState["editorAmount"] ?: "", note = savedState["editorNote"] ?: "",
        date = savedState["editorDate"] ?: "", time = savedState["editorTime"] ?: "",
    ) else null

    companion object {
        fun factory(repository: ServiceRepository, fares: pe.mitaxi.app.data.repository.FareRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ServicesViewModel(repository, createSavedStateHandle(), fares) }
        }
    }
}
