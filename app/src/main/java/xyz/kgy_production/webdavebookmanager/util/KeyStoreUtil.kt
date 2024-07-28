package xyz.kgy_production.webdavebookmanager.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import java.security.KeyStore.SecretKeyEntry
import java.security.KeyStoreException
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

private fun getStore(): KeyStore {
    val store = KeyStore.getInstance(KEYSTORE_KEY)
    store.load(null)
    return store
}

private fun getKey(): SecretKey =
    (getStore().getEntry(KEY_ALIAS, null) as? SecretKeyEntry)
        ?.secretKey ?: createKeys()

fun encrypt(text: String): String {
    if (text.isEmpty()) throw RuntimeException("Empty String cannot be encrypted")

    val cipher = Cipher.getInstance(Encryption.TRANSFORMATION)
    val key = getKey()
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val iv = Base64.encodeToString(cipher.iv, Base64.DEFAULT)
    val digested = Base64.encodeToString(cipher.doFinal(text.toByteArray()), Base64.DEFAULT)

    return "$iv;$digested"
}

fun decrypt(text: String): String {
    if (text.isEmpty()) throw RuntimeException("Empty String cannot be decrypted")
    if (!text.contains(";")) throw RuntimeException("Invalid encrypted String")
    val (iv, content) = text.split(";").map { Base64.decode(it, Base64.DEFAULT) }

    val cipher = Cipher.getInstance(Encryption.TRANSFORMATION)
    val key = getKey()
    val ivSpec = IvParameterSpec(iv)
    cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
    val digested = cipher.doFinal(content)

    return String(digested)

}

private fun createKeys(): SecretKey {
    try {
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_KEY)
        val keyGenParameterSpecBuilder = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )

        val keyGenParameter = keyGenParameterSpecBuilder
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .build()

        keyGenerator.init(keyGenParameter)
        return keyGenerator.generateKey()
    } catch (e: KeyStoreException) {
        e.printStackTrace()
        throw e
    }
}
