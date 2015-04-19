package hoecoga.websocket

import java.net.URI
import javax.net.ssl.SSLContext

import org.java_websocket.client.DefaultSSLWebSocketClientFactory
import org.java_websocket.handshake.ServerHandshake
import play.api.libs.json.JsValue

/**
 * A [[Client]] factory for tls websocket connections.
 */
trait ClientFactory {
  def wss(uri: URI,
          open: ServerHandshake => Unit,
          error: Exception => Unit,
          close: (Int, String, Boolean) => Unit,
          receive: JsValue => Unit): Client = {
    val client = new Client(uri = uri, open = open, error = error, close = close, receive = receive)
    val ssl = SSLContext.getInstance("TLS")
    ssl.init(null, null, null)
    client.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(ssl))
    client.connect()
    client
  }
}
