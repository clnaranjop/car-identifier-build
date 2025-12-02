package com.hotwheels.identifier.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.hotwheels.identifier.data.repository.HotWheelsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: HotWheelsRepository
    private val tag = "SettingsViewModel"

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    init {
        repository = HotWheelsRepository(application)
    }

    fun resetCollection() {
        viewModelScope.launch {
            try {
                Log.d(tag, "🗑️ Resetting collection...")
                repository.clearAllCollectionItems()
                _message.value = "Collection cleared successfully"
                Log.d(tag, "✅ Collection reset complete")
            } catch (e: Exception) {
                Log.e(tag, "❌ Error resetting collection", e)
                _message.value = "Failed to reset collection: ${e.message}"
            }
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
