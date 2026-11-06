package com.example.nuvilo.regression

import com.example.nuvilo.data.backup.BackupCrypto
import com.example.nuvilo.data.backup.InvalidPassphraseOrCorruptedDataException
import org.json.JSONObject
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64
import java.util.Random

/**
 * Regression Test Suite: Category C (Backup, Restoration & Security)
 * Tests T086 to T093: Cryptographic Envelope, PBKDF2 Key Derivation, and AES-GCM Integrity
 */
class BackupCryptoRegressionTest {

    private val defaultPassphrase = "NoveloSecurePassword2026!".toCharArray()

    @Test
    fun t086_roundTripEncryptionAndDecryptionRecoversExactPlaintext() {
        // T086: Round-trip encryption and decryption with valid passphrase recovers exact UTF-8 plaintext
        val originalText = "Confidential Patient Medication Records: Metformin 500mg, Atorvastatin 20mg"
        val plaintextBytes = originalText.toByteArray(Charsets.UTF_8)

        val encryptedEnvelope = BackupCrypto.encrypt(plaintextBytes, defaultPassphrase)
        val decryptedBytes = BackupCrypto.decrypt(encryptedEnvelope, defaultPassphrase)

        assertEquals(originalText, String(decryptedBytes, Charsets.UTF_8))
    }

    @Test
    fun t087_binaryDataRoundTripPreservesArbitraryByteSequences() {
        // T087: Binary data round-trip encryption and decryption preserves arbitrary byte sequences
        val randomBytes = ByteArray(1024)
        Random(42).nextBytes(randomBytes)

        val encryptedEnvelope = BackupCrypto.encrypt(randomBytes, defaultPassphrase)
        val decryptedBytes = BackupCrypto.decrypt(encryptedEnvelope, defaultPassphrase)

        assertArrayEquals(randomBytes, decryptedBytes)
    }

    @Test
    fun t088_decryptionWithIncorrectPassphraseThrowsException() {
        // T088: Decryption with incorrect passphrase throws InvalidPassphraseOrCorruptedDataException
        val plaintext = "Sensitive data".toByteArray(Charsets.UTF_8)
        val encryptedEnvelope = BackupCrypto.encrypt(plaintext, defaultPassphrase)

        val wrongPassphrase = "WrongPassword999!".toCharArray()
        assertThrows(InvalidPassphraseOrCorruptedDataException::class.java) {
            BackupCrypto.decrypt(encryptedEnvelope, wrongPassphrase)
        }
    }

    @Test
    fun t089_decryptionWithCorruptedCiphertextThrowsException() {
        // T089: Decryption with corrupted ciphertext base64 throws InvalidPassphraseOrCorruptedDataException
        val plaintext = "Integrity test string".toByteArray(Charsets.UTF_8)
        val envelope = BackupCrypto.encrypt(plaintext, defaultPassphrase)

        val json = JSONObject(envelope)
        val originalCiphertext = json.getString("ciphertextBase64")
        val tampered = if (originalCiphertext.startsWith("A")) "B" + originalCiphertext.substring(1) else "A" + originalCiphertext.substring(1)
        json.put("ciphertextBase64", tampered)

        assertThrows(InvalidPassphraseOrCorruptedDataException::class.java) {
            BackupCrypto.decrypt(json.toString(), defaultPassphrase)
        }
    }

    @Test
    fun t090_decryptionWithModifiedSaltFailsAuthentication() {
        // T090: Decryption with modified salt base64 fails authentication
        val plaintext = "Salt tamper test".toByteArray(Charsets.UTF_8)
        val envelope = BackupCrypto.encrypt(plaintext, defaultPassphrase)

        val json = JSONObject(envelope)
        val saltBytes = Base64.getDecoder().decode(json.getString("saltBase64"))
        saltBytes[0] = (saltBytes[0].toInt() xor 0xFF).toByte()
        json.put("saltBase64", Base64.getEncoder().encodeToString(saltBytes))

        assertThrows(InvalidPassphraseOrCorruptedDataException::class.java) {
            BackupCrypto.decrypt(json.toString(), defaultPassphrase)
        }
    }

    @Test
    fun t091_decryptionWithModifiedNonceFailsAuthentication() {
        // T091: Decryption with modified IV/nonce base64 fails authentication
        val plaintext = "Nonce tamper test".toByteArray(Charsets.UTF_8)
        val envelope = BackupCrypto.encrypt(plaintext, defaultPassphrase)

        val json = JSONObject(envelope)
        val nonceBytes = Base64.getDecoder().decode(json.getString("nonceBase64"))
        nonceBytes[0] = (nonceBytes[0].toInt() xor 0x01).toByte()
        json.put("nonceBase64", Base64.getEncoder().encodeToString(nonceBytes))

        assertThrows(InvalidPassphraseOrCorruptedDataException::class.java) {
            BackupCrypto.decrypt(json.toString(), defaultPassphrase)
        }
    }

    @Test
    fun t092_emptyPlaintextEncryptsAndDecryptsCleanly() {
        // T092: Empty plaintext encrypts and decrypts cleanly
        val emptyBytes = ByteArray(0)
        val envelope = BackupCrypto.encrypt(emptyBytes, defaultPassphrase)
        val decrypted = BackupCrypto.decrypt(envelope, defaultPassphrase)

        assertEquals(0, decrypted.size)
    }

    @Test
    fun t093_largePayloadEncryptsAndDecryptsWithIntactIntegrity() {
        // T093: Large payload (64KB) encrypts and decrypts with intact integrity
        val largeBytes = ByteArray(64 * 1024)
        Random(12345).nextBytes(largeBytes)

        val envelope = BackupCrypto.encrypt(largeBytes, defaultPassphrase)
        val decrypted = BackupCrypto.decrypt(envelope, defaultPassphrase)

        assertArrayEquals(largeBytes, decrypted)
    }
}
