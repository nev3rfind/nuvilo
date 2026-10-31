package com.example.nuvilo.security

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

enum class BiometricStatus {
    AVAILABLE,
    DEVICE_CREDENTIAL_ONLY,
    NONE_ENROLLED,
    NO_HARDWARE,
    UNAVAILABLE
}

sealed interface BiometricAuthResult {
    data object Success : BiometricAuthResult
    data object Cancelled : BiometricAuthResult
    data class Failed(val message: String) : BiometricAuthResult
    data class Error(val errorCode: Int, val errString: CharSequence) : BiometricAuthResult
}

class BiometricAuthManager(private val context: Context) {

    private val biometricManager = BiometricManager.from(context)
    private val keyguardManager = context.getSystemService(Context.KEYGUARD_SERVICE) as? KeyguardManager

    /**
     * Determines the device authentication capabilities.
     * Evaluates Biometric hardware and Android Keyguard device credentials (PIN/pattern/password).
     */
    fun checkBiometricStatus(): BiometricStatus {
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG
        val canAuthBiometric = biometricManager.canAuthenticate(authenticators)
        val isDeviceSecure = keyguardManager?.isDeviceSecure == true

        return when {
            canAuthBiometric == BiometricManager.BIOMETRIC_SUCCESS -> BiometricStatus.AVAILABLE
            isDeviceSecure -> BiometricStatus.DEVICE_CREDENTIAL_ONLY
            canAuthBiometric == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> BiometricStatus.NONE_ENROLLED
            canAuthBiometric == BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> BiometricStatus.NO_HARDWARE
            else -> BiometricStatus.UNAVAILABLE
        }
    }

    /**
     * Launches AndroidX BiometricPrompt on the supplied FragmentActivity.
     * Supports fingerprint, face, and device PIN/pattern/password fallback.
     */
    fun authenticate(
        activity: FragmentActivity,
        onResult: (BiometricAuthResult) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Unlock Novelo")
            .setSubtitle("Confirm your identity to manage medications")
            .setAllowedAuthenticators(authenticators)
            // Note: setNegativeButtonText is strictly prohibited when DEVICE_CREDENTIAL is enabled
            .build()

        val prompt = BiometricPrompt(
            activity,
            executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    onResult(BiometricAuthResult.Success)
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    if (errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                        errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_CANCELED
                    ) {
                        onResult(BiometricAuthResult.Cancelled)
                    } else {
                        onResult(BiometricAuthResult.Error(errorCode, errString))
                    }
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onResult(BiometricAuthResult.Failed("Authentication failed. Please try again."))
                }
            }
        )

        prompt.authenticate(promptInfo)
    }
}
