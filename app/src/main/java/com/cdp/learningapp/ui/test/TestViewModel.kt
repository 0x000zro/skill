package com.cdp.learningapp.ui.test

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.cdp.learningapp.CDPApplication
import com.cdp.learningapp.data.local.TestAttemptEntity
import com.cdp.learningapp.data.model.*
import com.cdp.learningapp.data.repository.ContentRepository
import com.cdp.learningapp.domain.model.Resource
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class TestUiState(
    val resource: Resource<MockTest> = Resource.Loading(),
    val currentQuestionIndex: Int = 0,
    val userAnswers: Map<Int, UserQuestionState> = emptyMap(),
    val remainingSeconds: Long = 0,
    val initialTotalSeconds: Long = 0,
    val isTimerActive: Boolean = false,
    val showPaletteDialog: Boolean = false,
    val showSubmitConfirmDialog: Boolean = false,
    val isSubmitted: Boolean = false,
    val resultSummary: TestResultSummary? = null
) {
    val currentQuestion: Question?
        get() {
            val questions = (resource as? Resource.Success)?.data?.questions ?: return null
            return questions.getOrNull(currentQuestionIndex)
        }

    val totalQuestions: Int
        get() = (resource as? Resource.Success)?.data?.questions?.size ?: 0

    val attemptedCount: Int
        get() = userAnswers.values.count { it.selectedOptionIndex != null }

    val markedForReviewCount: Int
        get() = userAnswers.values.count { it.isMarkedForReview }

    val unattemptedCount: Int
        get() = totalQuestions - attemptedCount
}

class TestViewModel(
    private val repository: ContentRepository = CDPApplication.instance.repository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TestUiState())
    val uiState: StateFlow<TestUiState> = _uiState.asStateFlow()

    private var timerJob: Job? = null

    fun loadMockTest(path: String) {
        viewModelScope.launch {
            repository.getMockTest(path).collect { res ->
                if (res is Resource.Success && res.data != null) {
                    val durationSec = (res.data.durationMinutes * 60).toLong()
                    _uiState.value = _uiState.value.copy(
                        resource = res,
                        currentQuestionIndex = 0,
                        remainingSeconds = durationSec,
                        initialTotalSeconds = durationSec,
                        isSubmitted = false,
                        resultSummary = null,
                        userAnswers = emptyMap()
                    )
                    startCountdownTimer()
                } else {
                    _uiState.value = _uiState.value.copy(resource = res)
                }
            }
        }
    }

    private fun startCountdownTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTimerActive = true)
            while (isActive && _uiState.value.remainingSeconds > 0) {
                delay(1000)
                val newSeconds = _uiState.value.remainingSeconds - 1
                _uiState.value = _uiState.value.copy(remainingSeconds = newSeconds)
            }
            if (_uiState.value.remainingSeconds <= 0 && !_uiState.value.isSubmitted) {
                // Auto-submit when time expires
                submitTest()
            }
        }
    }

    fun selectOption(optionIndex: Int) {
        if (_uiState.value.isSubmitted) return
        val currentIdx = _uiState.value.currentQuestionIndex
        val currentEntry = _uiState.value.userAnswers[currentIdx] ?: UserQuestionState()

        val updatedMap = _uiState.value.userAnswers.toMutableMap()
        // Toggle if already selected or set new
        val newSelection = if (currentEntry.selectedOptionIndex == optionIndex) null else optionIndex
        updatedMap[currentIdx] = currentEntry.copy(selectedOptionIndex = newSelection)
        _uiState.value = _uiState.value.copy(userAnswers = updatedMap)
    }

    fun toggleMarkForReview() {
        if (_uiState.value.isSubmitted) return
        val currentIdx = _uiState.value.currentQuestionIndex
        val currentEntry = _uiState.value.userAnswers[currentIdx] ?: UserQuestionState()

        val updatedMap = _uiState.value.userAnswers.toMutableMap()
        updatedMap[currentIdx] = currentEntry.copy(isMarkedForReview = !currentEntry.isMarkedForReview)
        _uiState.value = _uiState.value.copy(userAnswers = updatedMap)
    }

    fun jumpToQuestion(index: Int) {
        if (index in 0 until _uiState.value.totalQuestions) {
            _uiState.value = _uiState.value.copy(
                currentQuestionIndex = index,
                showPaletteDialog = false
            )
        }
    }

    fun setPaletteDialogVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showPaletteDialog = visible)
    }

    fun setSubmitConfirmVisible(visible: Boolean) {
        _uiState.value = _uiState.value.copy(showSubmitConfirmDialog = visible)
    }

    fun submitTest() {
        timerJob?.cancel()
        val mockTest = (_uiState.value.resource as? Resource.Success)?.data ?: return
        val total = mockTest.questions.size
        var correct = 0
        var wrong = 0
        val reviews = mutableListOf<QuestionReviewItem>()

        mockTest.questions.forEachIndexed { idx, q ->
            val userState = _uiState.value.userAnswers[idx]
            val selected = userState?.selectedOptionIndex
            val isCorrect = selected == q.correctIndex

            if (selected != null) {
                if (isCorrect) correct++ else wrong++
            }

            reviews.add(
                QuestionReviewItem(
                    questionIndex = idx,
                    questionText = q.question,
                    options = q.options,
                    selectedOptionIndex = selected,
                    correctOptionIndex = q.correctIndex,
                    isCorrect = isCorrect,
                    explanation = q.explanation
                )
            )
        }

        val timeTaken = _uiState.value.initialTotalSeconds - _uiState.value.remainingSeconds
        val scorePercent = if (total > 0) (correct.toFloat() / total.toFloat()) * 100f else 0f

        val summary = TestResultSummary(
            testId = mockTest.id,
            testTitle = mockTest.title,
            totalQuestions = total,
            attemptedCount = _uiState.value.attemptedCount,
            correctCount = correct,
            wrongCount = wrong,
            unattemptedCount = total - _uiState.value.attemptedCount,
            scorePercentage = scorePercent,
            timeTakenSeconds = timeTaken,
            reviews = reviews
        )

        // Save result in Room DB
        viewModelScope.launch {
            repository.saveTestAttempt(
                TestAttemptEntity(
                    testId = mockTest.id,
                    testTitle = mockTest.title,
                    score = correct,
                    total = total,
                    timeTakenSeconds = timeTaken
                )
            )
        }

        _uiState.value = _uiState.value.copy(
            isSubmitted = true,
            showSubmitConfirmDialog = false,
            resultSummary = summary
        )
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
