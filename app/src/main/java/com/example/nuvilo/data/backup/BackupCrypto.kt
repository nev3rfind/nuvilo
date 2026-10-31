package com.example.nuvilo.data.backup

import java.security.SecureRandom
import java.util.Arrays
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.json.JSONObject

class InvalidPassphraseOrCorruptedDataException(message: String, cause: Throwable? = null) :
    Exception(message, cause)

class UnsupportedBackupFormatException(message: String) : Exception(message)

object BackupCrypto {

    const val CURRENT_FORMAT_VERSION = 1
    const val CIPHER_ALGORITHM = "AES/GCM/NoPadding"
    const val KDF_ALGORITHM = "PBKDF2WithHmacSHA256"
    const val KDF_ITERATIONS = 100_000
    const val KEY_LENGTH_BITS = 256
    const val SALT_LENGTH_BYTES = 16
    const val NONCE_LENGTH_BYTES = 12
    const val GCM_TAG_LENGTH_BITS = 128

    private val secureRandom = SecureRandom()

    /**
     * Encrypts plaintext bytes using a passphrase with PBKDF2-HMAC-SHA256 and AES-256-GCM.
     * Returns an envelope JSON string containing metadata, salt, nonce, and ciphertext.
     */
    fun encrypt(
        plaintext: ByteArray,
        passphrase: CharArray,
        appVersion: String = "1.0",
        schemaVersion: Int = 3
    ): String {
        require(passphrase.isNotEmpty()) { "Passphrase cannot be empty" }

        val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
        val nonce = ByteArray(NONCE_LENGTH_BYTES).also { secureRandom.nextBytes(it) }

        val keySpec = PBEKeySpec(passphrase, salt, KDF_ITERATIONS, KEY_LENGTH_BITS)
        val secretKeyFactory = SecretKeyFactory.getInstance(KDF_ALGORITHM)
        val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
        keySpec.clearPassword()

        val secretKey = SecretKeySpec(keyBytes, "AES")
        Arrays.fill(keyBytes, 0.toByte())

        val cipher = Cipher.getInstance(CIPHER_ALGORITHM)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)

        val ciphertext = cipher.doFinal(plaintext)

        val envelope = JSONObject().apply {
            put("formatVersion", CURRENT_FORMAT_VERSION)
            put("appVersion", appVersion)
            put("schemaVersion", schemaVersion)
            put("cipher", CIPHER_ALGORITHM)
            put("kdf", KDF_ALGORITHM)
            put("kdfIterations", KDF_ITERATIONS)
            put("saltBase64", Base64.getEncoder().encodeToString(salt))
            put("nonceBase64", Base64.getEncoder().encodeToString(nonce))
            put("ciphertextBase64", Base64.getEncoder().encodeToString(ciphertext))
        }

        return envelope.toString(2)
    }

    /**
     * Decrypts an envelope JSON string using the provided passphrase.
     * Verifies format version, algorithms, and GCM authentication tag.
     */
    fun decrypt(envelopeJson: String, passphrase: CharArray): ByteArray {
        require(passphrase.isNotEmpty()) { "Passphrase cannot be empty" }

        val json = try {
            JSONObject(envelopeJson)
        } catch (e: Exception) {
            throw InvalidPassphraseOrCorruptedDataException("Invalid JSON envelope format", e)
        }

        val formatVersion = json.optInt("formatVersion", -1)
        if (formatVersion > CURRENT_FORMAT_VERSION) {
            throw UnsupportedBackupFormatException(
                "Backup format version $formatVersion is newer than supported version $CURRENT_FORMAT_VERSION"
            )
        }

        val cipherAlg = json.optString("cipher")
        if (cipherAlg != CIPHER_ALGORITHM) {
            throw UnsupportedBackupFormatException("Unsupported cipher algorithm: $cipherAlg")
        }

        val kdfAlg = json.optString("kdf")
        if (kdfAlg != KDF_ALGORITHM) {
            throw UnsupportedBackupFormatException("Unsupported KDF algorithm: $kdfAlg")
        }

        val iterations = json.optInt("kdfIterations", KDF_ITERATIONS)
        val saltBase64 = json.optString("saltBase64")
        val nonceBase64 = json.optString("nonceBase64")
        val ciphertextBase64 = json.optString("ciphertextBase64")

        if (saltBase64.isBlank() || nonceBase64.isBlank() || ciphertextBase64.isBlank()) {
            throw InvalidPassphraseOrCorruptedDataException("Envelope missing required cryptographic components")
        }

        val salt = try {
            Base64.getDecoder().decode(saltBase64)
        } catch (e: Exception) {
            throw InvalidPassphraseOrCorruptedDataException("Invalid salt encoding", e)
        }

        val nonce = try {
            Base64.getDecoder().decode(nonceBase64)
        } catch (e: Exception) {
            throw InvalidPassphraseOrCorruptedDataException("Invalid nonce encoding", e)
        }

        val ciphertext = try {
            Base64.getDecoder().decode(ciphertextBase64)
        } catch (e: Exception) {
            throw InvalidPassphraseOrCorruptedDataException("Invalid ciphertext encoding", e)
        }

        val keySpec = PBEKeySpec(passphrase, salt, iterations, KEY_LENGTH_BITS)
        val secretKeyFactory = SecretKeyFactory.getInstance(kdfAlg)
        val keyBytes = secretKeyFactory.generateSecret(keySpec).encoded
        keySpec.clearPassword()

        val secretKey = SecretKeySpec(keyBytes, "AES")
        Arrays.fill(keyBytes, 0.toByte())

        val cipher = Cipher.getInstance(cipherAlg)
        val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH_BITS, nonce)

        return try {
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            cipher.doFinal(ciphertext)
        } catch (e: Exception) {
            // AEADBadTagException or GeneralSecurityException
            throw InvalidPassphraseOrCorruptedDataException(
                "Decryption failed: Incorrect passphrase or tampered backup data.",
                e
            )
        }
    }

    fun isEncryptedEnvelope(content: String): Boolean {
        return try {
            val obj = JSONObject(content)
            obj.has("cipher") && obj.has("saltBase64") && obj.has("ciphertextBase64")
        } catch (_: Exception) {
            false
        }
    }

    fun encryptJson(jsonString: String, passphrase: CharArray): String {
        return encrypt(jsonString.toByteArray(Charsets.UTF_8), passphrase)
    }

    fun decryptJson(envelopeJson: String, passphrase: CharArray): String {
        val bytes = decrypt(envelopeJson, passphrase)
        return String(bytes, Charsets.UTF_8)
    }
}
