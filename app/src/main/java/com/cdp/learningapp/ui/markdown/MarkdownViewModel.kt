package com.cdp.learningapp.ui.markdown

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cdp.learningapp.CDPApplication
import com.cdp.learningapp.data.repository.ContentRepository
import com.cdp.learningapp.domain.model.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MarkdownUiState(
    val contentResource: Resource<String> = Resource.Loading(),
    val isSavedOffline: Boolean = false,
    val fontSizeSp: Float = 16f,
    val notePath: String = "",
    val noteTitle: String = ""
)

class MarkdownViewModel(
    private val repository: ContentRepository = CDPApplication.instance.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(MarkdownUiState())
    val uiState: StateFlow<MarkdownUiState> = _uiState.asStateFlow()

    fun init(path: String, title: String) {
        if (_uiState.value.notePath == path && _uiState.value.contentResource is Resource.Success) {
            return
        }
        _uiState.value = _uiState.value.copy(notePath = path, noteTitle = title)
        checkOfflineStatus(path)
        loadNote(path, forceRefresh = false)
    }

    private fun checkOfflineStatus(path: String) {
        viewModelScope.launch {
            val isSaved = repository.isNoteSavedOffline(path)
            _uiState.value = _uiState.value.copy(isSavedOffline = isSaved)
        }
    }

    fun loadNote(path: String = _uiState.value.notePath, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            repository.getMarkdownNote(path, forceRefresh).collect { resource ->
                _uiState.value = _uiState.value.copy(contentResource = resource)
            }
        }
    }

    fun toggleOfflineSave() {
        val path = _uiState.value.notePath
        val newStatus = !_uiState.value.isSavedOffline
        viewModelScope.launch {
            repository.toggleOfflineSaved(path, newStatus)
            _uiState.value = _uiState.value.copy(isSavedOffline = newStatus)
        }
    }

    fun setFontSize(size: Float) {
        _uiState.value = _uiState.value.copy(fontSizeSp = size.coerceIn(12f, 28f))
    }
}
