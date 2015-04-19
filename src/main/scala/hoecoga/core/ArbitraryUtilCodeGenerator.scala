package hoecoga.core

/**
 * $ sbt console
 * scala> hoecoga.core.ArbitraryUtilCodeGenerator.code("hoecoga.core")
 */
object ArbitraryUtilCodeGenerator {
  private[this] def upper(index: Int): String = ('A' to 'Z')(index).toString

  private[this] def lower(index: Int): String = ('a' to 'z')(index).toString

  private[this] def types(size: Int): String = (0 to size).map(upper).mkString(", ")

  private[this] def args(size: Int): String =
    s"""(apply: (${types(size - 1)}) => ${upper(size)})"""

  private[this] def aImplicit(index: Int) = s"""a${lower(index)}: Arbitrary[${upper(index)}]"""

  private[this] def implicits(size: Int): String = s"""(implicit ${(0 to size).map(i => aImplicit(i)).mkString(", ")})"""

  private[this] def genName(index: Int) = s"""g${lower(index)}"""

  private[this] def gen(index: Int) = s"""${genName(index)} <- a${lower(index)}.arbitrary"""

  private[this] def arbitrary(size: Int): String =
    s"""  def arbitrary[${types(size)}]${args(size)}${implicits(size - 1)}: Arbitrary[${upper(size)}] = {
       |    Arbitrary {
       |      val tuple = for {
       |        ${(0 to size - 1).map(gen).mkString("; ")}
       |      } yield (${(0 to size - 1).map(genName).mkString(", ")})
       |      tuple.map(apply.tupled)
       |    }
       |  }
     """.stripMargin

  def code(packageName: String): String =
    s"""package $packageName
        |
        |import org.scalacheck.Arbitrary
        |
        |trait ArbitraryUtil {
        |  ${(2 to 10).map(arbitrary).mkString("\n")}
        |}
        |
        |object ArbitraryUtil extends ArbitraryUtil
     """.stripMargin
}
