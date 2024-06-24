package xyz.kgy_production.webdavebookmanager.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import arrow.core.Either
import arrow.core.identity
import java.security.Key
import java.security.KeyStore
import java.security.KeyStore.SecretKeyEntry
import java.security.KeyStoreException
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

// Ref from https://gist.github.com/willyrh495/20fb2975dc71fe6f491365ce2c6c42d2

private val store by lazy {
    val store = KeyStore.getInstance(KEYSTORE_KEY)
    store.load(null)
    store
}

private val aesKey by lazy {
    createKeys(store)
    (store.getEntry(KEY_ALIAS, null) as SecretKeyEntry).secretKey
}

fun encrypt(text: String): Either<Throwable, String> {
    if (text.isEmpty()) return Either.Left(RuntimeException("Empty String cannot be encrypted"))

    return Either.catch {
        val cipher = Cipher.getInstance(ENCRYPTION_ALGO)
        cipher.init(Cipher.ENCRYPT_MODE, aesKey)
        val iv = Base64.encodeToString(cipher.iv, Base64.DEFAULT)
        val digested = Base64.encodeToString(cipher.doFinal(text.toByteArray()), Base64.DEFAULT)

        "$iv;$digested"
    }
}

fun decrypt(text: String): Either<Throwable, String> {
    if (text.isEmpty()) return Either.Left(RuntimeException("Empty String cannot be decrypted"))
    if (!text.contains(";")) return Either.Left(RuntimeException("Invalid encrypted String"))
    val (iv, content) = text.split(";").map { Base64.decode(it, Base64.DEFAULT) }

    return Either.catch {
        val cipher = Cipher.getInstance(ENCRYPTION_ALGO)
        cipher.init(Cipher.DECRYPT_MODE, aesKey, IvParameterSpec(iv))
        val digested = cipher.doFinal(content)

        String(digested)
    }
}

fun createKeys(keyStore: KeyStore) {
    try {
        if (!keyStore.containsAlias(KEY_ALIAS)) {

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
            keyGenerator.generateKey()
        }
    } catch (e: KeyStoreException) {
        e.printStackTrace()
    }
}
