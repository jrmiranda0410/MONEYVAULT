package com.example.security

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.biometrics.BiometricPrompt
import android.os.Build
import android.os.CancellationSignal
import androidx.core.content.ContextCompat

object BiometricHelper {

    fun isBiometricAvailable(context: Context): Boolean {
        return context.packageManager.hasSystemFeature(PackageManager.FEATURE_FINGERPRINT)
    }

    fun authenticate(
        activity: Activity,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val executor = ContextCompat.getMainExecutor(activity)
            val cancellationSignal = CancellationSignal()

            val callback = object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult?) {
                    super.onAuthenticationSucceeded(result)
                    onSuccess()
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence?) {
                    super.onAuthenticationError(errorCode, errString)
                    onError(errString?.toString() ?: "Authentication error")
                }

                override fun onAuthenticationFailed() {
                    super.onAuthenticationFailed()
                    onError("Authentication failed")
                }
            }

            try {
                val prompt = BiometricPrompt.Builder(activity)
                    .setTitle("MoneyVault Security")
                    .setSubtitle("Unlock with Biometrics")
                    .setDescription("Authenticate to access your private financial vault.")
                    .setNegativeButton("Use Passcode", executor) { _, _ ->
                        cancellationSignal.cancel()
                    }
                    .build()

                prompt.authenticate(cancellationSignal, executor, callback)
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Biometric prompt error")
            }
        } else {
            onError("Biometrics not supported on this OS version")
        }
    }
}
