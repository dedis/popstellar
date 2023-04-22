package ch.epfl.pop.pubsub

import java.util.concurrent.TimeUnit

import akka.util.Timeout

import scala.concurrent.duration.{Duration, FiniteDuration}

trait AskPatternConstants {
  implicit final val duration: FiniteDuration = Duration(1, TimeUnit.SECONDS)
  implicit final val timeout: Timeout = Timeout(duration)
}
