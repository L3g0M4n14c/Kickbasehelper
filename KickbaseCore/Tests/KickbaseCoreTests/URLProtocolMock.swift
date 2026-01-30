import Foundation

final class URLProtocolMock: URLProtocol {
    static var requestHandler: ((URLRequest) throws -> (HTTPURLResponse, Data))?
    /// Optional delay to simulate slow responses (seconds)
    static var responseDelay: TimeInterval = 0

    override class func canInit(with request: URLRequest) -> Bool {
        return true
    }

    override class func canonicalRequest(for request: URLRequest) -> URLRequest {
        return request
    }

    override func startLoading() {
        guard let handler = URLProtocolMock.requestHandler else {
            fatalError("Request handler not set")
        }

        let deliver: () -> Void = {
            do {
                let (response, data) = try handler(self.request)
                self.client?.urlProtocol(
                    self, didReceive: response, cacheStoragePolicy: .notAllowed)
                self.client?.urlProtocol(self, didLoad: data)
                self.client?.urlProtocolDidFinishLoading(self)
            } catch {
                self.client?.urlProtocol(self, didFailWithError: error)
            }
        }

        if URLProtocolMock.responseDelay > 0 {
            DispatchQueue.global().asyncAfter(deadline: .now() + URLProtocolMock.responseDelay) {
                deliver()
            }
        } else {
            deliver()
        }
    }

    override func stopLoading() {
        // no-op
    }
}
