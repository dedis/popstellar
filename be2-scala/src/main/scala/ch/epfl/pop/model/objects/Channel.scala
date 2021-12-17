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
    case _ if channel.startsWith(Channel.ROOT_CHANNEL_PREFIX) =>
      Try(Base64.getUrlDecoder.decode(channel.substring(Channel.ROOT_CHANNEL_PREFIX.length).getBytes)) match {
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
  def extractChildChannel: Hash = {
      //After successful channel creation
      //c cannot be empty
      val c = channel.split(Channel.SEPARATOR)
      assert(!c.isEmpty)
      Hash(Base64Data( c.last))
  }

  def isRootChannel: Boolean = channel == Channel.ROOT_CHANNEL.channel

  def isSubChannel: Boolean = channel.startsWith(Channel.ROOT_CHANNEL_PREFIX)

  override def equals(that: Any): Boolean = that match {
    case that: Channel => channel == that.channel
    case _ => false
  }

  override def toString: String = channel
}

object Channel {
  final val SEPARATOR: Char = '/'
  final val ROOT_CHANNEL: Channel = Channel(s"${SEPARATOR}root")
  final val ROOT_CHANNEL_PREFIX: String = s"${SEPARATOR}root${SEPARATOR}"
  private final def channelRegex: String = "^/root(/[^/]+)*$"
  final val LAO_DATA_LOCATION: String = s"${SEPARATOR}data"

  final val SOCIAL_MEDIA_POSTS_PREFIX: String = s"${SEPARATOR}posts"
  final val SOCIAL_CHANNEL_PREFIX: String = s"${SEPARATOR}social"

  def apply(channel: String): Channel = {
    if(channel.isBlank() || !channel.matches(channelRegex)){
        throw new IllegalArgumentException("The channel name is invalid")
    }
    new Channel(channel)
  }
}
