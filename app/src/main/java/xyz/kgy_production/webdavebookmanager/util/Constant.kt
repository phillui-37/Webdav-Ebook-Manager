package xyz.kgy_production.webdavebookmanager.util

import android.security.keystore.KeyProperties

object Encryption {
    private const val ALGO = KeyProperties.KEY_ALGORITHM_AES
    private const val BLK_MODE = KeyProperties.BLOCK_MODE_CBC
    private const val PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7
    const val TRANSFORMATION = "$ALGO/$BLK_MODE/$PADDING"
}
const val KEY_ALIAS = "KeyAlias"
const val KEYSTORE_KEY = "AndroidKeyStore"

enum class ThemeOption {
    LIGHT, DARK, AUTO;
}

enum class ConfigKey {
    THEME_OPTION;
}

object DateTimePattern {
}

object MimeType {
    const val JSON = "application/json"
}

const val BOOK_METADATA_CONFIG_FILENAME = "webdav_mgr.conf.json"

enum class NotificationChannelEnum(val tag: String, val id: Int) {
    ScanWebDavService("Scan Server Directory", 200)
}

const val EBOOK_READER_LIB_URL = "https://phillui-37.github.io/foliate-js/"
const val WEBVIEW_COMMON_DELAY = 1500 //ms