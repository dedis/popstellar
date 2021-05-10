package ch.epfl.pop.model.objects

import java.util.Base64

import scala.util.{Success, Try}

object Channel {
  type Channel = String

  val rootChannel: String = "/root"
  val rootChannelPrefix: String = "/root/"

  /**
   * Extract the sub-channel (laoId) from a channel name
   *
   * @param channel full channel name
   * @return an array of bytes corresponding to the decoded sub-channel name or None if an error occurred
   */
  def decodeSubChannel(channel: Channel.Channel): Option[Array[Byte]] = channel match {
    case _ if channel.startsWith(rootChannelPrefix) =>
      Try(Base64.getUrlDecoder.decode(channel.substring(rootChannelPrefix.length).getBytes)) match {
        case Success(value) => Some(value)
        case _ => None
      }
    case _ => None
  }
}
