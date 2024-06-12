package lila

export scalalib.newtypes.{ *, given }
export scalalib.zeros.given
export scalalib.extensions.{ *, given }
export scalalib.time.*

export cats.syntax.all.*
export cats.{ Eq, Show }
export cats.data.NonEmptyList
export java.time.{ Instant, LocalDateTime }

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
