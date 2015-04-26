package hoecoga

import java.net.URL
import java.util.TimeZone
import java.util.concurrent.TimeUnit

import hoecoga.Config.{Scheduler, Slack}

import scala.collection.JavaConversions._
import scala.concurrent.duration._

class Config(config: com.typesafe.config.Config) {
  val slack: Slack = new Slack(config.getConfig("slack"))
  val scheduler: Scheduler = new Scheduler(config.getConfig("scheduler"))

  /**
   * The shutdown hook timer.
   */
  val awaitTermination: FiniteDuration = config.getDuration("await-termination", TimeUnit.SECONDS).seconds
}

object Config {
  class Slack(config: com.typesafe.config.Config) {
    /**
     * The slack base url.
     */
    val baseUrl: URL = new URL(config.getString("base-url"))

    /**
     * The slack bot user token.
     */
    val token: String = config.getString("token")

    /**
     * The interval to connect to the slack server on the reconnection.
     */
    val reconnectInterval: FiniteDuration = config.getDuration("reconnect-interval", TimeUnit.SECONDS).seconds

    /**
     * The slack channel names to ignore incoming message and to prohibit any response to the channels.
     */
    val ignoredChannels: List[String] = config.getStringList("ignored-channels").toList

    /**
     * The interval to send ping/pong rtm message for websocket keep alive.
     */
    val keepAliveInterval: FiniteDuration = config.getDuration("keep-alive-interval", TimeUnit.SECONDS).seconds
  }

  class Scheduler(config: com.typesafe.config.Config) {
    /**
     * The scheduler timezone.
     */
    val timeZone: TimeZone = TimeZone.getTimeZone(config.getString("timezone"))
  }
}
