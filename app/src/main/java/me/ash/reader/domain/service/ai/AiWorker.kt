package me.ash.reader.domain.service.ai

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import me.ash.reader.domain.repository.ArticleDao
import me.ash.reader.domain.service.AccountService
import me.ash.reader.domain.service.RssService
import me.ash.reader.infrastructure.preference.SettingsProvider
import me.ash.reader.infrastructure.rss.ReaderCacheHelper

@HiltWorker
class AiWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val rssService: RssService,
    private val accountService: AccountService,
    private val articleDao: ArticleDao,
    private val aiService: AiService,
    private val readerCacheHelper: ReaderCacheHelper,
    private val settingsProvider: SettingsProvider
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val semaphore = Semaphore(2)
        val aiCredentials = settingsProvider.settings.aiCredentials
        if (aiCredentials.apiKey.isBlank() || aiCredentials.modelId.isBlank()) {
            return Result.failure()
        }

        val deferredList = withContext(Dispatchers.IO) {
            val articlesToProcess =
                articleDao.queryUnreadProcessableArticles(accountService.getCurrentAccountId())

            articlesToProcess
                .filter { it.feed.isSummarize && it.article.summary.isNullOrBlank() }
                .map { articleWithFeed ->
                    async {
                        semaphore.withPermit {
                            val fullContentResult =
                                readerCacheHelper.readOrFetchFullContent(articleWithFeed.article)
                            fullContentResult.getOrNull()?.let { content ->
                                aiService.getSummary(aiCredentials, content)
                                    .onSuccess { summary ->
                                        articleDao.update(articleWithFeed.article.copy(summary = summary))
                                    }
                                    .isSuccess
                            } ?: false
                        }
                    }
                }
        }

        return if (deferredList.awaitAll().any { !it }) Result.retry() else Result.success()
    }

    companion object {
        const val AI_WORKER_TAG = "AI_WORKER_TAG"

        fun enqueue(workManager: WorkManager) {
            workManager.enqueue(OneTimeWorkRequestBuilder<AiWorker>().addTag(AI_WORKER_TAG).build())
        }
    }
}