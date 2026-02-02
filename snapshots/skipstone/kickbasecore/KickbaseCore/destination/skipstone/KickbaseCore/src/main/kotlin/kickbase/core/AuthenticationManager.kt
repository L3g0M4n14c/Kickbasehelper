package kickbase.core

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import skip.lib.*

import skip.foundation.*
import skip.ui.*
import skip.model.*

@Stable
open class AuthenticationManager: ObservableObject {
    override val objectWillChange = ObservableObjectPublisher()
    open var isAuthenticated: Boolean
        get() = _isAuthenticated.wrappedValue
        set(newValue) {
            objectWillChange.send()
            _isAuthenticated.wrappedValue = newValue
        }
    var _isAuthenticated: skip.model.Published<Boolean> = skip.model.Published(false)
    open var isLoading: Boolean
        get() = _isLoading.wrappedValue
        set(newValue) {
            objectWillChange.send()
            _isLoading.wrappedValue = newValue
        }
    var _isLoading: skip.model.Published<Boolean> = skip.model.Published(false)
    open var errorMessage: String?
        get() = _errorMessage.wrappedValue
        set(newValue) {
            objectWillChange.send()
            _errorMessage.wrappedValue = newValue
        }
    var _errorMessage: skip.model.Published<String?> = skip.model.Published(null)
    open var currentUser: User?
        get() = _currentUser.wrappedValue
        set(newValue) {
            objectWillChange.send()
            _currentUser.wrappedValue = newValue
        }
    var _currentUser: skip.model.Published<User?> = skip.model.Published(null)

    var accessToken: String? = null
        private set
    private val apiService = KickbaseAPIService()

    constructor() {
        loadStoredToken()
    }

    open suspend fun login(email: String, password: String): Unit = MainActor.run {
        isLoading = true
        errorMessage = null

        try {
            print("🚀 Starting login process...")

            val loginResponse = MainActor.run { apiService }.login(email = email, password = password)

            print("✅ Login successful!")
            print("🎯 Token: ${loginResponse.tkn.prefix(20)}...")

            // Set token for API service
            apiService.setAuthToken(loginResponse.tkn)
            this.accessToken = loginResponse.tkn

            // Wenn User-Daten in der Login-Response sind, verwende sie
            val matchtarget_0 = loginResponse.user
            if (matchtarget_0 != null) {
                val user = matchtarget_0
                print("👤 User from login: ${user.name} (${user.email})")
                this.currentUser = user
            } else {
                // Andernfalls erstelle einen Platzhalter-User
                // Die echten User-Daten werden beim Laden der Ligen geholt
                print("⚠️ No user data in login response, creating placeholder")
                this.currentUser = User(id = loginResponse.userId ?: "unknown", name = "User", teamName = "", email = email, budget = 0, teamValue = 0, points = 0, placement = 0, flags = 0)
            }

            this.isAuthenticated = true
            storeToken(loginResponse.tkn)

        } catch (decodingError: DecodingError) {
            print("💥 Login decoding error: ${decodingError}")

            // Detaillierte Fehlerinformation für Debugging
            when (decodingError) {
                is DecodingError.KeyNotFoundCase -> {
                    val key = decodingError.associated0
                    val context = decodingError.associated1
                    print("❌ Missing key '${key.stringValue}' - ${context.debugDescription}")
                }
                is DecodingError.TypeMismatchCase -> {
                    val type = decodingError.associated0
                    val context = decodingError.associated1
                    print("❌ Type mismatch for type '${type}' - ${context.debugDescription}")
                }
                is DecodingError.ValueNotFoundCase -> {
                    val type = decodingError.associated0
                    val context = decodingError.associated1
                    print("❌ Value not found for type '${type}' - ${context.debugDescription}")
                }
                is DecodingError.DataCorruptedCase -> {
                    val context = decodingError.associated0
                    print("❌ Data corrupted - ${context.debugDescription}")
                }
                else -> print("❌ Unknown decoding error")
            }

            errorMessage = "Unerwartete Serverantwort. Bitte versuchen Sie es erneut."
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("💥 Login error: ${error}")

            // Spezifische Fehlerbehandlung
            val nsError = error as NSError

            if (nsError.domain == "InvalidCredentials" || nsError.code == 401) {
                errorMessage = "Ungültige E-Mail oder Passwort. Bitte überprüfen Sie Ihre Anmeldedaten."
            } else if (nsError.domain == NSURLErrorDomain) {
                errorMessage = "Netzwerkfehler. Bitte prüfen Sie Ihre Internetverbindung und versuchen Sie es erneut."
            } else {
                val matchtarget_1 = nsError.localizedDescription as String?
                if (matchtarget_1 != null) {
                    val localizedDescription = matchtarget_1
                    if (!localizedDescription.isEmpty) {
                        errorMessage = localizedDescription
                    } else {
                        errorMessage = "Login fehlgeschlagen. Bitte versuchen Sie es erneut."
                    }
                } else {
                    errorMessage = "Login fehlgeschlagen. Bitte versuchen Sie es erneut."
                }
            }
        }

        isLoading = false
    }

    internal open suspend fun loginWithDemo(): Unit = MainActor.run {
        isLoading = true
        errorMessage = null

        try {
            print("🎮 Starting demo mode...")

            // Simuliere kurze Verzögerung für realistische UX
            Task.sleep(nanoseconds = 1_000_000_000) // 1 Sekunde

            val demoLoginResponse = DemoDataService.createDemoLoginResponse()

            // Set token for API service
            apiService.setAuthToken(demoLoginResponse.tkn)
            this.accessToken = demoLoginResponse.tkn

            demoLoginResponse.user?.let { user ->
                print("👤 Demo User: ${user.name} - ${user.teamName}")
                this.currentUser = user
            }

            this.isAuthenticated = true
            storeToken(demoLoginResponse.tkn)

            print("✅ Demo mode activated!")

        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("💥 Demo mode error: ${error}")
            errorMessage = "Fehler beim Laden der Demo-Daten"
        }

        isLoading = false
    }

    internal open fun logout() {
        print("👋 Logging out user")
        isAuthenticated = false
        currentUser = null
        accessToken = null
        removeStoredToken()
    }

    private fun storeToken(token: String) {
        UserDefaults.standard.set(token, forKey = "kickbase_token")
        print("💾 Token stored securely")
    }

    private fun loadStoredToken() {
        UserDefaults.standard.string(forKey = "kickbase_token")?.let { token ->
            accessToken = token
            print("🔑 Found stored token, validating...")
            Task { -> validateToken() }
        }
    }

    private fun removeStoredToken() {
        UserDefaults.standard.removeObject(forKey = "kickbase_token")
        print("🗑️ Stored token removed")
    }

    private suspend fun validateToken(): Unit = MainActor.run l@{
        val token_0 = accessToken
        if (token_0 == null) {
            return@l
        }

        apiService.setAuthToken(token_0)

        try {
            val userSettings = MainActor.run { apiService }.getUserSettings()
            print("✅ Token validation successful")

            // Try to extract user info if available
            val userData = (dict(from = userSettings["user"]) ?: dict(from = userSettings)).sref()
            if (userData != null) {
                val user = User(id = userData["id"] as? String ?: "", name = userData["name"] as? String ?: "", teamName = userData["teamName"] as? String ?: "", email = userData["email"] as? String ?: "", budget = userData["budget"] as? Int ?: 0, teamValue = userData["teamValue"] as? Int ?: 0, points = userData["points"] as? Int ?: 0, placement = userData["placement"] as? Int ?: 0, flags = userData["flags"] as? Int ?: 0)

                this.currentUser = user
            }

            this.isAuthenticated = true
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Token validation failed: ${error}")
            logout()
        }
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}

internal sealed class AuthError: Exception(), Error, LocalizedError {
    class InvalidCredentialsCase: AuthError() {
    }
    class NetworkErrorCase: AuthError() {
    }
    class InvalidResponseCase(val associated0: String): AuthError() {
    }

    override val errorDescription: String?
        get() {
            when (this) {
                is AuthError.InvalidCredentialsCase -> return "Ungültige E-Mail oder Passwort"
                is AuthError.NetworkErrorCase -> return "Netzwerkfehler. Bitte versuchen Sie es erneut."
                is AuthError.InvalidResponseCase -> {
                    val message = this.associated0
                    return "Serverantwort ungültig: ${message}"
                }
            }
        }

    @androidx.annotation.Keep
    companion object {
        val invalidCredentials: AuthError
            get() = InvalidCredentialsCase()
        val networkError: AuthError
            get() = NetworkErrorCase()
        fun invalidResponse(associated0: String): AuthError = InvalidResponseCase(associated0)
    }
}
