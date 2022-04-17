package lichess

import akka.stream.*
import akka.stream.scaladsl.*
import akka.stream.stage.*
import scala.concurrent.duration.*

import lila.game.Game
import org.joda.time.DateTime
import org.joda.time.format.*

object Reporter extends GraphStage[FlowShape[Option[Game.WithInitialFen], Game.WithInitialFen]]:

  val freq = 2.seconds

  val in             = Inlet[Option[Game.WithInitialFen]]("reporter.in")
  val out            = Outlet[Game.WithInitialFen]("reporter.out")
  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    private val formatter = DateTimeFormat forStyle "MS"

    private var counter                = 0
    private var prev                   = 0
    private var date: Option[DateTime] = None

    setHandler(
      in,
      new InHandler {
        override def onPush() = {
          grab(in) match {
            case Some(g) => {
              counter += 1
              date = Some(g.game.createdAt)
              push(out, g)
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
