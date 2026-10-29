package com.nothing.player;

import android.content.Context;
import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

/**
 * Hardware Biometric Authentication Engine (Fingerprint & Face Recognition)
 * adhering to Google AndroidX Biometric standards with Nothing OS styling.
 */
public class BiometricAuthHelper {

    public interface BiometricCallback {
        void onAuthenticationSucceeded();
        void onAuthenticationFailed();
        void onAuthenticationError(int errorCode, String errString);
    }

    public static boolean isBiometricAvailable(Context context) {
        if (context == null) return false;
        BiometricManager manager = BiometricManager.from(context);
        int result = manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        );
        return result == BiometricManager.BIOMETRIC_SUCCESS;
    }

    public static String getBiometricStatus(Context context) {
        if (context == null) return "UNAVAILABLE";
        BiometricManager manager = BiometricManager.from(context);
        int result = manager.canAuthenticate(
                BiometricManager.Authenticators.BIOMETRIC_STRONG |
                BiometricManager.Authenticators.BIOMETRIC_WEAK
        );
        switch (result) {
            case BiometricManager.BIOMETRIC_SUCCESS:
                return "READY (FINGERPRINT / FACE)";
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED:
                return "NOT ENROLLED IN PHONE SETTINGS";
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE:
                return "NO BIOMETRIC HARDWARE DETECTED";
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE:
                return "BIOMETRIC SENSOR BUSY";
            default:
                return "UNAVAILABLE";
        }
    }

    public static void authenticate(
            @NonNull FragmentActivity activity,
            @NonNull String title,
            @NonNull String subtitle,
            @NonNull String negativeButtonText,
            @NonNull BiometricCallback callback
    ) {
        Executor executor = ContextCompat.getMainExecutor(activity);

        BiometricPrompt biometricPrompt = new BiometricPrompt(activity, executor,
                new BiometricPrompt.AuthenticationCallback() {
                    @Override
                    public void onAuthenticationSucceeded(@NonNull BiometricPrompt.AuthenticationResult result) {
                        super.onAuthenticationSucceeded(result);
                        callback.onAuthenticationSucceeded();
                    }

                    @Override
                    public void onAuthenticationFailed() {
                        super.onAuthenticationFailed();
                        callback.onAuthenticationFailed();
                    }

                    @Override
                    public void onAuthenticationError(int errorCode, @NonNull CharSequence errString) {
                        super.onAuthenticationError(errorCode, errString);
                        callback.onAuthenticationError(errorCode, errString.toString());
                    }
                });

        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle(title)
                .setSubtitle(subtitle)
                .setNegativeButtonText(negativeButtonText)
                .setConfirmationRequired(false)
                .build();

        biometricPrompt.authenticate(promptInfo);
    }
}

