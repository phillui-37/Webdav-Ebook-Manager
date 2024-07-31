package xyz.kgy_production.webdavebookmanager.common

data class Result<T>(
    private val result: T?,
    private val error: String?
) {
    companion object {
        fun <T> fail(error: String) =
            Result<T>(null, error)
        fun <T> ok(value: T) = Result(value, null)
    }

    val isFail: Boolean
        get() = error != null

    fun next(getResult: () -> Result<T>): Result<T> = if (isFail) this else getResult()
    fun or(getResult: () -> Result<T>): Result<T> = if (!isFail) this else getResult()
    fun get() = result
    fun getError() = error
}