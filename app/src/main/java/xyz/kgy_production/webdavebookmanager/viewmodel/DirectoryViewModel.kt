package xyz.kgy_production.webdavebookmanager.viewmodel

import androidx.lifecycle.ViewModel
import at.bitfire.dav4jvm.Response
import at.bitfire.dav4jvm.property.CreationDate
import at.bitfire.dav4jvm.property.DisplayName
import at.bitfire.dav4jvm.property.GetContentLength
import at.bitfire.dav4jvm.property.GetContentType
import at.bitfire.dav4jvm.property.GetLastModified
import dagger.hilt.android.lifecycle.HiltViewModel
import okhttp3.MediaType
import xyz.kgy_production.webdavebookmanager.data.WebDavRepository
import xyz.kgy_production.webdavebookmanager.util.formatDateTime
import xyz.kgy_production.webdavebookmanager.util.toDateTime
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

@HiltViewModel
class DirectoryViewModel @Inject constructor(
    private val webDavRepository: WebDavRepository
) : ViewModel() {
    data class ContentData(
        val fullUrl: String,
        val name: String,
        val creationTime: LocalDateTime, // from String, yyyy-mm-ddTHH:MM:SSZ
        val lastModifiedDateTime: LocalDateTime, // from Long
        val contentType: MediaType?,
        val fileSize: Long,
    ) {
        val isDir: Boolean
            get() = contentType == null

        companion object {
            fun fromResponse(response: Response): ContentData {
                var result = ContentData(
                    response.href.toString().run {
                        if (endsWith("/")) substring(0, length - 1)
                        else this
                    },
                    "",
                    LocalDateTime.now(),
                    LocalDateTime.now(),
                    null,
                    0L
                )
                response.propstat.forEach { propstat ->
                    propstat.properties.forEach { property ->
                        result = when (property) {
                            is DisplayName -> result.copy(name = property.displayName ?: "-")
                            is GetContentType -> result.copy(contentType = property.type)
                            is GetContentLength -> result.copy(fileSize = property.contentLength)
                            is CreationDate -> result.copy(
                                creationTime = property.creationDate.formatDateTime(
                                    DateTimeFormatter.ISO_OFFSET_DATE_TIME
                                )
                            )

                            is GetLastModified -> result.copy(
                                lastModifiedDateTime = property.lastModified.toDateTime()
                            )

                            else -> result
                        }
                    }
                }
                return result
            }
        }
    }

    suspend fun getWebDavModel(id: Int) = webDavRepository.getEntryById(id)
}