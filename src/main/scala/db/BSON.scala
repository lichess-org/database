package lila.db

import alleycats.Zero
import dsl.*
import org.joda.time.DateTime
import ornicar.scalalib.Zero
import reactivemongo.api.bson.*
import scala.util.Try

abstract class BSON[T] extends BSONReadOnly[T] with BSONDocumentReader[T] with BSONDocumentWriter[T] {

  val logMalformed = true

  import BSON.*

  def write(obj: T): Bdoc = ???

  def writeTry(obj: T): Try[Bdoc] = ???
}

abstract class BSONReadOnly[T] extends BSONDocumentReader[T] {

  import BSON.*

  def reads(reader: Reader): T

  def readDocument(doc: Bdoc) =
    Try {
      reads(new Reader(doc))
    }

  def read(doc: Bdoc) = readDocument(doc).get
}

object BSON extends Handlers {

  final class Reader(val doc: Bdoc) {

    def get[A: BSONReader](k: String): A =
      doc.getAsTry[A](k).get
    def getO[A: BSONReader](k: String): Option[A] =
      doc.getAsOpt[A](k)
    def getD[A](k: String)(using zero: Zero[A], reader: BSONReader[A]): A =
      doc.getAsOpt[A](k) getOrElse zero.zero
    def getD[A: BSONReader](k: String, default: => A): A =
      doc.getAsOpt[A](k) getOrElse default
    def getsD[A: BSONReader](k: String) =
      doc.getAsOpt[List[A]](k) getOrElse Nil

    def str(k: String)                         = get[String](k)(BSONStringHandler)
    def strO(k: String)                        = getO[String](k)(BSONStringHandler)
    def strD(k: String)                        = strO(k) getOrElse ""
    def int(k: String)                         = get[Int](k)
    def intO(k: String)                        = getO[Int](k)
    def intD(k: String)                        = intO(k) getOrElse 0
    def double(k: String)                      = get[Double](k)
    def doubleO(k: String)                     = getO[Double](k)
    def floatO(k: String)                      = getO[Float](k)
    def bool(k: String)                        = get[Boolean](k)
    def boolO(k: String)                       = getO[Boolean](k)
    def boolD(k: String)                       = boolO(k) getOrElse false
    def date(k: String)                        = get[DateTime](k)
    def dateO(k: String)                       = getO[DateTime](k)
    def dateD(k: String, default: => DateTime) = getD(k, default)
    def bytes(k: String)                       = get[ByteArray](k)
    def bytesO(k: String)                      = getO[ByteArray](k)
    def bytesD(k: String)                      = bytesO(k) getOrElse ByteArray.empty
    def nInt(k: String)                        = get[BSONNumberLike](k).toInt.get
    def nIntO(k: String): Option[Int]          = getO[BSONNumberLike](k) flatMap (_.toInt.toOption)
    def nIntD(k: String): Int                  = nIntO(k) getOrElse 0
    def intsD(k: String)                       = getO[List[Int]](k) getOrElse Nil
    def strsD(k: String)                       = getO[List[String]](k) getOrElse Nil

    def contains = doc.contains _

    def debug = BSON debug doc
  }

  def debug(v: BSONValue): String = v match {
    case d: Bdoc        => debugDoc(d)
    case d: Barr        => debugArr(d)
    case BSONString(x)  => x
    case BSONInteger(x) => x.toString
    case BSONDouble(x)  => x.toString
    case BSONBoolean(x) => x.toString
    case v              => v.toString
  }
  def debugArr(doc: Barr): String = doc.values.toList.map(debug).mkString("[", ", ", "]")
  def debugDoc(doc: Bdoc): String =
    (doc.elements.toList map {
      case BSONElement(k, v) => s"$k: ${debug(v)}"
      case x                 => x.toString
    }).mkString("{", ", ", "}")

  def hashDoc(doc: Bdoc): String = debugDoc(doc).replace(" ", "")
}
