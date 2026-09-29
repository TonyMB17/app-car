package pe.mitaxi.app.ui.home

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pe.mitaxi.app.data.repository.LocalStorageRepository

enum class StorageState { Loading, Ready, Error }

class HomeViewModel(private val repository: LocalStorageRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(StorageState.Loading)
    val state = mutableState.asStateFlow()

    init { verifyStorage() }

    fun verifyStorage() {
        mutableState.value = StorageState.Loading
        viewModelScope.launch {
            try {
                repository.verifyAccess()
                mutableState.value = StorageState.Ready
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Log.e("HomeViewModel", "Unable to open local storage", error)
                mutableState.value = StorageState.Error
            }
        }
    }

    companion object {
        fun factory(repository: LocalStorageRepository): ViewModelProvider.Factory = viewModelFactory {
            initializer { HomeViewModel(repository) }
        }
    }
}
