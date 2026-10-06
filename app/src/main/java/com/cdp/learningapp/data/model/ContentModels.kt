package com.cdp.learningapp.data.model

import com.google.gson.annotations.SerializedName

data class Question(
    @SerializedName("id")
    val id: String,

    @SerializedName("question")
    val question: String,

    @SerializedName("options")
    val options: List<String>,

    @SerializedName("answer")
    val rawAnswer: Any,

    @SerializedName("explanation")
    val explanation: String = ""
) {
    /**
     * Resolves the correct option index whether the answer in JSON is an Integer (0-based)
     * or a String that matches one of the option texts.
     */
    val correctIndex: Int
        get() {
            return when (val ans = rawAnswer) {
                is Number -> ans.toInt()
                is String -> {
                    // Try parsing as number first
                    ans.toIntOrNull() ?: options.indexOfFirst { it.trim().equals(ans.trim(), ignoreCase = true) }
                }
                else -> 0
            }.coerceIn(0, (options.size - 1).coerceAtLeast(0))
        }
}

data class PracticeSet(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("category")
    val category: String = "Pedagogy Practice",

    @SerializedName("questions")
    val questions: List<Question> = emptyList()
)

data class MockTest(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("duration_minutes")
    val durationMinutes: Int = 30,

    @SerializedName("category")
    val category: String = "Mock Test",

    @SerializedName("questions")
    val questions: List<Question> = emptyList()
)

enum class QuestionPaletteStatus {
    UNATTEMPTED,
    ATTEMPTED,
    MARKED_FOR_REVIEW
}

data class UserQuestionState(
    val selectedOptionIndex: Int? = null,
    val isMarkedForReview: Boolean = false
) {
    val status: QuestionPaletteStatus
        get() = when {
            isMarkedForReview -> QuestionPaletteStatus.MARKED_FOR_REVIEW
            selectedOptionIndex != null -> QuestionPaletteStatus.ATTEMPTED
            else -> QuestionPaletteStatus.UNATTEMPTED
        }
}

data class QuestionReviewItem(
    val questionIndex: Int,
    val questionText: String,
    val options: List<String>,
    val selectedOptionIndex: Int?,
    val correctOptionIndex: Int,
    val isCorrect: Boolean,
    val explanation: String
)

data class TestResultSummary(
    val testId: String,
    val testTitle: String,
    val totalQuestions: Int,
    val attemptedCount: Int,
    val correctCount: Int,
    val wrongCount: Int,
    val unattemptedCount: Int,
    val scorePercentage: Float,
    val timeTakenSeconds: Long,
    val reviews: List<QuestionReviewItem>
)
