package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.example.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

sealed class AuthUiState {
    object Idle : AuthUiState()
    object Loading : AuthUiState()
    data class Success(val user: FirebaseUser) : AuthUiState()
    data class SuccessLocal(val userInfo: AuthUserInfo) : AuthUiState()
    data class Error(val message: String, val isConfigurationError: Boolean = false) : AuthUiState()
}

data class AuthUserInfo(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isAnonymous: Boolean = false,
    val isLocalOnly: Boolean = false
)

class FirebaseAuthManager(
    private val context: Context,
    private val analyticsManager: HabitAnalyticsManager? = null
) {
    private val TAG = "FirebaseAuthManager"

    private var auth: FirebaseAuth? = null
    private val credentialManager: CredentialManager = CredentialManager.create(context)

    private val _currentUser = MutableStateFlow<AuthUserInfo?>(null)
    val currentUser: StateFlow<AuthUserInfo?> = _currentUser.asStateFlow()

    private val _authState = MutableStateFlow<AuthUiState>(AuthUiState.Idle)
    val authState: StateFlow<AuthUiState> = _authState.asStateFlow()

    init {
        try {
            auth = FirebaseAuth.getInstance()
            updateCurrentUser(auth?.currentUser)
            auth?.addAuthStateListener { firebaseAuth ->
                updateCurrentUser(firebaseAuth.currentUser)
            }
        } catch (e: Exception) {
            Log.w(TAG, "FirebaseAuth initialization warning: ${e.message}")
        }
    }

    private fun updateCurrentUser(user: FirebaseUser?) {
        if (user != null) {
            val info = AuthUserInfo(
                uid = user.uid,
                email = user.email,
                displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Habit Explorer",
                photoUrl = user.photoUrl?.toString(),
                isAnonymous = user.isAnonymous,
                isLocalOnly = false
            )
            _currentUser.value = info
            _authState.value = AuthUiState.Success(user)
            analyticsManager?.setUserId(user.uid)
        } else {
            // Keep local user if currently logged in locally
            if (_currentUser.value?.isLocalOnly != true) {
                _currentUser.value = null
                _authState.value = AuthUiState.Idle
                analyticsManager?.setUserId(null)
            }
        }
    }

    val isUserSignedIn: Boolean
        get() = _currentUser.value != null

    val currentUserId: String?
        get() = _currentUser.value?.uid

    /**
     * Translates Firebase exceptions into human-readable user explanations.
     */
    private fun formatAuthErrorMessage(e: Exception, action: String): Pair<String, Boolean> {
        val raw = e.message ?: e.localizedMessage ?: "Unknown authentication error"
        val lower = raw.lowercase()
        return when {
            lower.contains("configuration_not_found") || lower.contains("configuration not found") -> {
                Pair(
                    "Firebase Authentication 'Email/Password' provider is not enabled in Firebase Console. You can enable it in Firebase Console > Authentication > Sign-in Method, or continue using Local Account Mode below.",
                    true
                )
            }
            lower.contains("invalid_login_credentials") || lower.contains("invalid-credential") || lower.contains("wrong-password") -> {
                Pair("Incorrect email or password. Please verify your credentials or create a new account.", false)
            }
            lower.contains("user-not-found") -> {
                Pair("No account found with this email. Switch to the 'Create Account' tab to register.", false)
            }
            lower.contains("email-already-in-use") || lower.contains("email_exists") -> {
                Pair("An account with this email already exists. Switch to the 'Sign In' tab.", false)
            }
            lower.contains("weak-password") -> {
                Pair("Password must be at least 6 characters.", false)
            }
            lower.contains("invalid-email") -> {
                Pair("Please enter a valid email address.", false)
            }
            lower.contains("network") || lower.contains("timeout") -> {
                Pair("Network connection issue. Please check your connection or continue in Local Account Mode.", false)
            }
            else -> {
                Pair("$action failed: $raw", false)
            }
        }
    }

    /**
     * Seamlessly creates or logs in a local account on device.
     */
    fun signInLocalUser(email: String, displayName: String): AuthUserInfo {
        val cleanEmail = email.trim().ifBlank { "local_user@habitflow.app" }
        val cleanName = displayName.trim().ifBlank { cleanEmail.substringBefore("@").ifBlank { "Habit User" } }
        val info = AuthUserInfo(
            uid = "local_${System.currentTimeMillis()}",
            email = cleanEmail,
            displayName = cleanName,
            photoUrl = null,
            isAnonymous = false,
            isLocalOnly = true
        )
        _currentUser.value = info
        _authState.value = AuthUiState.SuccessLocal(info)
        analyticsManager?.logAuthEvent("login", "local_mode")
        return info
    }

    suspend fun signInWithGoogle(webClientId: String = ""): Result<AuthUserInfo> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth ?: return@withContext Result.failure(
            IllegalStateException("Firebase Auth is not initialized. Please ensure google-services.json is configured.")
        )

        _authState.value = AuthUiState.Loading

        try {
            val serverClientId = if (webClientId.isNotBlank()) {
                webClientId
            } else {
                "504640700093-apps.googleusercontent.com"
            }

            val googleIdOption = GetSignInWithGoogleOption.Builder(serverClientId)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val result = credentialManager.getCredential(context = context, request = request)
            val credential = result.credential

            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                val authCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
                val authResult = firebaseAuth.signInWithCredential(authCredential).await()
                val user = authResult.user

                if (user != null) {
                    val info = AuthUserInfo(
                        uid = user.uid,
                        email = user.email,
                        displayName = user.displayName ?: googleIdTokenCredential.displayName ?: "Habit Explorer",
                        photoUrl = user.photoUrl?.toString() ?: googleIdTokenCredential.profilePictureUri?.toString(),
                        isLocalOnly = false
                    )
                    _currentUser.value = info
                    _authState.value = AuthUiState.Success(user)
                    analyticsManager?.logAuthEvent("login", "google")
                    Result.success(info)
                } else {
                    _authState.value = AuthUiState.Error("Failed to retrieve user profile after sign-in")
                    Result.failure(Exception("Empty Firebase user returned"))
                }
            } else {
                _authState.value = AuthUiState.Error("Unsupported credential type")
                Result.failure(Exception("Unsupported credential type received"))
            }
        } catch (e: GetCredentialException) {
            val errorMsg = "Google Sign-In canceled or unavailable: ${e.localizedMessage}"
            Log.e(TAG, errorMsg, e)
            _authState.value = AuthUiState.Error(errorMsg)
            analyticsManager?.recordNonFatalException(TAG, errorMsg, e)
            Result.failure(e)
        } catch (e: Exception) {
            val (msg, isConfig) = formatAuthErrorMessage(e, "Google Sign-In")
            Log.e(TAG, "Google Sign-In error: $msg", e)
            _authState.value = AuthUiState.Error(msg, isConfig)
            analyticsManager?.recordNonFatalException(TAG, msg, e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): Result<AuthUserInfo> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth ?: run {
            val info = signInLocalUser(email, "")
            return@withContext Result.success(info)
        }
        if (email.isBlank() || pass.isBlank()) {
            _authState.value = AuthUiState.Error("Email and password cannot be blank")
            return@withContext Result.failure(IllegalArgumentException("Email and password cannot be blank"))
        }

        _authState.value = AuthUiState.Loading

        try {
            val authResult = firebaseAuth.signInWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user
            if (user != null) {
                val info = AuthUserInfo(
                    uid = user.uid,
                    email = user.email,
                    displayName = user.displayName ?: user.email?.substringBefore("@") ?: "Habit User",
                    photoUrl = user.photoUrl?.toString(),
                    isLocalOnly = false
                )
                _currentUser.value = info
                _authState.value = AuthUiState.Success(user)
                analyticsManager?.logAuthEvent("login", "email_password")
                Result.success(info)
            } else {
                _authState.value = AuthUiState.Error("Sign-in failed")
                Result.failure(Exception("Null user after email login"))
            }
        } catch (e: Exception) {
            val (msg, isConfig) = formatAuthErrorMessage(e, "Email sign-in")
            Log.e(TAG, "Email sign-in error: $msg", e)
            _authState.value = AuthUiState.Error(msg, isConfig)
            Result.failure(e)
        }
    }

    suspend fun registerWithEmail(email: String, pass: String, displayName: String): Result<AuthUserInfo> = withContext(Dispatchers.IO) {
        val firebaseAuth = auth ?: run {
            val info = signInLocalUser(email, displayName)
            return@withContext Result.success(info)
        }
        if (email.isBlank() || pass.length < 6) {
            _authState.value = AuthUiState.Error("Password must be at least 6 characters")
            return@withContext Result.failure(IllegalArgumentException("Invalid password"))
        }

        _authState.value = AuthUiState.Loading

        try {
            val authResult = firebaseAuth.createUserWithEmailAndPassword(email.trim(), pass).await()
            val user = authResult.user
            if (user != null) {
                if (displayName.isNotBlank()) {
                    try {
                        val profileUpdate = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName.trim())
                            .build()
                        user.updateProfile(profileUpdate).await()
                    } catch (e: Exception) {
                        Log.w(TAG, "Could not set display name: ${e.message}")
                    }
                }

                val info = AuthUserInfo(
                    uid = user.uid,
                    email = user.email,
                    displayName = if (displayName.isNotBlank()) displayName.trim() else user.email?.substringBefore("@"),
                    photoUrl = null,
                    isLocalOnly = false
                )
                _currentUser.value = info
                _authState.value = AuthUiState.Success(user)
                analyticsManager?.logAuthEvent("sign_up", "email_password")
                Result.success(info)
            } else {
                _authState.value = AuthUiState.Error("Account creation failed")
                Result.failure(Exception("Null user after account creation"))
            }
        } catch (e: Exception) {
            val (msg, isConfig) = formatAuthErrorMessage(e, "Account creation")
            Log.e(TAG, "Account creation error: $msg", e)
            _authState.value = AuthUiState.Error(msg, isConfig)
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth?.signOut()
            _currentUser.value = null
            _authState.value = AuthUiState.Idle
            analyticsManager?.logAuthEvent("logout", "user_action")
        } catch (e: Exception) {
            Log.e(TAG, "Sign out error: ${e.message}")
            _currentUser.value = null
            _authState.value = AuthUiState.Idle
        }
    }

    fun clearErrorState() {
        if (_authState.value is AuthUiState.Error) {
            _authState.value = if (_currentUser.value != null) {
                val current = _currentUser.value!!
                if (current.isLocalOnly) {
                    AuthUiState.SuccessLocal(current)
                } else {
                    auth?.currentUser?.let { AuthUiState.Success(it) } ?: AuthUiState.Idle
                }
            } else {
                AuthUiState.Idle
            }
        }
    }
}
