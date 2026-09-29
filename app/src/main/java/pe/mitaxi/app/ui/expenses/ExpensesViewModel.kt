package pe.mitaxi.app.ui.expenses

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
import pe.mitaxi.app.data.repository.ExpenseRepository
import pe.mitaxi.app.domain.model.Expense
import pe.mitaxi.app.domain.model.ExpenseCategory
import pe.mitaxi.app.domain.model.ServiceInput

data class ExpenseEditor(
    val id: Long? = null, val category: ExpenseCategory = ExpenseCategory.FUEL,
    val amount: String = "", val originalTime: Long? = null,
    val zone: String = ZoneId.systemDefault().id, val date: String = "", val time: String = "",
    val invalidAmount: Boolean = false, val invalidDate: Boolean = false, val saveError: Boolean = false,
    val quick: Boolean = false,
)

enum class ExpenseNotice { Saved, Deleted, DeleteFailed }

data class ExpensesState(
    val expenses: List<Expense> = emptyList(), val total: BigDecimal = BigDecimal.ZERO,
    val loading: Boolean = true, val loadError: Boolean = false, val busy: Boolean = false,
    val editor: ExpenseEditor? = null, val pendingDelete: Expense? = null,
    val notice: ExpenseNotice? = null, val noticeVersion: Long = 0,
)

class ExpensesViewModel(private val repository: ExpenseRepository, private val saved: SavedStateHandle) : ViewModel() {
    private val mutableState = MutableStateFlow(ExpensesState(editor = restoreEditor()))
    val state = mutableState.asStateFlow()
    private var observation: Job? = null
    init { reload() }

    fun reload() {
        observation?.cancel()
        mutableState.update { it.copy(loading = true, loadError = false) }
        observation = viewModelScope.launch {
            repository.expenses.catch { error ->
                if (error is CancellationException) throw error
                Log.e("Expenses", "Unable to load expenses", error)
                mutableState.update { it.copy(loading = false, loadError = true) }
            }.collect { rows ->
                val total = rows.fold(BigDecimal.ZERO) { sum, row -> sum + BigDecimal.valueOf(row.amountCents, 2) }
                mutableState.update { it.copy(expenses = rows, total = total, loading = false, loadError = false) }
            }
        }
    }

    fun add(category: ExpenseCategory) { if (!state.value.busy) setEditor(ExpenseEditor(category = category)) }
    fun quickAdd(category: ExpenseCategory, cents: Long) {
        if (state.value.busy || state.value.editor != null) return
        require((category == ExpenseCategory.FUEL && cents in listOf(5000L, 10000L)) ||
            (category == ExpenseCategory.WASH && cents == 1000L))
        setEditor(ExpenseEditor(category = category, amount = ServiceInput.editableAmount(cents), quick = true))
        save()
    }
    fun edit(expense: Expense) {
        if (state.value.busy) return
        val zone = ZoneId.systemDefault()
        setEditor(ExpenseEditor(id = expense.id, category = expense.category,
            amount = ServiceInput.editableAmount(expense.amountCents), originalTime = expense.occurredAt,
            zone = zone.id, date = ServiceInput.date(expense.occurredAt, zone), time = ServiceInput.time(expense.occurredAt, zone)))
    }
    private fun change(block: ExpenseEditor.() -> ExpenseEditor) {
        if (!state.value.busy) state.value.editor?.let { setEditor(it.block()) }
    }
    fun amount(value: String) = change { copy(amount = value, invalidAmount = false, saveError = false) }
    fun category(value: ExpenseCategory) = change { copy(category = value, saveError = false) }
    fun date(value: String) = change { copy(date = value, invalidDate = false, saveError = false) }
    fun time(value: String) = change { copy(time = value, invalidDate = false, saveError = false) }
    fun close() { if (!state.value.busy) setEditor(null) }

    fun save() {
        val editor = state.value.editor ?: return
        if (state.value.busy) return
        val cents = ServiceInput.amountCents(editor.amount)
        val zone = ZoneId.of(editor.zone)
        val original = editor.originalTime
        val timestamp = if (editor.id == null) null else if (original != null &&
            editor.date == ServiceInput.date(original, zone) && editor.time == ServiceInput.time(original, zone)) original
            else ServiceInput.occurredAt(editor.date, editor.time, zone)
        if (cents == null || (editor.id != null && timestamp == null)) {
            setEditor(editor.copy(invalidAmount = cents == null, invalidDate = editor.id != null && timestamp == null))
            return
        }
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                if (editor.id == null) repository.add(editor.category, cents)
                else repository.update(editor.id, editor.category, cents, checkNotNull(timestamp))
                setEditor(null)
                notice(ExpenseNotice.Saved)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                Log.e("Expenses", "Unable to save expense", error)
                setEditor(editor.copy(saveError = true))
            } finally { if (editor.quick) delay(600); mutableState.update { it.copy(busy = false) } }
        }
    }

    fun requestDelete(expense: Expense) { if (!state.value.busy) mutableState.update { it.copy(pendingDelete = expense) } }
    fun cancelDelete() { if (!state.value.busy) mutableState.update { it.copy(pendingDelete = null) } }
    fun delete() {
        val expense = state.value.pendingDelete ?: return
        if (state.value.busy) return
        mutableState.update { it.copy(busy = true) }
        viewModelScope.launch {
            try {
                repository.delete(expense.id)
                mutableState.update { it.copy(pendingDelete = null) }
                notice(ExpenseNotice.Deleted)
            } catch (cancelled: CancellationException) { throw cancelled
            } catch (error: Exception) {
                Log.e("Expenses", "Unable to delete expense", error)
                mutableState.update { it.copy(pendingDelete = null) }
                notice(ExpenseNotice.DeleteFailed)
            } finally { mutableState.update { it.copy(busy = false) } }
        }
    }
    private fun notice(notice: ExpenseNotice) { mutableState.update { it.copy(notice = notice, noticeVersion = it.noticeVersion + 1) } }
    fun clearNotice(version: Long) { mutableState.update { if (it.noticeVersion == version) it.copy(notice = null) else it } }

    private fun setEditor(editor: ExpenseEditor?) {
        saved["open"] = editor != null
        saved["id"] = editor?.id
        saved["category"] = editor?.category?.name
        saved["amount"] = editor?.amount
        saved["original"] = editor?.originalTime
        saved["zone"] = editor?.zone
        saved["date"] = editor?.date
        saved["time"] = editor?.time
        mutableState.update { it.copy(editor = editor) }
    }
    private fun restoreEditor(): ExpenseEditor? = if (saved.get<Boolean>("open") == true) ExpenseEditor(
        id = saved["id"], category = ExpenseCategory.valueOf(saved["category"] ?: "FUEL"), amount = saved["amount"] ?: "",
        originalTime = saved["original"], zone = saved["zone"] ?: ZoneId.systemDefault().id,
        date = saved["date"] ?: "", time = saved["time"] ?: "",
    ) else null
    companion object {
        fun factory(repository: ExpenseRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { ExpensesViewModel(repository, createSavedStateHandle()) }
        }
    }
}
