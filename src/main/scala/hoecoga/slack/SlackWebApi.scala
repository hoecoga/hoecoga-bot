package hoecoga.slack

import java.net.{URI, URL}

import hoecoga.Config
import play.api.libs.json.{JsValue, Json}

import scalaj.http.Http

/**
 * @see [[https://api.slack.com/methods]]
 */
class SlackWebApi(config: Config) {
  private[this] def request(path: String, params: (String, String)*): JsValue = {
    val http = (params.toList :+ ("token" -> config.slack.token)).foldRight(Http(new URL(config.slack.baseUrl, path).toString)) {
      case ((key, value), req) =>
        req.param(key, value)
    }
    Json.parse(http.asBytes.body)
  }

  /**
   * @see [[https://api.slack.com/methods/channels.info]]
   */
  def info(channel: SlackChannel): String = {
    val path = "/api/channels.info"
    val json = request(path, "channel" -> channel.id)
    val name = (json \ "channel" \ "name").as[String]
    name
  }

  /**
   * @see [[https://api.slack.com/methods/rtm.start]]
   */
  def start(): (URI, SlackUser) = {
    val path = "/api/rtm.start"
    val json = request(path)
    val url = (json \ "url").as[String]
    val selfId = (json \ "self" \ "id").as[String]
    (new URI(url), SlackUser(selfId))
  }
}
