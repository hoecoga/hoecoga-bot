package hoecoga.websocket

import java.net.URI

import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import play.api.libs.json.{JsValue, Json}

/**
 * A web socket client.
 */
class Client(
  uri: URI, 
  open: ServerHandshake => Unit, 
  error: Exception => Unit, 
  close: (Int, String, Boolean) => Unit, 
  receive: JsValue => Unit
) extends WebSocketClient(uri) {
  
  override def onOpen(handshake: ServerHandshake): Unit = {
    open(handshake)
  }

  override def onError(ex: Exception): Unit = {
    error(ex)
  }

  override def onMessage(message: String): Unit = {
    receive(Json.parse(message))
  }

  override def onClose(code: Int, reason: String, remote: Boolean): Unit = {
    close(code, reason, remote)
  }
}
