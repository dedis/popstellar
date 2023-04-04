package ch.epfl.pop.model.objects

import scala.util.{Success, Try}

final case class Channel(channel: String) {

  /** Extract the laoId from a channel name (even though it might be in the middle)
    *
    * @return
    *   An Option[Hash] corresponding to the decoded laoId or None if an error occurred
    */
  def decodeChannelLaoId: Option[Hash] = channel match {
    case _ if channel.startsWith(Channel.ROOT_CHANNEL_PREFIX) =>
      Try(Hash(Base64Data(channel.substring(Channel.ROOT_CHANNEL_PREFIX.length).split(Channel.CHANNEL_SEPARATOR)(0)))) match {
        case Success(value) => Some(value)
        case _              => None
      }
    case _ => None
  }

  /** Extract the channel id from a Channel (i.e. the last part of the path)
    *
    * @return
    *   the id of the Channel
    * @example
    *   extractChannelId(Channel("/root/mEKXWFCMwb") == Hash(Base64Data("mEKXWFCMwb"))
    */
  def extractChildChannel: Hash = {
    // After successful channel creation
    // c cannot be empty
    val c = channel.split(Channel.CHANNEL_SEPARATOR)
    assert(!c.isEmpty)
    Hash(Base64Data(c.last))
  }

  def isRootChannel: Boolean = channel == Channel.ROOT_CHANNEL.channel

  def isSubChannel: Boolean = channel.startsWith(Channel.ROOT_CHANNEL_PREFIX)

  override def equals(that: Any): Boolean = that match {
    case t: Channel => channel == t.channel
    case _          => false
  }

  override def toString: String = channel
}

object Channel {
  final val CHANNEL_SEPARATOR: Char = '/'
  final val DATA_SEPARATOR: Char = '#'
  final val ROOT_CHANNEL: Channel = Channel(s"${CHANNEL_SEPARATOR}root")
  final val ROOT_CHANNEL_PREFIX: String = s"${CHANNEL_SEPARATOR}root$CHANNEL_SEPARATOR"

  private def channelRegex: String = "^/root(/[^/]+)*$"

  final val LAO_DATA_LOCATION: String = s"${DATA_SEPARATOR}laodata"

  final val COIN_CHANNEL_PREFIX: String = s"${CHANNEL_SEPARATOR}coin"

  final val SOCIAL_CHANNEL_PREFIX: String = s"${CHANNEL_SEPARATOR}social$CHANNEL_SEPARATOR"
  final val SOCIAL_MEDIA_CHIRPS_PREFIX: String = s"${SOCIAL_CHANNEL_PREFIX}chirps"
  final val REACTIONS_CHANNEL_PREFIX: String = s"${SOCIAL_CHANNEL_PREFIX}reactions"

  def apply(channel: String): Channel = {
    if (channel.trim.isEmpty || !channel.matches(channelRegex)) {
      throw new IllegalArgumentException("The channel name is invalid")
    }
    new Channel(channel)
  }

}
