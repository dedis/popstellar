package ch.epfl.pop.storage

import akka.actor.{Actor, ActorLogging}

case class DbActorNew() extends Actor with ActorLogging {
  private val storage: DiskStorage = new DiskStorage()

  override def postStop(): Unit = {
    storage.close()
    super.postStop()
  }

  override def receive: Receive = ???
}
