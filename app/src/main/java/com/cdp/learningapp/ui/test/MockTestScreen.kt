package com.cdp.learningapp.ui.test

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.GridOn
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
import com.cdp.learningapp.data.model.QuestionPaletteStatus
import com.cdp.learningapp.data.model.UserQuestionState
import com.cdp.learningapp.domain.model.Resource
import com.cdp.learningapp.ui.common.ErrorCard
import com.cdp.learningapp.ui.common.FullScreenLoading
import com.cdp.learningapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MockTestScreen(
    testPath: String,
    testTitle: String,
    onNavigateBack: () -> Unit,
    viewModel: TestViewModel = viewModel()
) {
    LaunchedEffect(testPath) {
        viewModel.loadMockTest(testPath)
    }

    val uiState by viewModel.uiState.collectAsState()
    var showExitWarning by remember { mutableStateOf(false) }

    // Intercept hardware back button during active test
    BackHandler(enabled = !uiState.isSubmitted) {
        showExitWarning = true
    }

    // If submitted, show result screen directly
    if (uiState.isSubmitted && uiState.resultSummary != null) {
        TestResultScreen(
            result = uiState.resultSummary!!,
            onBackToHome = onNavigateBack,
            onRetakeTest = { viewModel.loadMockTest(testPath) }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = testTitle,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                        // Live countdown display
                        val minutes = uiState.remainingSeconds / 60
                        val seconds = uiState.remainingSeconds % 60
                        val timeStr = String.format("%02d:%02d", minutes, seconds)
                        val isLowTime = uiState.remainingSeconds < 120

                        Text(
                            text = "⏱ $timeStr remaining",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isLowTime) ErrorRed else PrimaryLight,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { showExitWarning = true }) {
                        Icon(Icons.Default.Close, contentDescription = "Exit")
                    }
                },
                actions = {
                    // Question Palette button
                    IconButton(onClick = { viewModel.setPaletteDialogVisible(true) }) {
                        Icon(Icons.Outlined.GridOn, contentDescription = "Question Palette")
                    }
                    // Submit button
                    FilledTonalButton(
                        onClick = { viewModel.setSubmitConfirmVisible(true) },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text("Submit")
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
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.jumpToQuestion(uiState.currentQuestionIndex - 1) },
                            enabled = uiState.currentQuestionIndex > 0
                        ) {
                            Icon(Icons.Default.ArrowBackIos, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text("Previous")
                        }

                        // Mark for Review Button
                        val isMarked = uiState.userAnswers[uiState.currentQuestionIndex]?.isMarkedForReview == true
                        FilterChip(
                            selected = isMarked,
                            onClick = { viewModel.toggleMarkForReview() },
                            label = { Text(if (isMarked) "Reviewing" else "Mark Review") },
                            leadingIcon = {
                                Icon(
                                    imageVector = if (isMarked) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        )

                        Button(
                            onClick = {
                                if (uiState.currentQuestionIndex < uiState.totalQuestions - 1) {
                                    viewModel.jumpToQuestion(uiState.currentQuestionIndex + 1)
                                } else {
                                    viewModel.setSubmitConfirmVisible(true)
                                }
                            }
                        ) {
                            Text(if (uiState.currentQuestionIndex == uiState.totalQuestions - 1) "Review All" else "Next")
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
            when (val res = uiState.resource) {
                is Resource.Loading -> {
                    FullScreenLoading("Preparing mock test environment...")
                }
                is Resource.Error -> {
                    ErrorCard(
                        message = res.message ?: "Failed to load mock test",
                        onRetry = { viewModel.loadMockTest(testPath) }
                    )
                }
                is Resource.Success -> {
                    val question = uiState.currentQuestion
                    if (question == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text("No questions found in this mock test.")
                        }
                    } else {
                        // Linear progress indicator
                        val progress = (uiState.currentQuestionIndex + 1).toFloat() / uiState.totalQuestions.toFloat()
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth(),
                            color = PrimaryIndigo
                        )

                        TestQuestionBody(
                            question = question,
                            questionIndex = uiState.currentQuestionIndex,
                            totalQuestions = uiState.totalQuestions,
                            userState = uiState.userAnswers[uiState.currentQuestionIndex] ?: UserQuestionState(),
                            onOptionSelected = { viewModel.selectOption(it) }
                        )
                    }
                }
            }
        }
    }

    // Question Palette Modal Bottom Sheet
    if (uiState.showPaletteDialog) {
        QuestionPaletteBottomSheet(
            totalQuestions = uiState.totalQuestions,
            currentIndex = uiState.currentQuestionIndex,
            userAnswers = uiState.userAnswers,
            onDismiss = { viewModel.setPaletteDialogVisible(false) },
            onSelectQuestion = { viewModel.jumpToQuestion(it) }
        )
    }

    // Submit confirmation dialog
    if (uiState.showSubmitConfirmDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.setSubmitConfirmVisible(false) },
            title = { Text("Submit Mock Test?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Are you sure you want to finalize and submit your test?")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Attempted: ${uiState.attemptedCount} / ${uiState.totalQuestions}", fontWeight = FontWeight.SemiBold)
                    Text("• Marked for Review: ${uiState.markedForReviewCount}", color = AccentAmber)
                    Text("• Unattempted: ${uiState.unattemptedCount}", color = TextSecondary)
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.submitTest() },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryIndigo)
                ) {
                    Text("Confirm & Submit")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.setSubmitConfirmVisible(false) }) {
                    Text("Continue Test")
                }
            }
        )
    }

    // Exit Warning Dialog
    if (showExitWarning) {
        AlertDialog(
            onDismissRequest = { showExitWarning = false },
            title = { Text("Leave Test?") },
            text = { Text("The timer is running. Leaving now will lose your current unsubmitted progress.") },
            confirmButton = {
                Button(
                    onClick = {
                        showExitWarning = false
                        onNavigateBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ErrorRed)
                ) {
                    Text("Leave")
                }
            },
            dismissButton = {
                TextButton(onClick = { showExitWarning = false }) {
                    Text("Stay")
                }
            }
        )
    }
}

@Composable
fun TestQuestionBody(
    question: Question,
    questionIndex: Int,
    totalQuestions: Int,
    userState: UserQuestionState,
    onOptionSelected: (Int) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SuggestionChip(
                onClick = {},
                label = { Text("Question ${questionIndex + 1} of $totalQuestions") }
            )
            if (userState.isMarkedForReview) {
                Badge(containerColor = AccentAmber) {
                    Text("Marked for Review", color = Color.White, modifier = Modifier.padding(4.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = question.question,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 28.sp
        )

        Spacer(modifier = Modifier.height(24.dp))

        question.options.forEachIndexed { optIndex, optionText ->
            val isSelected = userState.selectedOptionIndex == optIndex
            val optionLabels = listOf("A", "B", "C", "D", "E")
            val label = optionLabels.getOrElse(optIndex) { "${optIndex + 1}" }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOptionSelected(optIndex) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isSelected) PrimaryLight.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface
                ),
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.outlineVariant
                )
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
                            .background(if (isSelected) PrimaryIndigo else MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = optionText,
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    RadioButton(
                        selected = isSelected,
                        onClick = { onOptionSelected(optIndex) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuestionPaletteBottomSheet(
    totalQuestions: Int,
    currentIndex: Int,
    userAnswers: Map<Int, UserQuestionState>,
    onDismiss: () -> Unit,
    onSelectQuestion: (Int) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = "Question Palette",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))

            // Palette Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                LegendItem(color = PrimaryIndigo, label = "Attempted")
                LegendItem(color = AccentAmber, label = "Review")
                LegendItem(color = DividerColor, label = "Unattempted")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Grid of questions
            LazyVerticalGrid(
                columns = GridCells.Fixed(5),
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 300.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(totalQuestions) { index ->
                    val state = userAnswers[index]
                    val status = state?.status ?: QuestionPaletteStatus.UNATTEMPTED

                    val (bgColor, textColor) = when (status) {
                        QuestionPaletteStatus.ATTEMPTED -> PrimaryIndigo to Color.White
                        QuestionPaletteStatus.MARKED_FOR_REVIEW -> AccentAmber to Color.White
                        QuestionPaletteStatus.UNATTEMPTED -> SoftBackground to TextPrimary
                    }

                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(bgColor)
                            .border(
                                width = if (index == currentIndex) 2.5.dp else 1.dp,
                                color = if (index == currentIndex) PrimaryDark else DividerColor,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onSelectQuestion(index) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "${index + 1}",
                            color = textColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = label, style = MaterialTheme.typography.bodySmall)
    }
}
