package xyz.kgy_production.webdavebookmanager.data.model

import android.util.Log
import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.toOption
import kotlinx.serialization.Serializable

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
    private fun checkUrl(): Either<String, Unit> {
        Log.d("WebDavModel", "url: $url, ${urlRegex.matches(url)}")
        return if (url.isEmpty() || !urlRegex.matches(url))
            Either.Left("URL not valid")
        else
            Either.Right(Unit)
    }

    private fun checkLoginId() = if (loginId.isEmpty())
        Either.Left("Empty login ID is prohibited")
    else
        Either.Right(Unit)

    private fun checkPassword() = if (password.isEmpty())
        Either.Left("Empty password is prohibited")
    else
        Either.Right(Unit)

    fun validate(): Option<String> {
        return checkUrl()
            .flatMap { checkLoginId() }
            .flatMap { checkPassword() }
            .leftOrNull()
            .toOption()
    }
}
