package lila.analyse

import chess.Color

import org.joda.time.DateTime

case class Analysis(
    id: String,
    infos: List[Info],
    startPly: Int,
    uid: Option[String], // requester lichess ID
    by: Option[String],  // analyser lichess ID
    date: DateTime
) {

  type InfoAdvices = List[(Info, Option[Advice])]

  lazy val infoAdvices: InfoAdvices = {
    (Info.start(startPly) :: infos) sliding 2 collect { case List(prev, info) =>
      info -> {
        if (info.hasVariation) Advice(prev, info) else None
      }
    }
  }.toList

  lazy val advices: List[Advice] = infoAdvices.flatMap(_._2)

  // ply -> UCI
  def bestMoves: Map[Int, String] =
    infos.view.flatMap { i =>
      i.best map { b =>
        i.ply -> b.keys
      }
    }.toMap

  def summary: List[(Color, List[(Advice.Judgment, Int)])] = Color.all map { color =>
    color -> (Advice.Judgment.all map { judgment =>
      judgment -> (advices count { adv =>
        adv.color == color && adv.judgment == judgment
      })
    })
  }

  def valid = infos.nonEmpty

  def nbEmptyInfos       = infos.count(_.isEmpty)
  def emptyRatio: Double = nbEmptyInfos.toDouble / infos.size
}

object Analysis {

  import lila.db.BSON
  import reactivemongo.api.bson._

  type ID = String

  given BSON[Analysis] = new BSON[Analysis] {
    def reads(r: BSON.Reader) = {
      val startPly = r intD "ply"
      val raw      = r str "data"
      Analysis(
        id = r str "_id",
        infos = Info.decodeList(raw, startPly) getOrElse {
          sys error s"Invalid analysis data $raw"
        },
        startPly = startPly,
        uid = r strO "uid",
        by = r strO "by",
        date = r date "date"
      )
    }
  }
}
