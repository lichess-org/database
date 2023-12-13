package lichess

import akka.stream.*
import akka.stream.stage.*
import scala.concurrent.duration.*
import java.time.format.{ DateTimeFormatter, FormatStyle }
import java.time.Instant
import ornicar.scalalib.time.*

import lila.game.Game

object Reporter:

  val freq = 2.seconds

  val graph = new GraphStage[FlowShape[Option[Seq[Game.WithInitialFen]], Seq[Game.WithInitialFen]]]:

    val in             = Inlet[Option[Seq[Game.WithInitialFen]]]("reporter.in")
    val out            = Outlet[Seq[Game.WithInitialFen]]("reporter.out")
    override val shape = FlowShape.of(in, out)

    override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

      private val formatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)

      private var counter               = 0
      private var prev                  = 0
      private var date: Option[Instant] = None

      setHandler(
        in,
        new InHandler {
          override def onPush() = {
            grab(in) match {
              case Some(gs) => {
                counter += gs.size
                date = gs.headOption.map(_.game.createdAt) orElse date
                push(out, gs)
              }
              case None => {
                val gps = (counter - prev) / freq.toSeconds
                println(s"${date.fold("-")(formatter.print)} $counter $gps/s")
                prev = counter
                pull(in)
              }
            }
          }

          setHandler(
            out,
            new OutHandler {
              override def onPull() = {
                pull(in)
              }
            }
          )

          //       override def onUpstreamFinish(): Unit = {
          //         println("finished?")
          //         completeStage()
          //       }
        }
      )

    }
