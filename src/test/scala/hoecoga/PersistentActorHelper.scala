package hoecoga

import java.io.File
import java.nio.file.{Files, Path}
import java.util.UUID

import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory

trait PersistentActorHelper {
  def tempDirectory(): Path = Files.createTempDirectory(UUID.randomUUID().toString)
  
  private[this] def config(temp: Path) = {
    val journal = new File(temp.toAbsolutePath.toString, "journal").toString
    val snapshots = new File(temp.toAbsolutePath.toString, "snapshots").toString
    ConfigFactory.parseString(
      s"""akka.persistence.journal.leveldb.dir = "$journal"
         |akka.persistence.snapshot-store.local.dir = "$snapshots"
         |akka {
         |  loggers = ["akka.event.slf4j.Slf4jLogger"]
         |  loglevel = "DEBUG"
         |}
       """.stripMargin)
  }
  
  def createActorSystem(temp: Path) = ActorSystem(UUID.randomUUID().toString, config(temp))

  def createActorSystem() = ActorSystem(UUID.randomUUID().toString, config(tempDirectory()))
}

object PersistentActorHelper extends PersistentActorHelper
