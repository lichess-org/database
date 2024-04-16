package lila

export ornicar.scalalib.newtypes.{ *, given }
export ornicar.scalalib.zeros.given
export ornicar.scalalib.extensions.{ *, given }
export ornicar.scalalib.time.*

export cats.syntax.all.*
export cats.{ Eq, Show }
export cats.data.NonEmptyList
export java.time.{ Instant, LocalDateTime }

inline def nowMillis: Long = System.currentTimeMillis()
inline def nowSeconds: Int = (nowMillis / 1000).toInt

import scala.util.Try
implicit final class LilaPimpedTryList[A](list: List[Try[A]]):
  def sequence: Try[List[A]] =
    (Try(List[A]()) /: list) { (a, b) =>
      a.flatMap(c => b.map(d => d :: c))
    }.map(_.reverse)
implicit final class LilaPimpedOptionList[A](list: List[Option[A]]):
  def sequence: Option[List[A]] =
    (Option(List[A]()) /: list) { (a, b) =>
      a.flatMap(c => b.map(d => d :: c))
    }.map(_.reverse)
