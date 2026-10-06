package com.cdp.learningapp.ui.practice

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cdp.learningapp.CDPApplication
import com.cdp.learningapp.data.model.PracticeSet
import com.cdp.learningapp.data.model.Question
import com.cdp.learningapp.data.repository.ContentRepository
import com.cdp.learningapp.domain.model.Resource
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PracticeUiState(
    val resource: Resource<PracticeSet> = Resource.Loading(),
    val currentQuestionIndex: Int = 0,
    val selectedAnswers: Map<Int, Int> = emptyMap(), // questionIndex -> selectedOptionIndex
    val isExplanationExpanded: Boolean = true
) {
    val currentQuestion: Question?
        get() {
            val questions = (resource as? Resource.Success)?.data?.questions ?: return null
            return questions.getOrNull(currentQuestionIndex)
        }

    val totalQuestions: Int
        get() = (resource as? Resource.Success)?.data?.questions?.size ?: 0
}

class PracticeViewModel(
    private val repository: ContentRepository = CDPApplication.instance.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PracticeUiState())
    val uiState: StateFlow<PracticeUiState> = _uiState.asStateFlow()

    fun loadPracticeSet(path: String, forceRefresh: Boolean = false) {
        viewModelScope.launch {
            repository.getPracticeSet(path, forceRefresh).collect { resource ->
                _uiState.value = _uiState.value.copy(
                    resource = resource,
                    currentQuestionIndex = 0,
                    selectedAnswers = emptyMap()
                )
            }
        }
    }

    fun selectOption(optionIndex: Int) {
        val currIdx = _uiState.value.currentQuestionIndex
        // Only allow selecting if not already answered
        if (!_uiState.value.selectedAnswers.containsKey(currIdx)) {
            val newAnswers = _uiState.value.selectedAnswers.toMutableMap()
            newAnswers[currIdx] = optionIndex
            _uiState.value = _uiState.value.copy(
                selectedAnswers = newAnswers,
                isExplanationExpanded = true
            )
        }
    }

    fun nextQuestion() {
        val nextIdx = _uiState.value.currentQuestionIndex + 1
        if (nextIdx < _uiState.value.totalQuestions) {
            _uiState.value = _uiState.value.copy(currentQuestionIndex = nextIdx)
        }
    }

    fun previousQuestion() {
        val prevIdx = _uiState.value.currentQuestionIndex - 1
        if (prevIdx >= 0) {
            _uiState.value = _uiState.value.copy(currentQuestionIndex = prevIdx)
        }
    }

    fun toggleExplanation() {
        _uiState.value = _uiState.value.copy(
            isExplanationExpanded = !_uiState.value.isExplanationExpanded
        )
    }
}
