package ccr.net.rcon

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.assertEquals

/**
 * Integration tests for RconServer using real UDP on localhost.
 * Client uses DatagramSocket (supports SO_TIMEOUT); server runs in IO scope.
 */
class RconServerTest {

    private lateinit var scope: CoroutineScope
    private lateinit var client: DatagramSocket

    @BeforeEach
    fun setup() {
        scope = CoroutineScope(Dispatchers.IO)
        client = DatagramSocket().apply { soTimeout = 3_000 }
    }

    @AfterEach
    fun teardown() {
        client.close()
        scope.cancel()
    }

    // ---- Helpers ----

    private fun startServer(
        port: Int,
        password: String = "",
        remoteAdminAllowed: Boolean = true,
        welcomeMessage: String = "",
        commandHandler: (String) -> String = { "response: $it" },
    ) {
        val server = RconServer(
            port = port,
            password = password,
            remoteAdminAllowed = remoteAdminAllowed,
            welcomeMessage = welcomeMessage,
            commandHandler = commandHandler,
        )
        scope.launch { server.run() }
        Thread.sleep(100) // give the server time to bind
    }

    private fun sendRcon(dest: InetSocketAddress, message: String, password: String = "") {
        val msgBytes = message.toByteArray(Charsets.US_ASCII)
        val payload = ByteArray(4 + msgBytes.size + 1)
        ByteBuffer.wrap(payload).order(ByteOrder.LITTLE_ENDIAN).putInt(0) // REQUEST
        System.arraycopy(msgBytes, 0, payload, 4, msgBytes.size)
        payload[4 + msgBytes.size] = 0

        val key = deriveKey(password)
        val encrypted = encrypt(payload, key)
        val crc = computeCrc(encrypted)

        val packet = ByteArray(4 + encrypted.size)
        ByteBuffer.wrap(packet).order(ByteOrder.LITTLE_ENDIAN).putInt(crc)
        System.arraycopy(encrypted, 0, packet, 4, encrypted.size)

        client.send(DatagramPacket(packet, packet.size, dest))
    }

    private fun receiveRcon(password: String = ""): String {
        val dp = DatagramPacket(ByteArray(1024), 1024)
        client.receive(dp) // blocks with 3s timeout

        val raw = dp.data.copyOf(dp.length)
        if (raw.size < 5) return ""

        val payload = raw.copyOfRange(4, raw.size)
        val key = deriveKey(password)
        val decrypted = decrypt(payload, key)

        if (decrypted.size < 4) return ""
        val msgType = ByteBuffer.wrap(decrypted, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        if (msgType != 1) return "<not response>"

        val msgBytes = decrypted.copyOfRange(4, decrypted.size)
        val nullIdx = msgBytes.indexOfFirst { it == 0.toByte() }
        return if (nullIdx >= 0) String(msgBytes, 0, nullIdx, Charsets.US_ASCII)
        else String(msgBytes, Charsets.US_ASCII)
    }

    // ---- Tests ----

    @Test
    fun `full auth flow with password`() {
        val port = 43001
        startServer(port, password = "secret")
        val dest = InetSocketAddress("127.0.0.1", port)

        sendRcon(dest, "CONNECT", "secret")
        assertEquals("Password required:", receiveRcon("secret"))

        sendRcon(dest, "secret", "secret")
        assertEquals("Password accepted.\n", receiveRcon("secret"))
    }

    @Test
    fun `no-password mode grants immediate auth on CONNECT`() {
        val port = 43002
        startServer(port, password = "")
        val dest = InetSocketAddress("127.0.0.1", port)

        sendRcon(dest, "CONNECT")
        assertEquals("Password accepted.\n", receiveRcon())
    }

    @Test
    fun `command dispatch after auth`() {
        val port = 43003
        startServer(port, password = "", commandHandler = { cmd -> "echo: $cmd" })
        val dest = InetSocketAddress("127.0.0.1", port)

        sendRcon(dest, "CONNECT")
        receiveRcon() // consume "Password accepted.\n"

        sendRcon(dest, "hello")
        assertEquals("echo: hello", receiveRcon())
    }

    @Test
    fun `BYE disconnects session`() {
        val port = 43004
        startServer(port, password = "")
        val dest = InetSocketAddress("127.0.0.1", port)

        sendRcon(dest, "CONNECT")
        receiveRcon() // consume accepted

        sendRcon(dest, "BYE")
        assertEquals("Goodbye!\n", receiveRcon())
    }

    @Test
    fun `response chunking for long responses`() {
        val port = 43005
        val longResponse = "X".repeat(1200)
        startServer(port, password = "", commandHandler = { longResponse })
        val dest = InetSocketAddress("127.0.0.1", port)

        sendRcon(dest, "CONNECT")
        receiveRcon() // consume accepted

        sendRcon(dest, "bigcmd")

        // Expect 3 chunks: 499 + 499 + 202
        val chunk1 = receiveRcon()
        val chunk2 = receiveRcon()
        val chunk3 = receiveRcon()

        assertEquals(499, chunk1.length)
        assertEquals(499, chunk2.length)
        assertEquals(202, chunk3.length)
        assertEquals(longResponse, chunk1 + chunk2 + chunk3)
    }

    @Test
    fun `welcome message sent after auth`() {
        val port = 43006
        startServer(port, password = "", welcomeMessage = "Welcome to CCR!")
        val dest = InetSocketAddress("127.0.0.1", port)

        sendRcon(dest, "CONNECT")
        assertEquals("Password accepted.\n", receiveRcon())
        assertEquals("Welcome to CCR!", receiveRcon())
    }
}
