package xyz.kgy_production.webdavebookmanager.util

import arrow.core.getOrElse

fun String.encrypt() = encrypt(this).getOrElse {
    it.printStackTrace()
    ""
}

fun String.decrypt() = decrypt(this)
    .getOrElse {
        it.printStackTrace()
        ""
    }