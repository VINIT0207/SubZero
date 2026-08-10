package com.example.subzero.utils

import android.util.Base64
import java.security.spec.KeySpec
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object EncryptionHelper {
    private const val ALGORITHM = "AES/CBC/PKCS5Padding"
    private const val KEY_ALGORITHM = "PBKDF2WithHmacSHA1"
    private const val ITERATION_COUNT = 1000
    private const val KEY_LENGTH = 256
    
    // Constant salt and IV for deterministic key derivation of backups
    private val SALT = byteArrayOf(0xa9.toByte(), 0x9b.toByte(), 0xc8.toByte(), 0x32.toByte(), 0x56.toByte(), 0x35.toByte(), 0xe3.toByte(), 0x03.toByte())
    private val IV = byteArrayOf(0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08, 0x09, 0x10, 0x11, 0x12, 0x13, 0x14, 0x15, 0x16)

    fun encrypt(plainText: String, password: String): String {
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), SALT, ITERATION_COUNT, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, IvParameterSpec(IV))
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(cipherText, Base64.DEFAULT or Base64.NO_WRAP)
    }

    fun decrypt(cipherText: String, password: String): String {
        val factory = SecretKeyFactory.getInstance(KEY_ALGORITHM)
        val spec: KeySpec = PBEKeySpec(password.toCharArray(), SALT, ITERATION_COUNT, KEY_LENGTH)
        val tmp = factory.generateSecret(spec)
        val secretKey = SecretKeySpec(tmp.encoded, "AES")

        val cipher = Cipher.getInstance(ALGORITHM)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, IvParameterSpec(IV))
        val decoded = Base64.decode(cipherText, Base64.DEFAULT)
        val decryptedBytes = cipher.doFinal(decoded)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    fun saveFileToDownloads(context: android.content.Context, fileName: String, content: String, mimeType: String = "text/csv"): android.net.Uri? {
        val resolver = context.contentResolver
        return try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                val contentValues = android.content.ContentValues().apply {
                    put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(android.provider.MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, android.os.Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, contentValues)
                if (uri != null) {
                    resolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(content.toByteArray(Charsets.UTF_8))
                    }
                }
                uri
            } else {
                val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS)
                if (!downloadsDir.exists()) {
                    downloadsDir.mkdirs()
                }
                val file = java.io.File(downloadsDir, fileName)
                file.writeText(content, Charsets.UTF_8)
                android.net.Uri.fromFile(file)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
