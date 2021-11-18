package ch.epfl.pop.model.objects

import java.util.Base64

import scala.util.{Success, Try}

final case class Channel(channel: String) {

  /**
   * Extract the sub-channel (laoId) from a channel name
   *
   * @return an array of bytes corresponding to the decoded sub-channel name or None if an error occurred
   */
  def decodeSubChannel: Option[Array[Byte]] = channel match {
    case _ if channel.startsWith(Channel.rootChannelPrefix) =>
      Try(Base64.getUrlDecoder.decode(channel.substring(Channel.rootChannelPrefix.length).getBytes)) match {
        case Success(value) => Some(value)
        case _ => None
      }
    case _ => None
  }

  /**
   * Extract the channel id from a Channel (i.e. the last part of the path)
   *
   * @return the id of the Channel
   * @example extractChannelId(Channel("/root/mEKXWFCMwb") == Hash(Base64Data("mEKXWFCMwb"))
   */
  def extractChildChannel: Hash = Hash(Base64Data(channel.split(Channel.SEPARATOR).last))

  def extractLaoId: String = {
    if (isRootChannel){
      null
    }
    else{
      channel.split(Channel.SEPARATOR)(2) //since the channel starts with '/'
    }
  }

  def isRootChannel: Boolean = channel == Channel.rootChannel.channel

  def isSubChannel: Boolean = channel.startsWith(Channel.rootChannelPrefix)

  override def equals(that: Any): Boolean = that match {
    case that: Channel => channel == that.channel
    case _ => false
  }

  override def toString: String = channel
}

object Channel {
  final val SEPARATOR: Char = '/'
  final val rootChannel: Channel = Channel(s"${SEPARATOR}root")
  final val rootChannelPrefix: String = s"${SEPARATOR}root${SEPARATOR}"
}
