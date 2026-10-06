package com.cdp.learningapp.ui.markdown

import android.content.Context
import android.graphics.Color as AndroidColor
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.FormatSize
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cdp.learningapp.domain.model.Resource
import com.cdp.learningapp.ui.common.ErrorCard
import com.cdp.learningapp.ui.common.FullScreenLoading
import com.cdp.learningapp.ui.common.OfflineBanner
import io.noties.markwon.LinkResolverDef
import io.noties.markwon.Markwon
import io.noties.markwon.MarkwonConfiguration
import io.noties.markwon.ext.strikethrough.StrikethroughPlugin
import io.noties.markwon.ext.tables.TablePlugin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkdownReaderScreen(
    notePath: String,
    noteTitle: String,
    onNavigateBack: () -> Unit,
    onNavigateToInternalNote: (targetPath: String, targetTitle: String) -> Unit,
    viewModel: MarkdownViewModel = viewModel()
) {
    LaunchedEffect(notePath, noteTitle) {
        viewModel.init(notePath, noteTitle)
    }

    val uiState by viewModel.uiState.collectAsState()
    val isDark = isSystemInDarkTheme()
    val textColor = MaterialTheme.colorScheme.onSurface.toArgb()
    var showFontSizeSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = uiState.noteTitle.ifEmpty { noteTitle },
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
                    // Font size adjuster
                    IconButton(onClick = { showFontSizeSheet = true }) {
                        Icon(Icons.Outlined.FormatSize, contentDescription = "Font Size")
                    }
                    // Offline Save / Bookmark
                    IconButton(onClick = { viewModel.toggleOfflineSave() }) {
                        Icon(
                            imageVector = if (uiState.isSavedOffline) Icons.Default.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = "Save Offline",
                            tint = if (uiState.isSavedOffline) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    // Refresh
                    IconButton(onClick = { viewModel.loadNote(forceRefresh = true) }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            val isFromCache = uiState.contentResource is Resource.Success &&
                    (uiState.contentResource as Resource.Success).isFromCache
            OfflineBanner(visible = isFromCache)

            when (val res = uiState.contentResource) {
                is Resource.Loading -> {
                    FullScreenLoading("Fetching study note from repository...")
                }
                is Resource.Error -> {
                    ErrorCard(
                        message = res.message ?: "Failed to load note",
                        onRetry = { viewModel.loadNote(forceRefresh = true) }
                    )
                }
                is Resource.Success -> {
                    val markdown = res.data ?: ""
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                    ) {
                        AndroidView(
                            factory = { context ->
                                TextView(context).apply {
                                    setTextColor(textColor)
                                    textSize = uiState.fontSizeSp
                                    setLineSpacing(8f, 1.25f)

                                    val markwon = buildMarkwon(context) { clickedUrl ->
                                        // Intercept internal markdown links
                                        if (clickedUrl.endsWith(".md") || clickedUrl.contains(".md#") || clickedUrl.contains(".md")) {
                                            val cleanTarget = resolveRelativePath(notePath, clickedUrl)
                                            val derivedTitle = cleanTarget.substringAfterLast("/")
                                                .removeSuffix(".md")
                                                .replace("_", " ")
                                                .replace("-", " ")
                                                .replaceFirstChar { it.uppercase() }
                                            onNavigateToInternalNote(cleanTarget, derivedTitle)
                                        } else {
                                            // Fallback standard external URL handling
                                            LinkResolverDef().resolve(this, clickedUrl)
                                        }
                                    }
                                    tag = markwon
                                    markwon.setMarkdown(this, markdown)
                                }
                            },
                            update = { textView ->
                                textView.textSize = uiState.fontSizeSp
                                textView.setTextColor(textColor)
                                val markwon = (textView.tag as? Markwon) ?: buildMarkwon(textView.context) { clickedUrl ->
                                    if (clickedUrl.endsWith(".md") || clickedUrl.contains(".md")) {
                                        val cleanTarget = resolveRelativePath(notePath, clickedUrl)
                                        val derivedTitle = cleanTarget.substringAfterLast("/")
                                            .removeSuffix(".md")
                                            .replace("_", " ")
                                            .replaceFirstChar { it.uppercase() }
                                        onNavigateToInternalNote(cleanTarget, derivedTitle)
                                    } else {
                                        LinkResolverDef().resolve(textView, clickedUrl)
                                    }
                                }
                                markwon.setMarkdown(textView, markdown)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (showFontSizeSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFontSizeSheet = false },
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Reading Text Size",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("A", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Slider(
                        value = uiState.fontSizeSp,
                        onValueChange = { viewModel.setFontSize(it) },
                        valueRange = 13f..26f,
                        steps = 6,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 12.dp)
                    )
                    Text("A", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                }
                Text(
                    text = "Current: ${uiState.fontSizeSp.toInt()} sp",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

private fun buildMarkwon(
    context: Context,
    onLinkIntercept: (url: String) -> Unit
): Markwon {
    return Markwon.builder(context)
        .usePlugin(TablePlugin.create(context))
        .usePlugin(StrikethroughPlugin.create())
        .usePlugin(object : io.noties.markwon.AbstractMarkwonPlugin() {
            override fun configureConfiguration(builder: MarkwonConfiguration.Builder) {
                builder.linkResolver { _, link ->
                    onLinkIntercept(link)
                }
            }
        })
        .build()
}

/**
 * Resolves relative markdown links to project root relative paths.
 * E.g., current note = "study/cdp_growth.md", target = "growth_detail.md" -> "study/growth_detail.md"
 */
private fun resolveRelativePath(currentPath: String, targetUrl: String): String {
    val cleanTarget = targetUrl.substringBefore("#").trim()
    if (cleanTarget.startsWith("http://") || cleanTarget.startsWith("https://")) {
        return cleanTarget
    }
    if (cleanTarget.startsWith("/")) {
        return cleanTarget.trimStart('/')
    }
    if (cleanTarget.contains("/")) {
        return cleanTarget
    }
    val currentFolder = currentPath.substringBeforeLast("/", "")
    return if (currentFolder.isNotEmpty()) "$currentFolder/$cleanTarget" else cleanTarget
}
