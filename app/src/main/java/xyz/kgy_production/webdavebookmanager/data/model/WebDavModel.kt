package xyz.kgy_production.webdavebookmanager.data.model

import android.util.Log
import kotlinx.serialization.Serializable
import xyz.kgy_production.webdavebookmanager.common.Result

data class WebDavModel(
    val id: Int = 0,
    val uuid: String = "",
    val name: String = "",
    val url: String,
    val loginId: String = "",
    val password: String = "",
    val folderStructure: String = "",
    val isActive: Boolean = true,
    val bypassPattern: List<ByPassPattern> = listOf()
) {
    @Serializable
    data class ByPassPattern(val pattern: String, val isRegex: Boolean)

    private val urlRegex = Regex("^https?://.*")
    private fun checkUrl(): Result<Unit> {
        Log.d("WebDavModel", "url: $url, ${urlRegex.matches(url)}")
        return if (url.isEmpty() || !urlRegex.matches(url))
            Result.fail("URL not valid")
        else
            Result.ok(Unit)
    }

    private fun checkLoginId() = if (loginId.isEmpty())
        Result.fail("Empty login ID is prohibited")
    else
        Result.ok(Unit)

    private fun checkPassword() = if (password.isEmpty())
        Result.fail("Empty password is prohibited")
    else
        Result.ok(Unit)

    fun validate(): String? {
        return checkUrl()
            .next(::checkLoginId)
            .next(::checkPassword)
            .getError()
    }
}
