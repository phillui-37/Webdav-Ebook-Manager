package xyz.kgy_production.webdavebookmanager.util

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import arrow.core.Either
import arrow.core.identity
import java.security.KeyStore
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
    store.getKey(KEY_ALIAS, null) as SecretKey
}

fun prepareCipher(key: ByteArray, iv: ByteArray, mode: Int): Cipher {
    val cipher = Cipher.getInstance(ENCRYPTION_ALGO)
    val secretKeySpec = SecretKeySpec(key, ENCRYPTION_ALGO.split("/")[0])
    val ivParamSpec = IvParameterSpec(iv)
    cipher.init(mode, secretKeySpec, ivParamSpec)
    return cipher
}

fun encrypt(text: String): Either<Throwable, String> {
    if (text.isEmpty()) return Either.Left(RuntimeException("Empty String cannot be encrypted"))

    return Either.catch {
        val iv = generateRandomIV()
        val cipher = prepareCipher(aesKey.encoded, iv.toByteArray(), Cipher.ENCRYPT_MODE)
        val digested = cipher.doFinal(text.toByteArray())

        "$iv;${Base64.encodeToString(digested, Base64.DEFAULT)}"
    }
}

fun decrypt(text: String): Either<Throwable, String> {
    if (text.isEmpty()) return Either.Left(RuntimeException("Empty String cannot be decrypted"))
    if (!text.contains(";")) return Either.Left(RuntimeException("Invalid encrypted String"))
    val (iv, content) = text.split(";")

    return Either.catch {
        val bytes = Base64.decode(content.toByteArray(), Base64.DEFAULT)
        val cipher = prepareCipher(aesKey.encoded, iv.toByteArray(), Cipher.DECRYPT_MODE)
        val digested = cipher.doFinal(bytes)

        String(digested)
    }
}

fun generateRandomIV(): String {
    val random = SecureRandom()
    val generated = random.generateSeed(12)
    return Base64.encodeToString(generated, Base64.DEFAULT)
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
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_OAEP)
                .setRandomizedEncryptionRequired(false)
                .build()

            keyGenerator.init(keyGenParameter)
            keyGenerator.generateKey()
        }
    } catch (e: KeyStoreException) {
        e.printStackTrace()
    }
}
