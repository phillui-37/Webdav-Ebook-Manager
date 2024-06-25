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