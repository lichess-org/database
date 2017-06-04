package lila.db

import scala.collection.breakOut
import org.joda.time.DateTime
import reactivemongo.bson._
import scalaz.NonEmptyList

trait Handlers {

  implicit object BSONJodaDateTimeHandler extends BSONHandler[BSONDateTime, DateTime] {
    def read(x: BSONDateTime) = new DateTime(x.value)
    def write(x: DateTime) = BSONDateTime(x.getMillis)
  }

  implicit def nullableHandler[T, B <: BSONValue](implicit reader: BSONReader[B, T], writer: BSONWriter[T, B]): BSONHandler[BSONValue, Option[T]] = new BSONHandler[BSONValue, Option[T]] {
    private val generalizedReader = reader.asInstanceOf[BSONReader[BSONValue, T]]
    def read(bv: BSONValue): Option[T] = generalizedReader.readOpt(bv)
    def write(v: Option[T]): BSONValue = v.fold[BSONValue](BSONNull)(writer.write)
  }

  implicit def bsonArrayToListHandler[T](implicit reader: BSONReader[_ <: BSONValue, T], writer: BSONWriter[T, _ <: BSONValue]): BSONHandler[BSONArray, List[T]] = new BSONHandler[BSONArray, List[T]] {
    def read(array: BSONArray) = readStreamList(array, reader.asInstanceOf[BSONReader[BSONValue, T]])
    def write(repr: List[T]) =
      new BSONArray(repr.map(s => scala.util.Try(writer.write(s))).to[Stream])
  }

  implicit def bsonArrayToVectorHandler[T](implicit reader: BSONReader[_ <: BSONValue, T], writer: BSONWriter[T, _ <: BSONValue]): BSONHandler[BSONArray, Vector[T]] = new BSONHandler[BSONArray, Vector[T]] {
    def read(array: BSONArray) = readStreamVector(array, reader.asInstanceOf[BSONReader[BSONValue, T]])
    def write(repr: Vector[T]) =
      new BSONArray(repr.map(s => scala.util.Try(writer.write(s))).to[Stream])
  }

  private def readStreamList[T](array: BSONArray, reader: BSONReader[BSONValue, T]): List[T] =
    array.stream.filter(_.isSuccess).map { v =>
      reader.read(v.get)
    }(breakOut)

  private def readStreamVector[T](array: BSONArray, reader: BSONReader[BSONValue, T]): Vector[T] =
    array.stream.filter(_.isSuccess).map { v =>
      reader.read(v.get)
    }(breakOut)
}
