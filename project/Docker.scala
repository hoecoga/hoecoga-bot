import sbt.Keys._
import sbt._

object Docker {
  val docker = taskKey[Unit]("docker")

  val dockerSettings = Seq(docker := {
    val stream = streams.value
    val dir = "target/docker"
    val tag = "hoecoga/hoecoga-bot"

    def target(file: File) = new File(dir, file.getName)

    def copy(file: File) = {
      val t = target(file)
      stream.log.info(s"Copy $file to $t")
      IO.copyFile(file, t)
    }

    val conf = new File("conf/application.conf")
    val logback = new File("conf/logback.xml")
    val dockerfile = new File("Dockerfile")
    val jar = sbtassembly.AssemblyKeys.assembly.value

    IO.createDirectory(new File(dir))
    copy(conf)
    copy(logback)
    copy(jar)
    copy(dockerfile)

    def run(cmd: String) = {
      stream.log.info(s"Run: $cmd")
      cmd.!
    }
    run(s"docker build -t $tag $dir")
    run(s"docker run -d $tag")
  })
}
