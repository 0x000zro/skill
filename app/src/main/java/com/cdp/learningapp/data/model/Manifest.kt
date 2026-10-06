package com.cdp.learningapp.data.model

import com.google.gson.annotations.SerializedName

data class ManifestResponse(
    @SerializedName("version")
    val version: Int = 1,
    
    @SerializedName("last_updated")
    val lastUpdated: String? = null,

    @SerializedName("study_modes")
    val studyModes: List<StudyItem> = emptyList(),

    @SerializedName("revision_modes")
    val revisionModes: List<RevisionItem> = emptyList(),

    @SerializedName("practice_modes")
    val practiceModes: List<PracticeItem> = emptyList(),

    @SerializedName("test_modes")
    val testModes: List<TestItem> = emptyList()
)

data class StudyItem(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("path")
    val path: String,

    @SerializedName("category")
    val category: String = "General"
)

data class RevisionItem(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("path")
    val path: String,

    @SerializedName("category")
    val category: String = "Revision"
)

data class PracticeItem(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("path")
    val path: String,

    @SerializedName("category")
    val category: String = "Practice",

    @SerializedName("total_questions")
    val totalQuestions: Int = 0
)

data class TestItem(
    @SerializedName("id")
    val id: String,

    @SerializedName("title")
    val title: String,

    @SerializedName("path")
    val path: String,

    @SerializedName("duration_minutes")
    val durationMinutes: Int = 30,

    @SerializedName("total_questions")
    val totalQuestions: Int = 0
)
