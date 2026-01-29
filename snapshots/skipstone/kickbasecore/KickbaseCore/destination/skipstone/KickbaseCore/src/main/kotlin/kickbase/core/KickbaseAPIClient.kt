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
import skip.lib.Array

import skip.model.*
import skip.foundation.*
import skip.ui.*

@Stable
open class KickbaseAPIClient: ObservableObject {
    override val objectWillChange = ObservableObjectPublisher()
    private val baseURL = "https://api.kickbase.com"
    private var authToken: String? = null
    // Exposed as internal to allow tests to inspect the session configuration
    internal val session: URLSession

    constructor(session: URLSession = URLSession.shared) {
        this.session = SessionSanitizer.sanitized(session)
    }

    open fun setAuthToken(token: String) {
        authToken = token
        print("🔑 Auth token set for KickbaseAPIClient")
    }

    open fun hasAuthToken(): Boolean = authToken != null

    // MARK: - Generic API Request Methods

    open suspend fun makeRequest(endpoint: String, method: String = "GET", body: Data? = null): Tuple2<Data, HTTPURLResponse> = MainActor.run l@{
        val token_0 = authToken
        if (token_0 == null) {
            throw APIError.noAuthToken
        }
        val url_0 = (try { URL(string = "${baseURL}${endpoint}") } catch (_: NullReturnException) { null })
        if (url_0 == null) {
            throw APIError.invalidURL
        }

        var request = URLRequest(url = url_0)
        request.httpMethod = method
        request.setValue("Bearer ${token_0}", forHTTPHeaderField = "Authorization")
        request.setValue("application/json", forHTTPHeaderField = "Accept")
        request.setValue("application/json", forHTTPHeaderField = "Content-Type")
        request.cachePolicy = URLRequest.CachePolicy.reloadIgnoringLocalCacheData

        if (body != null) {
            request.httpBody = body
        }

        print("📤 Making ${method} request to: ${url_0}")

        val (data, response) = MainActor.run { this.session }.data(for_ = request)
        val httpResponse_0 = response as? HTTPURLResponse
        if (httpResponse_0 == null) {
            throw APIError.noHTTPResponse
        }

        print("📊 Response Status Code: ${httpResponse_0.statusCode}")

        String(data = data, encoding = StringEncoding.utf8)?.let { responseString ->
            print("📥 Response: ${responseString.prefix(500)}")
        }

        return@l Tuple2(data.sref(), httpResponse_0)
    }

    open suspend fun tryMultipleEndpoints(endpoints: Array<String>, method: String = "GET", body: Data? = null): Tuple2<Data, Dictionary<String, Any>> = MainActor.run l@{
        val token_1 = authToken
        if (token_1 == null) {
            throw APIError.noAuthToken
        }

        var lastError: Error? = null

        for ((index, endpoint) in endpoints.enumerated()) {
            try {
                val (data, httpResponse) = makeRequest(endpoint = endpoint, method = method, body = body)

                if (httpResponse.statusCode == 200) {
                    val json = jsonDict(from = data)
                    if (!json.isEmpty) {
                        print("✅ Found working endpoint (${index + 1}/${endpoints.count}): ${endpoint}")
                        return@l Tuple2(data.sref(), json.sref())
                    } else {
                        print("⚠️ Could not parse JSON from endpoint: ${endpoint}")
                        continue
                    }
                } else if (httpResponse.statusCode == 401) {
                    throw APIError.authenticationFailed
                } else if (httpResponse.statusCode == 404) {
                    print("⚠️ Endpoint ${endpoint} not found (404), trying next...")
                    continue
                } else if (httpResponse.statusCode == 403) {
                    print("⚠️ Access forbidden (403) for endpoint ${endpoint}")
                    continue
                } else if (httpResponse.statusCode >= 500) {
                    print("⚠️ Server error (${httpResponse.statusCode}) for endpoint ${endpoint}")
                    continue
                } else {
                    print("⚠️ HTTP ${httpResponse.statusCode} for endpoint ${endpoint}")
                    continue
                }
            } catch (error: Throwable) {
                @Suppress("NAME_SHADOWING") val error = error.aserror()
                lastError = error.sref()
                print("❌ Network error with endpoint ${endpoint}: ${error.localizedDescription}")
                continue
            }
        }

        if (lastError != null) {
            val error = lastError.sref()
            throw error as Throwable
        } else {
            throw APIError.allEndpointsFailed
        }
    }

    // MARK: - Network Testing

    open suspend fun testNetworkConnectivity(): Boolean = MainActor.run l@{
        print("🌐 Testing network connectivity...")

        try {
            val url = URL(string = "${baseURL}/")
            var request = URLRequest(url = url)
            request.timeoutInterval = 5.0
            request.httpMethod = "HEAD"

            val (_, response) = MainActor.run { this.session }.data(for_ = request)

            val matchtarget_0 = response as? HTTPURLResponse
            if (matchtarget_0 != null) {
                val httpResponse = matchtarget_0
                print("✅ Network test successful - Status: ${httpResponse.statusCode}")
                return@l true
            } else {
                print("⚠️ Network test - No HTTP response")
                return@l false
            }
        } catch (error: Throwable) {
            @Suppress("NAME_SHADOWING") val error = error.aserror()
            print("❌ Network test failed: ${error.localizedDescription}")
            return@l false
        }
    }

    @androidx.annotation.Keep
    companion object: CompanionClass() {
    }
    open class CompanionClass {
    }
}

// MARK: - API Errors

internal sealed class APIError: Exception(), Error, LocalizedError {
    class NoAuthTokenCase: APIError() {
    }
    class InvalidURLCase: APIError() {
    }
    class NoHTTPResponseCase: APIError() {
    }
    class AuthenticationFailedCase: APIError() {
    }
    class AllEndpointsFailedCase: APIError() {
    }
    class ParsingFailedCase: APIError() {
    }
    class NetworkErrorCase(val associated0: String): APIError() {
    }

    override val errorDescription: String?
        get() {
            when (this) {
                is APIError.NoAuthTokenCase -> return "Kein Authentifizierungstoken verfügbar"
                is APIError.InvalidURLCase -> return "Ungültige URL"
                is APIError.NoHTTPResponseCase -> return "Keine HTTP-Antwort erhalten"
                is APIError.AuthenticationFailedCase -> return "Authentifizierung fehlgeschlagen. Token möglicherweise abgelaufen."
                is APIError.AllEndpointsFailedCase -> return "Konnte keine Verbindung zur Kickbase API herstellen. Bitte überprüfen Sie Ihre Internetverbindung und versuchen Sie es später erneut."
                is APIError.ParsingFailedCase -> return "Fehler beim Verarbeiten der Server-Antwort"
                is APIError.NetworkErrorCase -> {
                    val message = this.associated0
                    return "Netzwerkfehler: ${message}"
                }
            }
        }

    @androidx.annotation.Keep
    companion object {
        val noAuthToken: APIError
            get() = NoAuthTokenCase()
        val invalidURL: APIError
            get() = InvalidURLCase()
        val noHTTPResponse: APIError
            get() = NoHTTPResponseCase()
        val authenticationFailed: APIError
            get() = AuthenticationFailedCase()
        val allEndpointsFailed: APIError
            get() = AllEndpointsFailedCase()
        val parsingFailed: APIError
            get() = ParsingFailedCase()
        fun networkError(associated0: String): APIError = NetworkErrorCase(associated0)
    }
}
