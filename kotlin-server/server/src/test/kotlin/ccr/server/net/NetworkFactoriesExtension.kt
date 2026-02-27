package ccr.server.net

import org.junit.jupiter.api.extension.BeforeAllCallback
import org.junit.jupiter.api.extension.ExtensionContext

// JUnit 5 extension that registers all network object factories before each test class runs.
// Auto-discovered via META-INF/services when junit.jupiter.extensions.autodetection.enabled=true.
class NetworkFactoriesExtension : BeforeAllCallback {
    override fun beforeAll(context: ExtensionContext) {
        NetworkObjectFactories.register()
    }
}
