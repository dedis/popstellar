package ch.epfl.pop.decentralized

import akka.NotUsed
import akka.actor.{Actor, ActorLogging, ActorRef, PoisonPill, Props, Timers}
import akka.event.LoggingReceive
import akka.pattern.{AskableActorRef, ask}
import akka.stream.scaladsl.Sink
import ch.epfl.pop.config.RuntimeEnvironment.{readServerPeers, serverPeersListPath}
import ch.epfl.pop.decentralized.Monitor.TriggerHeartbeat
import ch.epfl.pop.model.network.JsonRpcRequest
import ch.epfl.pop.model.network.method.{Heartbeat, ParamsWithMap}
import ch.epfl.pop.model.objects.{Channel, Hash}
import ch.epfl.pop.pubsub.AskPatternConstants
import ch.epfl.pop.pubsub.graph.GraphMessage
import ch.epfl.pop.storage.DbActor

import java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY
import java.nio.file.{Path, WatchService}
import java.util.concurrent.atomic.AtomicBoolean
import scala.collection.immutable.HashMap
import scala.concurrent.Await
import scala.concurrent.duration.{DurationInt, FiniteDuration}
import scala.jdk.CollectionConverters.CollectionHasAsScala
import scala.util.Success

//This actor is tasked with scheduling heartbeats.
// To that end it sees every message the system receives.
// When a message is seen it schedule a heartbeat in the next heartbeatRate seconds.
// Periodic heartbeats are sent with a period of messageDelay seconds.
final case class Monitor(
    dbActorRef: AskableActorRef,
    heartbeatRate: FiniteDuration,
    messageDelay: FiniteDuration
) extends Actor with ActorLogging with Timers with AskPatternConstants() {

  // These keys are used to keep track of the timers states
  private val periodicHbKey = 0
  private val singleHbKey = 1

  // State of connected servSers
  private var someServerConnected = false

  // Monitor is self-contained,
  // To that end it doesn't know the ref of the connectionMediator
  private var connectionMediatorRef = ActorRef.noSender

  private var fileMonitor: FileMonitor = _

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

      val askForHeartbeat = dbActorRef ? DbActor.GenerateHeartbeat()
      val heartbeat: HashMap[Channel, Set[Hash]] =
        Await.ready(askForHeartbeat, duration).value.get match
          case Success(DbActor.DbActorGenerateHeartbeatAck(map)) => map
          case _ => HashMap.empty[Channel, Set[Hash]] // Handle anything else

      if (heartbeat.nonEmpty)
        connectionMediatorRef ! Heartbeat(heartbeat)

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
      fileMonitor = new FileMonitor(this.self)
      new Thread(fileMonitor).start()

    case msg: ConnectionMediator.ConnectTo =>
      connectionMediatorRef ! msg
  }

  override def postStop(): Unit = {
    if (fileMonitor != null) {
      if(!fileMonitor.running.compareAndSet(true, false)) {
        log.error("Couldn't stop file monitor")
      }
      }
    }
}

object Monitor {
  def props(dbActorRef: AskableActorRef, heartbeatRate: FiniteDuration = 15.seconds, messageDelay: FiniteDuration = 1.seconds): Props =
    Props(new Monitor(dbActorRef, heartbeatRate, messageDelay))

  def sink(monitorRef: ActorRef): Sink[GraphMessage, NotUsed] = {
    Sink.actorRef(
      monitorRef,
      DoNothing(),
      {
        (_: Throwable) => /* Do nothing */
      }
    )
  }

  sealed trait Event
  final case class AtLeastOneServerConnected() extends Event
  final case class NoServerConnected() extends Event
  final case class TriggerHeartbeat() extends Event
  private final case class DoNothing() extends Event
}

// This class watch the list of server peers config file and upon changes
// tells connectionMediator about it
private class FileMonitor(mediatorRef: ActorRef) extends Runnable {

  // getParent to exclude the filename from the path, i.e get the config directory path
  private val directory: Path = Path.of(serverPeersListPath).getParent
  private val watchService: WatchService = directory.getFileSystem.newWatchService()
  directory.register(watchService, ENTRY_MODIFY)
  var sendConnectToMessage = false
  var running: AtomicBoolean = AtomicBoolean(true)
  private var serverPeers: List[String] = _

  override def run(): Unit = {

    var sendConnectToMessage = false
    // Upon start, we connect to the servers
    serverPeers = readServerPeers()
    mediatorRef ! ConnectionMediator.ConnectTo(serverPeers)
    while (running.get()) {
      // Blocks until an event happen
      val watchKey = watchService.take()
      if (!running.get()) {
        return
      }
      sendConnectToMessage = false

      // For any event, read the file and send it
      for (event <- watchKey.pollEvents().asScala.toList) {
        if (serverPeersListPath.endsWith(event.context().toString)) {
          sendConnectToMessage = true
        }
      }

      if (running.get() && sendConnectToMessage) {
        val newPeers = readServerPeers()
        if (newPeers != serverPeers) {
          serverPeers = newPeers
          mediatorRef ! ConnectionMediator.ConnectTo(serverPeers)
        }
      }
      watchKey.reset()
      if (!watchKey.isValid) {
        return
      }
    }
    watchService.close()
  }
}
