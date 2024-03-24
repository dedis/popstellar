package ch.epfl.pop.model.network.method.message.data.election

enum VersionType(val version: String):
  case INVALID extends VersionType("__INVALID_OBJECT__")
  case OPEN_BALLOT extends VersionType("OPEN_BALLOT")
  case SECRET_BALLOT extends VersionType("SECRET_BALLOT")
