package xyz.kgy_production.webdavebookmanager.data.model

import arrow.core.Either
import arrow.core.Option
import arrow.core.flatMap
import arrow.core.toOption

data class WebDavModel(
    val id: Int = 0,
    val uuid: String = "",
    val name: String = "",
    val url: String,
    val loginId: String = "",
    val password: String = "",
    val isActive: Boolean = true
) {
    private val urlRegex = Regex("^https?://")
    private fun checkUrl() = if (url.isEmpty() || !urlRegex.matches(url))
        Either.Left("URL not valid")
    else
        Either.Right(Unit)

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
