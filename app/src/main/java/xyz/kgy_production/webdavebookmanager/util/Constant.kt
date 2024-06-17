package xyz.kgy_production.webdavebookmanager.util

const val ENCRYPTION_ALGO = "AES/CBS/PKCS5Padding"
const val KEY_ALIAS = "KeyAlias"
const val KEYSTORE_KEY = "AndroidKeyStore"

enum class ThemeOption {
    LIGHT, DARK, AUTO;
}

enum class ConfigKey {
    THEME_OPTION;
}