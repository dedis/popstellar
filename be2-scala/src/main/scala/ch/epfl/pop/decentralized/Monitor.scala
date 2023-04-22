package ch.epfl.pop.decentralized

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import akka.event.LoggingReceive
import akka.stream.scaladsl.Sink
import ch.epfl.pop.decentralized.Monitor.TriggerHeartbeat
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.ParamsWithMap
import ch.epfl.pop.pubsub.graph.GraphMessage

import scala.concurrent.duration.{DurationInt, FiniteDuration}

//This actor is tasked with scheduling heartbeats.
// To that end it sees every messages the system receives.
// When a message is seen it schedule a heartbeat in the next heartbeatRate seconds.
// Periodic heartbeats are sent with a period of messageDelay seconds.
final case class Monitor(
    heartbeatGenRef: ActorRef,
    heartbeatRate: FiniteDuration,
    messageDelay: FiniteDuration
) extends Actor with ActorLogging with Timers {

  // These keys are used to keep track of the timers states
  private val periodicHbKey = 0
  private val singleHbKey = 1

  // State of connected servers
  private var someServerConnected = false

  // Monitor is self-contained,
  // To that end it doesn't know the ref of the connectionMediator
  private var connectionMediatorRef = ActorRef.noSender

  override def receive: Receive = LoggingReceive {

    case Monitor.AtLeastOneServerConnected =>
      connectionMediatorRef = sender()
      if (!someServerConnected) {
        timers.startTimerWithFixedDelay(periodicHbKey, TriggerHeartbeat, heartbeatRate)
        someServerConnected = true
      }

    case Monitor.NoServerConnected =>
      someServerConnected = false
      timers.cancelAll()

    case Monitor.TriggerHeartbeat =>
      log.info("triggering a heartbeat")
      timers.cancel(singleHbKey)
      heartbeatGenRef ! Monitor.GenerateAndSendHeartbeat(connectionMediatorRef)

    case Right(jsonRpcMessage: JsonRpcRequest) =>
      jsonRpcMessage.getParams match {
        case _: ParamsWithMap => /* Actively ignoring this specific message */
        // For any other message, we schedule a single heartbeat to reduce messages propagation delay
        case _ =>
          if (someServerConnected && !timers.isTimerActive(singleHbKey)) {
            log.info(s"Scheduling single heartbeat")
            timers.startSingleTimer(singleHbKey, TriggerHeartbeat, messageDelay)
          }
      }

    case _ => /* DO NOTHING */
  }
}

object Monitor {
  def props(heartbeatGenRef: ActorRef, heartbeatRate: FiniteDuration = 30.seconds, messageDelay: FiniteDuration = 3.seconds): Props =
    Props(new Monitor(heartbeatGenRef, heartbeatRate, messageDelay))

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
