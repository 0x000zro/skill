package com.cdp.learningapp.ui.practice

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cdp.learningapp.data.model.Question
import com.cdp.learningapp.domain.model.Resource
import com.cdp.learningapp.ui.common.ErrorCard
import com.cdp.learningapp.ui.common.FullScreenLoading
import com.cdp.learningapp.ui.common.OfflineBanner
import com.cdp.learningapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PracticeQuizScreen(
    quizPath: String,
    quizTitle: String,
    onNavigateBack: () -> Unit,
    viewModel: PracticeViewModel = viewModel()
) {
    LaunchedEffect(quizPath) {
        viewModel.loadPracticeSet(quizPath)
    }

    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = quizTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.loadPracticeSet(quizPath, forceRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload")
                    }
                }
            )
        },
        bottomBar = {
            if (uiState.resource is Resource.Success && uiState.totalQuestions > 0) {
                Surface(
                    tonalElevation = 8.dp,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.previousQuestion() },
                            enabled = uiState.currentQuestionIndex > 0
                        ) {
                            Icon(Icons.Default.ArrowBackIos, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Previous")
                        }

                        Text(
                            text = "${uiState.currentQuestionIndex + 1} / ${uiState.totalQuestions}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )

                        Button(
                            onClick = {
                                if (uiState.currentQuestionIndex < uiState.totalQuestions - 1) {
                                    viewModel.nextQuestion()
                                } else {
                                    onNavigateBack()
                                }
                            }
                        ) {
                            Text(if (uiState.currentQuestionIndex == uiState.totalQuestions - 1) "Complete" else "Next")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(Icons.Default.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            val isFromCache = uiState.resource is Resource.Success &&
                    (uiState.resource as Resource.Success).isFromCache
            OfflineBanner(visible = isFromCache)

            when (val res = uiState.resource) {
                is Resource.Loading -> {
                    FullScreenLoading("Loading practice questions...")
                }
                is Resource.Error -> {
                    ErrorCard(
                        message = res.message ?: "Failed to load practice questions",
                        onRetry = { viewModel.loadPracticeSet(quizPath, forceRefresh = true) }
                    )
                }
                is Resource.Success -> {
                    val question = uiState.currentQuestion
                    if (question == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No questions in this set.")
                        }
                    } else {
                        // Linear progress indicator
                        val progress = (uiState.currentQuestionIndex + 1).toFloat() / uiState.totalQuestions.toFloat()
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth(),
                            color = AccentAmber
                        )

                        PracticeQuestionContent(
                            question = question,
                            questionIndex = uiState.currentQuestionIndex,
                            selectedOption = uiState.selectedAnswers[uiState.currentQuestionIndex],
                            isExplanationExpanded = uiState.isExplanationExpanded,
                            onOptionSelected = { viewModel.selectOption(it) },
                            onToggleExplanation = { viewModel.toggleExplanation() }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeQuestionContent(
    question: Question,
    questionIndex: Int,
    selectedOption: Int?,
    isExplanationExpanded: Boolean,
    onOptionSelected: (Int) -> Unit,
    onToggleExplanation: () -> Unit
) {
    val scrollState = rememberScrollState()
    val isAnswered = selectedOption != null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        // Question number chip
        SuggestionChip(
            onClick = {},
            label = { Text("Question ${questionIndex + 1}") }
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Question text
        Text(
            text = question.question,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 28.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Options List
        question.options.forEachIndexed { optIndex, optionText ->
            PracticeOptionItem(
                index = optIndex,
                optionText = optionText,
                isSelected = selectedOption == optIndex,
                isCorrect = optIndex == question.correctIndex,
                isAnswered = isAnswered,
                onClick = { onOptionSelected(optIndex) }
            )
            Spacer(modifier = Modifier.height(10.dp))
        }

        // Instant Explanation Card (shows automatically when answered)
        AnimatedVisibility(
            visible = isAnswered,
            enter = fadeIn() + expandVertically()
        ) {
            val wasCorrect = selectedOption == question.correctIndex
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (wasCorrect) SuccessGreenLight else ErrorRedLight
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = if (wasCorrect) Icons.Default.CheckCircle else Icons.Default.Cancel,
                                contentDescription = null,
                                tint = if (wasCorrect) SuccessGreen else ErrorRed
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (wasCorrect) "सही उत्तर! (Correct)" else "गलत उत्तर! (Incorrect)",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (wasCorrect) SuccessGreen else ErrorRed
                            )
                        }
                        IconButton(onClick = onToggleExplanation) {
                            Icon(
                                imageVector = if (isExplanationExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                contentDescription = "Toggle"
                            )
                        }
                    }

                    if (isExplanationExpanded) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "सही विकल्प: ${question.options.getOrNull(question.correctIndex) ?: ""}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = question.explanation.ifEmpty { "इस प्रश्न के लिए कोई अतिरिक्त व्याख्या उपलब्ध नहीं है।" },
                            style = MaterialTheme.typography.bodyMedium,
                            lineHeight = 22.sp,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PracticeOptionItem(
    index: Int,
    optionText: String,
    isSelected: Boolean,
    isCorrect: Boolean,
    isAnswered: Boolean,
    onClick: () -> Unit
) {
    val optionLabels = listOf("A", "B", "C", "D", "E")
    val label = optionLabels.getOrElse(index) { "${index + 1}" }

    val containerColor = when {
        !isAnswered -> MaterialTheme.colorScheme.surface
        isCorrect -> SuccessGreenLight
        isSelected && !isCorrect -> ErrorRedLight
        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
    }

    val borderColor = when {
        !isAnswered -> MaterialTheme.colorScheme.outlineVariant
        isCorrect -> SuccessGreen
        isSelected && !isCorrect -> ErrorRed
        else -> MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !isAnswered, onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = BorderStroke(1.5.dp, borderColor)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(
                        when {
                            isAnswered && isCorrect -> SuccessGreen
                            isAnswered && isSelected && !isCorrect -> ErrorRed
                            else -> MaterialTheme.colorScheme.primaryContainer
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = label,
                    color = if (isAnswered && (isCorrect || isSelected)) Color.White else MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = optionText,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyLarge,
                color = if (isAnswered && !isSelected && !isCorrect) TextSecondary else TextPrimary
            )
            if (isAnswered) {
                if (isCorrect) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Correct",
                        tint = SuccessGreen
                    )
                } else if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Wrong",
                        tint = ErrorRed
                    )
                }
            }
        }
    }
}
