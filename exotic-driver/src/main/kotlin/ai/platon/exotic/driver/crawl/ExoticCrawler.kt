package ai.platon.exotic.driver.crawl

import ai.platon.exotic.driver.common.*
import ai.platon.exotic.driver.crawl.entity.ItemDetail
import ai.platon.exotic.driver.crawl.scraper.ListenablePortalTask
import ai.platon.exotic.driver.crawl.scraper.OutPageScraper
import ai.platon.exotic.driver.crawl.scraper.ScrapeTask
import ai.platon.pulsar.driver.DriverSettings
import org.slf4j.LoggerFactory
import org.springframework.core.env.Environment
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.ConcurrentLinkedQueue

class ExoticCrawler(val env: Environment? = null) {
    private val logger = LoggerFactory.getLogger(ExoticCrawler::class.java)

    val scrapeServer: String
        get() = env?.getProperty("scrape.server") ?: "localhost"
    val scrapeServerPath: Int
        get() = env?.getProperty("scrape.server.port")?.toInt() ?: 8182
    val scrapeServerContextPath: String
        get() = env?.getProperty("scrape.server.servlet.context-path") ?: "/api"
    val authToken: String
        get() = env?.getProperty("scrape.authToken") ?: "b06test42c13cb000f74539b20be9550b8a1a90b9"

    val driverSettings get() = DriverSettings(
        scrapeServer,
        authToken,
        scrapeServerPath,
        scrapeServerContextPath
    )

    val outPageScraper = OutPageScraper(driverSettings)

    val driver get() = outPageScraper.taskSubmitter.driver

    val pendingPortalTasks: Deque<ListenablePortalTask> = ConcurrentLinkedDeque()

    val pendingItems = ConcurrentLinkedQueue<ItemDetail>()

    var maxPendingTaskCount = if (IS_DEVELOPMENT) 10 else 50

    fun crawl() {
        val taskSubmitter = outPageScraper.taskSubmitter
        val submittedTaskCount = taskSubmitter.pendingTaskCount

        if (submittedTaskCount >= maxPendingTaskCount) {
            return
        }

        val n = (maxPendingTaskCount - submittedTaskCount).coerceAtMost(10)
        if (pendingPortalTasks.isNotEmpty()) {
            scrapeFromQueue(pendingPortalTasks, n)
        }
    }

    @Throws(Exception::class)
    fun scrapeOutPages(task: ListenablePortalTask) {
        try {
//            task.onItemSuccess = {
//                createPendingItems(it)
//            }
            outPageScraper.scrape(task)
        } catch (t: Throwable) {
            logger.warn("Unexpected exception", t)
        }
    }

    private fun scrapeFromQueue(queue: Queue<ListenablePortalTask>, n: Int) {
        var n0 = n
        while (n0-- > 0) {
            val task = queue.poll()
            if (task != null) {
                scrapeOutPages(task)
            }
        }
    }

    private fun createPendingItems(task: ScrapeTask) {
        val allowDuplicate = task.companionPortalTask?.rule != null
        task.response.resultSet
            ?.filter { it.isNotEmpty() }
            ?.map { ItemDetail.create(it["uri"].toString(), it, allowDuplicate) }
            ?.toCollection(pendingItems)
    }
}

fun main() {
    val scraper = ExoticCrawler()
    scraper.crawl()

    readLine()
}
