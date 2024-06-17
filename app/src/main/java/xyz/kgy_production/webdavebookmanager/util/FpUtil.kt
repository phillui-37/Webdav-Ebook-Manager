package xyz.kgy_production.webdavebookmanager.util

// extension
fun <T, U> T.eq(u: U) = this?.equals(u) ?: (this == u)
fun <T, U> T.ne(u: U) = !(this.eq(u))

private enum class Ord {
    LT, EQ, GT;
}
private fun <T: Comparable<T>> T.cmp(other: T): Ord {
    val result = compareTo(other)
    return when {
        result < 0 -> Ord.LT
        result > 0 -> Ord.GT
        else -> Ord.EQ
    }
}

fun <T: Comparable<T>> T.gt(other: T) = cmp(other) == Ord.GT
fun <T: Comparable<T>> T.ge(other: T) = cmp(other) >= Ord.EQ
fun <T: Comparable<T>> T.lt(other: T) = cmp(other) == Ord.LT
fun <T: Comparable<T>> T.le(other: T) = cmp(other) <= Ord.EQ