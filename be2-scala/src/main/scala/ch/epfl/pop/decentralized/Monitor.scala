package ch.epfl.pop.decentralized

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Timers}
import akka.event.LoggingReceive
import akka.stream.scaladsl.Sink
import ch.epfl.pop.decentralized.Monitor.TriggerHeartbeat
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.{GetMessagesById, Heartbeat}
import ch.epfl.pop.pubsub.graph.GraphMessage

import scala.concurrent.duration.{DurationInt, FiniteDuration}

//This actor is tasked with scheduling heartbeats.
// To that end it sees every messages the system receives.
// When a message is seen it schedule a heartbeat in the next MESSAGE_DELAY seconds.
// Periodic heartbeats are sent with a period of PERIODIC_HEARTBEAT seconds.
final case class Monitor(
    heartbeatGenRef: ActorRef,
    PERIODIC_HEARTBEAT: FiniteDuration,
    MESSAGE_DELAY: FiniteDuration
)(implicit system: ActorSystem) extends Actor with ActorLogging with Timers {
  import system.dispatcher

  // Background scheduled periodic job, None until a server is connected
  private val periodicHbKey = 0
  private val singleHbKey = 1
  private var connectionActorRef = ActorRef.noSender

  override def receive: Receive = LoggingReceive {

    case Monitor.AtLeastOneServerConnected =>
      connectionActorRef = sender()
      if (!timers.isTimerActive(periodicHbKey))
        timers.startTimerWithFixedDelay(periodicHbKey, TriggerHeartbeat, PERIODIC_HEARTBEAT)

    case Monitor.NoServerConnected =>
      timers.cancelAll()

    case Monitor.TriggerHeartbeat =>
      log.info("triggering a heartbeat")
      timers.cancel(singleHbKey)
      heartbeatGenRef ! Monitor.GenerateAndSendHeartbeat(connectionActorRef)

    case Right(jsonRpcRequest: JsonRpcRequest) =>
      jsonRpcRequest.getParams match {

        case _: Heartbeat       => /* Actively ignoring this specific message */
        case _: GetMessagesById => /* Actively ignoring this specific message */
        // For any other message, we schedule a single heartbeat to reduce messages propagation delay
        case _ =>
          if (!timers.isTimerActive(singleHbKey) && timers.isTimerActive(periodicHbKey)) {
            log.info("Scheduling single heartbeat")
            timers.startSingleTimer(singleHbKey, TriggerHeartbeat, MESSAGE_DELAY)
          }
      }

    case _ => /* DO NOTHING */
  }
}

object Monitor {
  def props(heartbeatGenRef: ActorRef, PERIODIC_HEARTBEAT: FiniteDuration = 30.seconds, MESSAGE_DELAY: FiniteDuration = 3.seconds)(implicit system: ActorSystem): Props =
    Props(new Monitor(heartbeatGenRef, PERIODIC_HEARTBEAT, MESSAGE_DELAY)(system))

  def sink(monitorRef: ActorRef): Sink[GraphMessage, NotUsed] = {
    Sink.actorRef(
      monitorRef,
      DoNothing(),
      {
        _: Throwable => /* Do nothing */
      }
    )
  }

  sealed trait Event
  final case class AtLeastOneServerConnected() extends Event
  final case class NoServerConnected() extends Event
  final case class GenerateAndSendHeartbeat(connectionMediatorRef: ActorRef) extends Event
  final case class TriggerHeartbeat() extends Event
  private final case class DoNothing() extends Event
}
