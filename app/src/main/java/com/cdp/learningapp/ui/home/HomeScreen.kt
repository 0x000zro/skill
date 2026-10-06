package com.cdp.learningapp.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cdp.learningapp.data.model.*
import com.cdp.learningapp.domain.model.Resource
import com.cdp.learningapp.ui.common.EmptyContentView
import com.cdp.learningapp.ui.common.ErrorCard
import com.cdp.learningapp.ui.common.FullScreenLoading
import com.cdp.learningapp.ui.common.OfflineBanner
import com.cdp.learningapp.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToMarkdown: (path: String, title: String) -> Unit,
    onNavigateToPractice: (path: String, title: String) -> Unit,
    onNavigateToTest: (path: String, title: String) -> Unit,
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "CDP Hub • CTET/UPTET",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${uiState.currentOwner}/${uiState.currentRepo} @ ${uiState.currentBranch}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color.White.copy(alpha = 0.8f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                ),
                actions = {
                    IconButton(onClick = { viewModel.loadManifest(forceRefresh = true) }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = { viewModel.openSettingsDialog() }) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Repository Settings",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Offline banner indicator
            val isFromCache = uiState.manifestResource is Resource.Success &&
                    (uiState.manifestResource as Resource.Success).isFromCache
            OfflineBanner(visible = isFromCache)

            // Search Bar
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.updateSearchQuery(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search syllabus, notes, quizzes...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(24.dp)
            )

            // 4 Modes Tab Row
            val tabs = listOf("Study", "Revision", "Practice", "Mock Test")
            TabRow(
                selectedTabIndex = uiState.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = uiState.selectedTab == index,
                        onClick = { viewModel.selectTab(index) },
                        text = {
                            Text(
                                text = title,
                                fontWeight = if (uiState.selectedTab == index) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }

            // Content body based on resource state
            when (val res = uiState.manifestResource) {
                is Resource.Loading -> {
                    FullScreenLoading("Syncing latest CMS manifest from GitHub...")
                }
                is Resource.Error -> {
                    ErrorCard(
                        message = res.message ?: "Failed to load manifest",
                        onRetry = { viewModel.loadManifest(forceRefresh = true) }
                    )
                }
                is Resource.Success -> {
                    val manifest = res.data
                    if (manifest == null) {
                        EmptyContentView()
                    } else {
                        when (uiState.selectedTab) {
                            0 -> StudyList(
                                items = manifest.studyModes.filter {
                                    it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                                            it.category.contains(uiState.searchQuery, ignoreCase = true)
                                },
                                onItemClick = { onNavigateToMarkdown(it.path, it.title) }
                            )
                            1 -> RevisionList(
                                items = manifest.revisionModes.filter {
                                    it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                                            it.category.contains(uiState.searchQuery, ignoreCase = true)
                                },
                                onItemClick = { onNavigateToMarkdown(it.path, it.title) }
                            )
                            2 -> PracticeList(
                                items = manifest.practiceModes.filter {
                                    it.title.contains(uiState.searchQuery, ignoreCase = true) ||
                                            it.category.contains(uiState.searchQuery, ignoreCase = true)
                                },
                                onItemClick = { onNavigateToPractice(it.path, it.title) }
                            )
                            3 -> TestList(
                                items = manifest.testModes.filter {
                                    it.title.contains(uiState.searchQuery, ignoreCase = true)
                                },
                                onItemClick = { onNavigateToTest(it.path, it.title) }
                            )
                        }
                    }
                }
            }
        }
    }

    if (uiState.showSettingsDialog) {
        RepositorySettingsDialog(
            currentOwner = uiState.currentOwner,
            currentRepo = uiState.currentRepo,
            currentBranch = uiState.currentBranch,
            onDismiss = { viewModel.closeSettingsDialog() },
            onSave = { owner, repo, branch ->
                viewModel.updateGitHubConfig(owner, repo, branch)
            }
        )
    }
}

@Composable
fun StudyList(
    items: List<StudyItem>,
    onItemClick: (StudyItem) -> Unit
) {
    if (items.isEmpty()) {
        EmptyContentView(title = "No study notes found")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) },
                shape = RoundedCornerShape(12.dp),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text(item.category, fontSize = 11.sp) },
                            modifier = Modifier.height(24.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun RevisionList(
    items: List<RevisionItem>,
    onItemClick: (RevisionItem) -> Unit
) {
    if (items.isEmpty()) {
        EmptyContentView(title = "No revision sheets found")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            ElevatedCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(SecondaryEmerald.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.FlashOn,
                            contentDescription = null,
                            tint = SecondaryEmerald
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "⚡ Quick Review Bullets",
                            style = MaterialTheme.typography.bodySmall,
                            color = SecondaryEmerald
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = "Open",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun PracticeList(
    items: List<PracticeItem>,
    onItemClick: (PracticeItem) -> Unit
) {
    if (items.isEmpty()) {
        EmptyContentView(title = "No practice sets available")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(AccentAmber.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Quiz,
                            contentDescription = null,
                            tint = AccentAmber
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${item.totalQuestions} Questions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "• Instant Answers & Explanations",
                                style = MaterialTheme.typography.bodySmall,
                                color = SuccessGreen
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Start",
                        tint = AccentAmber
                    )
                }
            }
        }
    }
}

@Composable
fun TestList(
    items: List<TestItem>,
    onItemClick: (TestItem) -> Unit
) {
    if (items.isEmpty()) {
        EmptyContentView(title = "No mock tests available")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(items, key = { it.id }) { item ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onItemClick(item) },
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceCard),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(PrimaryIndigo.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = PrimaryIndigo
                            )
                        }
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Timed Simulation • Auto-Submit",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = DividerColor)
                    Spacer(modifier = Modifier.height(12.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Outlined.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.durationMinutes} mins",
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Icon(
                                imageVector = Icons.Outlined.FormatListNumbered,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "${item.totalQuestions} Questions",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }

                        Button(
                            onClick = { onItemClick(item) },
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp)
                        ) {
                            Text("Start Test")
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RepositorySettingsDialog(
    currentOwner: String,
    currentRepo: String,
    currentBranch: String,
    onDismiss: () -> Unit,
    onSave: (owner: String, repo: String, branch: String) -> Unit
) {
    var owner by remember { mutableStateOf(currentOwner) }
    var repo by remember { mutableStateOf(currentRepo) }
    var branch by remember { mutableStateOf(currentBranch) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Configure Remote Headless CMS") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Specify your GitHub repository coordinates. Content will be loaded dynamically from the raw GitHub API.",
                    style = MaterialTheme.typography.bodySmall
                )
                OutlinedTextField(
                    value = owner,
                    onValueChange = { owner = it },
                    label = { Text("GitHub Owner / Username") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = repo,
                    onValueChange = { repo = it },
                    label = { Text("Repository Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = branch,
                    onValueChange = { branch = it },
                    label = { Text("Branch (e.g. main)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { onSave(owner.trim(), repo.trim(), branch.trim()) }) {
                Text("Apply & Sync")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
