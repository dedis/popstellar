import scala.collection.immutable.Iterable
import akka.NotUsed
import akka.http.scaladsl.model.ws.{Message, TextMessage}
import akka.stream.FlowShape
import akka.stream.scaladsl.{Flow, GraphDSL, Merge, Partition, Sink, Source}

object PublishSubscribe {
  /**
   * Create a flow that handles messages of a publish-subscribe system.
   * To publish a message: p:channel_name:message
   * To subscribe to a channel: s:channel_name
   * @param mHub a mergehub where all published messages are sent
   * @param bHub a broadcast hub that forward all published messages
   * @return a flow that handles messages of a publish-subscribe system
   */
  def getFlow(mHub: Sink[Message, NotUsed], bHub: Source[Message, NotUsed]): Flow[Message, Message, NotUsed] =
    Flow.fromGraph(GraphDSL.create() { implicit builder =>
      import GraphDSL.Implicits._

      val input = builder.add(Flow[Message])
      val merge = builder.add(Merge[Message](2))
      val output = builder.add(Flow[Message])

      val partitioner = builder.add(Partition[Message](2,
        { message: Message =>
          message match {
            case TextMessage.Strict(text) =>
              if (text.startsWith("p")) 1 else 0
          }
        }
      ))

      val filter = builder.add(Flow[Message].statefulMapConcat(getFilter()))


      input ~> partitioner
      partitioner.out(1) ~> mHub
      partitioner.out(0) ~> merge
      bHub ~> merge
      merge ~> filter ~> output
      FlowShape(input.in, output.out)
    }
    )

  /**
   *A flow that handles subscriptions and only keep published messages from channels a user is subscribed to.
   */
  def getFilter(): () => Message => Iterable[Message] = {

    () =>
      var subscribed: Set[String] = Set[String]()

      message: Message =>
        message match {
          case TextMessage.Strict(text) =>
            val split: Array[String] = text.stripLineEnd.split(':')

            if (split(0) == "s".strip) {

              subscribed += split(1)
              Nil
            }
            else if (split(0) == "p") {

              if (subscribed contains split(1)) {
                List(TextMessage.Strict(split(2)))
              }
              else {
                Nil
              }
            }
            else {
              Nil
            }
        }
  }

}
