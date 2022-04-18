import ornicar.scalalib.ScalalibExtensions

package object lila extends ScalalibExtensions:

  def parseIntOption(str: String): Option[Int] =
    try Some(java.lang.Integer.parseInt(str))
    catch case e: NumberFormatException => None

  import scala.util.Try
  implicit final class LilaPimpedTryList[A](list: List[Try[A]]):
    def sequence: Try[List[A]] =
      (Try(List[A]()) /: list) { (a, b) =>
        a flatMap (c => b map (d => d :: c))
      } map (_.reverse)
  implicit final class LilaPimpedOptionList[A](list: List[Option[A]]):
    def sequence: Option[List[A]] =
      (Option(List[A]()) /: list) { (a, b) =>
        a flatMap (c => b map (d => d :: c))
      } map (_.reverse)
