import Foundation

enum SessionSanitizer {
    /// Returns true when running under XCTest (test environment)
    static func isRunningUnderXCTest() -> Bool {
        if ProcessInfo.processInfo.environment["XCTestConfigurationFilePath"] != nil {
            return true
        }
        // Some test runners may not set the env var; two other ways to detect XCTest
        if NSClassFromString("XCTestCase") != nil {
            return true
        }
        return false
    }

    /// If the provided session contains custom protocol classes and we are NOT running tests,
    /// returns a new URLSession with protocolClasses removed to avoid allowing untrusted
    /// code to intercept requests at runtime. Otherwise returns the original session.
    ///
    /// You can force 'production' behavior in tests by setting environment variable
    /// `KICKBASE_SANITIZER_FORCE_PRODUCTION=1` for the process (used by tests to simulate prod).
    static func sanitized(_ session: URLSession) -> URLSession {
        guard let proto = session.configuration.protocolClasses, !proto.isEmpty else {
            return session
        }

        // Force production simulation override for tests
        if ProcessInfo.processInfo.environment["KICKBASE_SANITIZER_FORCE_PRODUCTION"] == "1" {
            var config = session.configuration
            config.protocolClasses = nil
            print(
                "[SessionSanitizer] ⚠️ Forced production mode: Custom URLProtocol classes removed for safety"
            )
            return URLSession(configuration: config)
        }

        if isRunningUnderXCTest() {
            print(
                "[SessionSanitizer] Running under tests — allowing custom protocol classes for testing"
            )
            return session
        }

        var config = session.configuration
        config.protocolClasses = nil
        print(
            "[SessionSanitizer] ⚠️ Custom URLProtocol classes detected and removed for safety in production"
        )
        return URLSession(configuration: config)
    }
}
