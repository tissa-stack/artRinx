package com.rinx.artRINXapp.core.auth.google

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.rinx.artRINXapp.BuildConfig
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Thin wrapper over the Credential Manager "Sign in with Google" flow.
 *
 * Uses [GetSignInWithGoogleOption] (the explicit button flow): it always shows the account chooser,
 * behaving identically for brand-new sign-ups and returning sign-ins — exactly what a "Continue with
 * Google" button needs. The web (server) client id is the ID-token audience the backend validates.
 */
@Singleton
class GoogleAuthClient @Inject constructor() {

    /** Launch the Google account chooser and return the ID token + account details, or a typed failure. */
    suspend fun getResult(context: Context): GoogleSignInResult {
        val activity = context.findActivity()
            ?: return GoogleSignInResult.Failure(
                IllegalStateException("No Activity available for Credential Manager"),
            )

        val option = GetSignInWithGoogleOption
            .Builder(BuildConfig.GOOGLE_WEB_CLIENT_ID)
            .build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(option)
            .build()

        return try {
            val response = CredentialManager.create(activity).getCredential(activity, request)
            val credential = response.credential
            if (credential is CustomCredential &&
                credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
            ) {
                val google = GoogleIdTokenCredential.createFrom(credential.data)
                if (google.idToken.isBlank()) {
                    GoogleSignInResult.Failure(IllegalStateException("Empty Google ID token"))
                } else {
                    GoogleSignInResult.Success(
                        idToken = google.idToken,
                        displayName = google.displayName,
                        givenName = google.givenName,
                        familyName = google.familyName,
                        photoUrl = google.profilePictureUri?.toString(),
                    )
                }
            } else {
                GoogleSignInResult.Failure(IllegalStateException("Unexpected credential type"))
            }
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            GoogleSignInResult.NoCredential
        } catch (e: GetCredentialProviderConfigurationException) {
            GoogleSignInResult.PlayServicesUnavailable
        } catch (e: GoogleIdTokenParsingException) {
            GoogleSignInResult.Failure(e)
        } catch (e: Exception) {
            GoogleSignInResult.Failure(e)
        }
    }

    /**
     * Clear the saved credential selection so the next sign-in re-prompts account choice. Best-effort:
     * a failure here must never block logout, hence [runCatching]. Accepts an application context.
     */
    suspend fun clearCredentialState(context: Context) {
        runCatching {
            CredentialManager.create(context.applicationContext)
                .clearCredentialState(ClearCredentialStateRequest())
        }
    }

    private fun Context.findActivity(): Activity? {
        var current: Context? = this
        while (current is ContextWrapper) {
            if (current is Activity) return current
            current = current.baseContext
        }
        return null
    }
}
