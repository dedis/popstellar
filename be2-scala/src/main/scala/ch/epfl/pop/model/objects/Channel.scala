package ch.epfl.pop.model.objects

import java.util.Base64

object Channel {
  type Channel = String

  val rootChannelPrefix: String = "/root/"

  /**
   * Extract the sub-channel (laoId) from a channel name
   *
   * @param channel full channel name
   * @return an array of bytes corresponding to the decoded sub-channel name or None if an error occurred
   */
  def decodeSubChannel(channel: Channel.Channel): Option[Array[Byte]] = channel match {
    case _ if channel.startsWith(rootChannelPrefix) => Some(Base64.getDecoder.decode(channel.getBytes)) // TODO may throw
    case _ => None
  }
}
