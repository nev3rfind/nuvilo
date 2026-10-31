package com.example.nuvilo.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupCryptoTest {

    @Test
    fun roundTripEncryptionDecryptionSucceeds() {
        val samplePlaintext = "Hello Novelo: Confidential Medication Backup 12345".toByteArray(Charsets.UTF_8)
        val passphrase = "SecurePassword123!".toCharArray()

        val encryptedEnvelope = BackupCrypto.encrypt(samplePlaintext, passphrase)
        assertTrue(encryptedEnvelope.contains("formatVersion"))
        assertTrue(encryptedEnvelope.contains("ciphertextBase64"))

        val decryptedBytes = BackupCrypto.decrypt(encryptedEnvelope, passphrase)
        assertArrayEquals(samplePlaintext, decryptedBytes)
        assertEquals("Hello Novelo: Confidential Medication Backup 12345", String(decryptedBytes, Charsets.UTF_8))
    }

    @Test
    fun wrongPassphraseThrowsException() {
        val samplePlaintext = "Confidential Records".toByteArray(Charsets.UTF_8)
        val correctPassphrase = "CorrectPassword".toCharArray()
        val wrongPassphrase = "WrongPassword".toCharArray()

        val encryptedEnvelope = BackupCrypto.encrypt(samplePlaintext, correctPassphrase)

        assertThrows(InvalidPassphraseOrCorruptedDataException::class.java) {
            BackupCrypto.decrypt(encryptedEnvelope, wrongPassphrase)
        }
    }

    @Test
    fun tamperedCiphertextThrowsException() {
        val samplePlaintext = "Confidential Records".toByteArray(Charsets.UTF_8)
        val passphrase = "MySecretPassword".toCharArray()

        val encryptedEnvelope = BackupCrypto.encrypt(samplePlaintext, passphrase)
        val json = org.json.JSONObject(encryptedEnvelope)
        val originalCiphertext = json.getString("ciphertextBase64")
        val tamperedCiphertext = if (originalCiphertext.startsWith("A")) "B" + originalCiphertext.substring(1) else "A" + originalCiphertext.substring(1)
        json.put("ciphertextBase64", tamperedCiphertext)

        assertThrows(InvalidPassphraseOrCorruptedDataException::class.java) {
            BackupCrypto.decrypt(json.toString(), passphrase)
        }
    }
}
