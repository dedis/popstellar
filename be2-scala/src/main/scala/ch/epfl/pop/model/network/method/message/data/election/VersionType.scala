package ch.epfl.pop.model.network.method.message.data.election

enum VersionType:
  case INVALID extends VersionType
  case OPEN_BALLOT extends VersionType
  case SECRET_BALLOT extends VersionType

object VersionType:
  def unapply(versionType: String): Option[VersionType] =
    versionType match
      case "__INVALID_OBJECT__" => Some(INVALID)
      case "OPEN_BALLOT"        => Some(OPEN_BALLOT)
      case "SECRET_BALLOT"      => Some(SECRET_BALLOT)
      case _                    => None
