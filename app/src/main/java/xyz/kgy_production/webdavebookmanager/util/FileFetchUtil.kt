package xyz.kgy_production.webdavebookmanager.util

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.request.get
import io.ktor.client.statement.readBytes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import java.util.concurrent.Executors

typealias FilePath = String
typealias FileUrl = String
typealias FetchRequest = Pair<FileUrl, FilePath>
typealias FetchResponse = Pair<FileUrl, Boolean>

object FileFetchUtil {
    private val taskDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private val fetchPoolDispatcher =
        Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())
            .asCoroutineDispatcher()
    private val logger by Logger.delegate(FileFetchUtil::class.java)

    private val fetchRequestChannel = Channel<FetchRequest>()
    private val fetchResultChannel = Channel<FetchResponse>()

    init {
        CoroutineScope(taskDispatcher).launch {
            while (true) {
                val request = fetchRequestChannel.receive()
                logger.d("assets fetch request received for ${request.first} to ${request.second}")
                CoroutineScope(fetchPoolDispatcher).launch {
                    val result = receiveFetchRequest(request)
                    logger.d("asset fetch result for ${request.first} is $result")
                    fetchResultChannel.send(request.first to result)
                }
            }
        }
    }

    private suspend fun receiveFetchRequest(request: FetchRequest) = try {
        HttpClient(CIO).use { client ->
            val filename = request.first.split("/").last()
            val file = client.get(request.first).readBytes()
            saveFile(filename, request.second) { file }
        }
        true
    } catch (err: Exception) {
        logger.e("err occurred", err)
        false
    }


    suspend fun sendFetchRequest(url: FileUrl, dir: FilePath) {
        fetchRequestChannel.send(url to dir)
    }

    suspend fun getResult(url: FileUrl): Boolean {
        var result: Boolean? = null
        while (result == null) {
            val (_url, _result) = fetchResultChannel.receive()
            if (_url != url)
                fetchResultChannel.send(_url to _result)
            else
                result = _result
        }
        return result
    }
}