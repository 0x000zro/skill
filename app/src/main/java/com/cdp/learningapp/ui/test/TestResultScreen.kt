package com.cdp.learningapp.ui.test

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cdp.learningapp.data.model.QuestionReviewItem
import com.cdp.learningapp.data.model.TestResultSummary
import com.cdp.learningapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TestResultScreen(
    result: TestResultSummary,
    onBackToHome: () -> Unit,
    onRetakeTest: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scorecard & Analysis", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackToHome) {
                        Icon(Icons.Default.Close, contentDescription = "Exit")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main Score Summary Card
            item {
                ScoreSummaryCard(result)
            }

            // Statistics Breakdown Row
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatMetricCard(
                        title = "Correct",
                        count = result.correctCount,
                        color = SuccessGreen,
                        containerColor = SuccessGreenLight,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Wrong",
                        count = result.wrongCount,
                        color = ErrorRed,
                        containerColor = ErrorRedLight,
                        modifier = Modifier.weight(1f)
                    )
                    StatMetricCard(
                        title = "Skipped",
                        count = result.unattemptedCount,
                        color = TextSecondary,
                        containerColor = SoftBackground,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Retake & Home Buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onRetakeTest,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Replay, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Retake Test")
                    }
                    Button(
                        onClick = onBackToHome,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Done")
                    }
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Detailed Question Review & Explanations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            // Question Review List
            items(result.reviews) { review ->
                QuestionReviewCard(review)
            }
        }
    }
}

@Composable
fun ScoreSummaryCard(result: TestResultSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PrimaryIndigo)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = result.testTitle,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White.copy(alpha = 0.9f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${result.correctCount} / ${result.totalQuestions}",
                fontSize = 44.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Score: ${result.scorePercentage.toInt()}%",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (result.scorePercentage >= 60f) SecondaryLight else AccentAmber
            )
            Spacer(modifier = Modifier.height(12.dp))
            val minutes = result.timeTakenSeconds / 60
            val seconds = result.timeTakenSeconds % 60
            Text(
                text = "Time Spent: ${minutes}m ${seconds}s",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
fun StatMetricCard(
    title: String,
    count: Int,
    color: Color,
    containerColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "$count",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = color
            )
        }
    }
}

@Composable
fun QuestionReviewCard(review: QuestionReviewItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = SurfaceCard),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Q${review.questionIndex + 1}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                val statusText = when {
                    review.selectedOptionIndex == null -> "Skipped"
                    review.isCorrect -> "Correct"
                    else -> "Wrong"
                }
                val statusColor = when {
                    review.selectedOptionIndex == null -> TextSecondary
                    review.isCorrect -> SuccessGreen
                    else -> ErrorRed
                }
                SuggestionChip(
                    onClick = {},
                    label = { Text(statusText, color = statusColor, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = review.questionText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(12.dp))

            review.options.forEachIndexed { optIdx, optText ->
                val isSelectedByUser = review.selectedOptionIndex == optIdx
                val isCorrectOption = review.correctOptionIndex == optIdx

                val bg = when {
                    isCorrectOption -> SuccessGreenLight
                    isSelectedByUser && !review.isCorrect -> ErrorRedLight
                    else -> Color.Transparent
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(bg)
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val symbol = when {
                        isCorrectOption -> "✓ "
                        isSelectedByUser -> "✗ "
                        else -> "  "
                    }
                    Text(
                        text = "$symbol${optIdx + 1}. $optText",
                        style = MaterialTheme.typography.bodyMedium,
                        color = when {
                            isCorrectOption -> SuccessGreen
                            isSelectedByUser -> ErrorRed
                            else -> TextPrimary
                        },
                        fontWeight = if (isCorrectOption || isSelectedByUser) FontWeight.SemiBold else FontWeight.Normal
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
            }

            if (review.explanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = SoftBackground)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "व्याख्या (Explanation):",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = review.explanation,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        }
    }
}
