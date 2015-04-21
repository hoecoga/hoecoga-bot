package hoecoga

import java.net.URI
import java.util.concurrent.atomic.{AtomicInteger, AtomicReference}
import java.util.{TimeZone, UUID}

import akka.actor.{ActorSystem, PoisonPill}
import akka.testkit.{ImplicitSender, TestKit}
import com.google.inject.{AbstractModule, Guice}
import hoecoga.SlackChannelActor.ChannelName
import hoecoga.core.ArbitraryHelper
import hoecoga.scheduler.{JobData, SlackJob}
import hoecoga.slack._
import hoecoga.websocket.{Client, ClientFactory}
import org.java_websocket.handshake.ServerHandshake
import org.mockito.Mockito._
import org.mockito.{ArgumentMatcher, Matchers}
import org.scalatest.mock.MockitoSugar
import org.scalatest.{BeforeAndAfterAll, FunSpecLike}
import play.api.libs.json.{JsObject, JsString, JsValue, Json}

import scala.concurrent.duration._

class SlackActorSpec(_system: ActorSystem)
  extends TestKit(_system) with ImplicitSender with FunSpecLike with BeforeAndAfterAll with SlackActorSpecHelper {

  def this() = this(PersistentActorHelper.createActorSystem())

  override def afterAll(): Unit =  {
    TestKit.shutdownActorSystem(system)
  }

  describe("SlackActor") {
    it("keep alive") {
      test(keepAliveInterval = 1.seconds) { settings =>
        import settings._

        Thread.sleep(1000)
        verify(client, times(1)).send(Matchers.argThat(new PingMessageMatcher))

        Thread.sleep(1000)
        verify(client, times(1)).close()
      }

      test(keepAliveInterval = 1.seconds) { settings =>
        import settings._

        Thread.sleep(1000)
        verify(client, times(1)).send(Matchers.argThat(new PingMessageMatcher))

        receive(Json.toJson(PongEvent(1)))

        Thread.sleep(1000)
        verify(client, never()).close()
      }

      test(keepAliveInterval = 1.seconds) { settings =>
        import settings._

        Thread.sleep(1000)
        verify(client, times(1)).send(Matchers.argThat(new PingMessageMatcher))

        receive(Json.obj())

        Thread.sleep(1000)
        verify(client, never()).close()
      }
    }

    it("ping") {
      test(keepAliveInterval = 100.seconds) { settings =>
        import settings._

        val ignored = ignoredChannel()
        val accepted = acceptedChannel()

        def ping(channel: SlackChannel) = message(channel, "ping")

        ping(ignored.id)

        Thread.sleep(1000)
        verify(client, never()).send(Matchers.any[String])

        ping(accepted.id)

        Thread.sleep(1000)
        verify(client, times(1)).send(Matchers.any[String])
        verify(client, times(1)).send(Matchers.argThat(new SimpleMessageMatcher(accepted.id, "pong")))
      }
    }

    it("cron") {
      test(keepAliveInterval = 100.seconds) { settings =>
        import settings._

        val channel1, channel2 = acceptedChannel().id

        def cron(channel: SlackChannel, args: String) = message(channel, s"cron $args")

        var counter = 0
        def expect(channel: SlackChannel, text: String) = {
          counter = counter + 1
          Thread.sleep(1000)
          verify(client, times(1)).send(Matchers.argThat(new SimpleMessageMatcher(channel, text)))
          verify(client, times(counter)).send(Matchers.any[String])
        }

        def expectNextJobId(channel: SlackChannel, text: String => String): String = {
          counter = counter + 1
          Thread.sleep(1000)
          val lastJobId = settings.jobId.getAndSet(UUID.randomUUID().toString)
          verify(client, times(1)).send(Matchers.argThat(new SimpleMessageMatcher(channel, text(lastJobId))))
          verify(client, times(counter)).send(Matchers.any[String])
          lastJobId
        }

        cron(channel1, "list")
        expect(channel1, "0 job(s) found")

        cron(channel1, "create 0 * * * * * hi there!")
        expect(channel1, s"CronExpression '0 * * * * *' is invalid.")

        cron(channel1, "create 0 * * * * ? hi there!")
        val lastJobId = expectNextJobId(channel1, id => s"job $id created")

        cron(channel1, "list")
        expect(channel1, s"1 job(s) found\nid=$lastJobId, trigger=0 * * * * ?, data=hi there!")

        cron(channel2, s"delete $lastJobId")
        expect(channel2, s"job $lastJobId not found")

        cron(channel1, s"delete $lastJobId")
        expect(channel1, s"job $lastJobId deleted")

        cron(channel1, "create 0 * * * * ? <http://example.com>")
        val jobId1 = expectNextJobId(channel1, id => s"job $id created")

        cron(channel1, "list")
        expect(channel1, s"1 job(s) found\nid=$jobId1, trigger=0 * * * * ?, data=http://example.com")

        val user = sample[SlackUser]
        val channel = sample[SlackChannel]
        cron(channel2, s"create 0 * * * * ? <@${user.id}>: <#${channel.id}>")
        val jobId2 = expectNextJobId(channel2, id => s"job $id created")

        cron(channel2, "list")
        expect(channel2, s"1 job(s) found\nid=$jobId2, trigger=0 * * * * ?, data=<@${user.id}>: <#${channel.id}>")
      }
    }
  }
}

trait SlackActorSpecHelper extends ArbitraryHelper with MockitoSugar {
  class SimpleMessageMatcher(channel: SlackChannel, text: String) extends ArgumentMatcher[String] {
    override def matches(argument: scala.Any): Boolean = {
      val a = Json.parse(argument.asInstanceOf[String]).as[SimpleMessage]
      a.channel == channel && a.text == text
    }
  }

  class PingMessageMatcher extends ArgumentMatcher[String] {
    override def matches(argument: scala.Any): Boolean = {
      Json.parse(argument.asInstanceOf[String]).asOpt[PingMessage].isDefined
    }
  }

  case class Channel(id: SlackChannel, name: ChannelName)

  case class SlackActorSpecSettings(
    api: SlackWebApi,
    client: Client,
    receive: JsValue => Unit,
    message: (SlackChannel, String) => Unit,
    ignoredChannel: () => Channel,
    acceptedChannel: () => Channel,
    jobId: AtomicReference[String]
  )

  def test(keepAliveInterval: FiniteDuration)(f: SlackActorSpecSettings => Unit)(implicit system: ActorSystem): Unit = {
    val api = mock[SlackWebApi]

    val channels = scala.collection.mutable.Map.empty[SlackChannel, ChannelName]
    def newChannel() = {
      val id = Stream.continually(sample[SlackChannel]).filter(!channels.keySet.contains(_)).head
      val name = Stream.continually(sample[ChannelName]).filter(!channels.valuesIterator.contains(_)).head
      channels += id -> name
      when(api.info(id)).thenReturn(name.name)
      Channel(id, name)
    }

    val uri = new URI(s"wss://example.com/${UUID.randomUUID().toString}")
    val ignoredChannels = Stream.continually(newChannel()).take(100).toList
    val client = mock[Client]
    val messageBus = new SimpleMessageEventBus
    val schedulerBus = new SchedulerEventBus
    val bot = sample[SlackUser]
    val jobId = new AtomicReference[String](UUID.randomUUID().toString)

    val send = new AtomicReference[(JsValue) => Unit]()

    when(api.start()).thenReturn((uri, bot))

    val factory = new ClientFactory {
      override def wss(uri: URI,
                       open: (ServerHandshake) => Unit,
                       error: (Exception) => Unit,
                       close: (Int, String, Boolean) => Unit,
                       receive: (JsValue) => Unit): Client = {
        send.set(receive)
        client
      }
    }

    val slack = {
      val settings = SlackActor.SlackActorSettings(
        keepAliveInterval = keepAliveInterval,
        ignoredChannels = ignoredChannels.map(_.name.name),
        reconnectInterval = 10.seconds,
        api = api,
        factory = factory,
        simpleMessageBus = messageBus,
        schedulerBus = schedulerBus)
      system.actorOf(SlackActor.props(settings))
    }

    val scheduler = {
      val injector = Guice.createInjector(new AbstractModule {
        override def configure(): Unit = {
          bind(classOf[SlackJob]).toInstance(new SlackJob {
            override def tell(a: JobData): Unit = {}
          })
        }
      })
      val settings = SchedulerActor.SchedulerActorSettings(
        newJobId = {() => jobId.get()},
        injector = injector,
        timeZone = TimeZone.getDefault,
        bus = schedulerBus)
      system.actorOf(SchedulerActor.props(settings))
    }

    def receive(json: JsValue): Unit = send.get()(json)

    def message(channel: SlackChannel, text: String): Unit = {
      val event = Json.toJson(sample[MessageEvent].copy(channel = channel, user = sample[SlackUser], text = s"<@${bot.id}>: $text"))
      receive(event.as[JsObject] + ("type" -> JsString("message")))
    }

    val count = new AtomicInteger(0)
    def ignoredChannel() = ignoredChannels(count.getAndIncrement)

    def acceptedChannel() = newChannel()

    Thread.sleep(500)

    try {
      f(SlackActorSpecSettings(api, client, receive, message, ignoredChannel, acceptedChannel, jobId))
    } finally {
      slack ! PoisonPill
      scheduler ! PoisonPill
    }
  }
}
