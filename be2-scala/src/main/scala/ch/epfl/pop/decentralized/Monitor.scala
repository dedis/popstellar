package ch.epfl.pop.decentralized

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, Props, Timers}
import akka.event.LoggingReceive
import akka.stream.scaladsl.Sink
import ch.epfl.pop.config.RuntimeEnvironment.{readServerPeers, serverPeersListPath}
import ch.epfl.pop.decentralized.Monitor.TriggerHeartbeat
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.ParamsWithMap
import ch.epfl.pop.pubsub.graph.GraphMessage

import java.nio.file.{Path, WatchService}
import java.nio.file.StandardWatchEventKinds.{ENTRY_CREATE, ENTRY_MODIFY}
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.jdk.CollectionConverters.CollectionHasAsScala

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

    case ConnectionMediator.Ping() =>
      log.info("Received ConnectionMediator ping")
      connectionMediatorRef = sender()
      new Thread(new FileMonitor(connectionMediatorRef)).start()

    case _ => /* DO NOTHING */
  }
}

object Monitor {
  def props(heartbeatGenRef: ActorRef, heartbeatRate: FiniteDuration = 15.seconds, messageDelay: FiniteDuration = 1.seconds): Props =
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

// This class watch the list of server peers config file and upon changes
// tells connectionMediator about it
private class FileMonitor(mediatorRef: ActorRef) extends Runnable {

  // getParent to exclude the filename from the path, i.e get the config directory path
  private val directory: Path = Path.of(serverPeersListPath).getParent
  private val watchService: WatchService = directory.getFileSystem.newWatchService()
  directory.register(watchService, ENTRY_CREATE, ENTRY_MODIFY)

  override def run(): Unit = {
    try {
      while (!Thread.currentThread().isInterrupted) {
        // Blocks until an event happen
        val watchKey = watchService.take()

        // For any event, read the file and send it
        for (event <- watchKey.pollEvents().asScala.toList) {
          if (serverPeersListPath.endsWith(event.context().toString)) {
            mediatorRef ! ConnectionMediator.ConnectTo(readServerPeers())
          }
        }
        watchKey.reset()
      }
    } catch {
      case _: InterruptedException =>
        println("File watch service interrupted")
    } finally {
      watchService.close()
    }
  }
}
