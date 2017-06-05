import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage._

import org.joda.time.format._
import org.joda.time.DateTime
import lila.game.Game

class Reporter extends GraphStage[FlowShape[Option[Game], Game]] {
  val in = Inlet[Option[Game]]("reporter.in")
  val out = Outlet[Game]("reporter.out")
  override val shape = FlowShape.of(in, out)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new GraphStageLogic(shape) {

    private val formatter = DateTimeFormat forStyle "MS"

    private var counter = 0
    private var date: Option[DateTime] = None

    setHandler(in, new InHandler {
      override def onPush() = {
        counter += 1
        grab(in) match {
          case Some(g) => {
            date = Some(g.createdAt)
            push(out, g)
          }
          case None => {
            println(s"${date.fold("-")(formatter.print)} $counter")
            pull(in)
          }
        }
      }

      setHandler(out, new OutHandler {
        override def onPull() = {
          pull(in)
        }
      })

//       override def onUpstreamFinish(): Unit = {
//         println("finished?")
//         completeStage()
//       }
    })

  }
}
