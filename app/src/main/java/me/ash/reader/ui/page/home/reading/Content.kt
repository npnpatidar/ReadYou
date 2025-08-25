package me.ash.reader.ui.page.home.reading

import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.util.Date
import me.ash.reader.infrastructure.preference.LocalReadingRenderer
import me.ash.reader.infrastructure.preference.LocalReadingSubheadUpperCase
import me.ash.reader.infrastructure.preference.ReadingRendererPreference
import me.ash.reader.ui.component.reader.LocalTextContentWidth
import me.ash.reader.ui.component.reader.MarkdownUtils
import me.ash.reader.ui.component.reader.Reader
import me.ash.reader.ui.component.reader.bodyStyle
import me.ash.reader.ui.component.reader.htmlFormattedText
import me.ash.reader.ui.component.reader.textHorizontalPadding
import me.ash.reader.ui.component.webview.RYWebView
import me.ash.reader.ui.ext.drawVerticalScrollbar
import me.ash.reader.ui.ext.extractDomain
import me.ash.reader.ui.ext.roundClick

import me.ash.reader.ui.page.adaptive.ReaderState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun Content(
    modifier: Modifier = Modifier,
    summary: String?,
    content: String,
    description: String?,
    feedName: String,
    title: String,
    author: String? = null,
    link: String = "",
    publishedDate: Date,
    scrollState: ScrollState,
    listState: LazyListState,
    isLoading: Boolean,
    isSummarizing: Boolean = false,
    readingMode: ReaderState.ReadingMode,
    isSummaryAvailable: Boolean,
    contentPadding: PaddingValues = PaddingValues(),
    onImageClick: ((imgUrl: String, altText: String) -> Unit)? = null,
    onSummarize: () -> Unit = {},
) {
    val context = LocalContext.current
    val subheadUpperCase = LocalReadingSubheadUpperCase.current
    val renderer = LocalReadingRenderer.current
    val textContentWidth = LocalTextContentWidth.current
    val maxWidthModifier = Modifier.widthIn(max = textContentWidth)
    val uriHandler = LocalUriHandler.current
    val headline =
        @Composable {
            Column(modifier = Modifier.then(maxWidthModifier).padding(horizontal = 12.dp)) {
                DisableSelection {
                    Metadata(
                        feedName = feedName,
                        title = title,
                        author = author,
                        publishedDate = publishedDate,
                        modifier = Modifier.roundClick { link?.let { uriHandler.openUri(it) } },
                    )
                }
            }
        }

    if (isLoading) {
        Column { LoadingIndicator(modifier = Modifier.size(56.dp)) }
    } else {
        when (renderer) {
            ReadingRendererPreference.WebView -> {
                Column(
                    modifier =
                        modifier.padding(top = contentPadding.calculateTopPadding()).fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize().verticalScroll(scrollState),
                        horizontalAlignment = Alignment.CenterHorizontally,

                    ) {

                        val contentToDisplay = when (readingMode) {
                            ReaderState.ReadingMode.Summary -> if (isSummaryAvailable && !summary.isNullOrBlank()) MarkdownUtils.markdownToHtml(context, summary) else ""
                            ReaderState.ReadingMode.FullContent -> content
                            ReaderState.ReadingMode.Description -> description ?: ""
                            else -> ""
                        }
                        Column(modifier = Modifier.then(maxWidthModifier)) {
                            // Top bar height
                            Spacer(modifier = Modifier.height(64.dp))
                            // padding
                            headline()
                            RYWebView(
                                modifier = Modifier.fillMaxSize(),
                                content = when (readingMode) {
                                    ReaderState.ReadingMode.Summary -> {
                                        if (isSummaryAvailable && !summary.isNullOrBlank()) {
                                            MarkdownUtils.markdownToHtml(context, summary)
                                        } else if (isSummarizing) {
                                            "<p>Generating summary...</p>"
                                        } else {
                                            ""
                                        }
                                    }
                                    ReaderState.ReadingMode.FullContent -> content
                                    ReaderState.ReadingMode.Description -> description ?: ""
                                    else -> ""
                                } ?: "",
                                refererDomain = link.extractDomain(),
                                onImageClick = onImageClick,
                            )
                            Spacer(modifier = Modifier.height(128.dp))
                            Spacer(
                                modifier = Modifier.height(contentPadding.calculateBottomPadding())
                            )
                        }
                    }
                }
            }
            ReadingRendererPreference.NativeComponent -> {
                SelectionContainer {
                    LazyColumn(
                        modifier = modifier.fillMaxSize().drawVerticalScrollbar(listState),
                        state = listState,
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        item {
                            // Top bar height
                            Spacer(modifier = Modifier.height(64.dp))
                            // padding
                            Spacer(modifier = Modifier.height(contentPadding.calculateTopPadding()))
                            headline()
                        }

                        val contentToDisplay = when (readingMode) {
                            ReaderState.ReadingMode.Summary -> {
                                if (isSummaryAvailable && !summary.isNullOrBlank()) {
                                    MarkdownUtils.markdownToHtml(context, summary)
                                } else if (isSummarizing) {
                                    "" // Return empty string for contentToDisplay
                                } else {
                                    "" // Fallback to empty if not summarizing and no summary
                                }
                            }
                            ReaderState.ReadingMode.FullContent -> content
                            ReaderState.ReadingMode.Description -> description ?: ""
                            else -> ""
                        }

                        if (readingMode == ReaderState.ReadingMode.Summary && isSummarizing && !isSummaryAvailable) { // New condition for NativeComponent loading
                            item {
                                Column(modifier = Modifier.then(maxWidthModifier)) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    LoadingIndicator(modifier = Modifier.size(56.dp).align(Alignment.CenterHorizontally))
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "Generating summary...",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = textHorizontalPadding().dp)
                                    )
                                }
                            }
                        } else if (readingMode == ReaderState.ReadingMode.Summary && isSummaryAvailable && !summary.isNullOrBlank()) {
                            item {
                                Column(modifier = Modifier.then(maxWidthModifier)) {
                                    HorizontalDivider(modifier = Modifier.padding(top = 12.dp, bottom = 12.dp))
                                    Text(
                                        text = "Summary",
                                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                        modifier = Modifier.padding(horizontal = textHorizontalPadding().dp)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = MarkdownUtils.markdownToAnnotatedString(summary),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(horizontal = textHorizontalPadding().dp)
                                    )
                                }
                            }
                        }

                        Reader(
                            context = context,
                            subheadUpperCase = subheadUpperCase.value,
                            link = link,
                            content = contentToDisplay ?: "",
                            onImageClick = onImageClick,
                            onLinkClick = { uriHandler.openUri(it) },
                        )

                        item {
                            Spacer(modifier = Modifier.height(128.dp))
                            Spacer(
                                modifier = Modifier.height(contentPadding.calculateBottomPadding())
                            )
                        }
                    }
                }
            }

        }
    }
}
