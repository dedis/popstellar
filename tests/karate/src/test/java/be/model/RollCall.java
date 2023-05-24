package be.model;

import be.utils.Hash;

/** Models a roll call in a pop lao */
public class RollCall {
  public String id;
  public String name;
  public long creation;
  public long start;
  public long end;
  public String location;
  public String description;
  public String laoId;

  private final static String ROLL_CALL_SUFFIX = "R";

  public RollCall(String id, String name, long creation, long start, long end, String location, String description, String laoId) {
    this.id = id;
    this.name = name;
    this.creation = creation;
    this.start = start;
    this.end = end;
    this.location = location;
    this.description = description;
    this.laoId = laoId;
  }

  /**
   * Copies the existing roll call but overwrites the name with the given new name.
   * Recomputes the roll call id to match the new name (Except for the empty string which cannot be hashed).
   *
   * @param newName the new roll name
   * @return copy of the roll call with new name
   */
  public RollCall setName(String newName) {
    String hashName = newName;
    if(newName.isEmpty()){
      // Cannot create a matching id with empty name, because empty string cannot be hashed
      hashName = "empty";
    }
    String newId = generateCreateRollCallId(laoId, creation, hashName);
    return new RollCall(newId, newName, creation, start, end, location, description, laoId);
  }

  /**
   * Copies the existing roll call but overwrites the creation time with the given new creation time.
   * Recomputes the roll call id to match the new creation time.
   *
   * @param newCreation the new roll creation time
   * @return copy of the roll call with new creation time
   */
  public RollCall setCreation(long newCreation) {
    String newId = generateCreateRollCallId(laoId, newCreation, name);
    return new RollCall(newId, name, newCreation, start, end, location, description, laoId);
  }

  /**
   * Copies the existing roll call but switches the start and end time of the roll call.
   *
   * @return copy of the roll call with switched start and end time
   */
  public RollCall switchStartAndEnd() {
    return new RollCall(id, name, creation, end, start, location, description, laoId);
  }


  /**
   * Copies the existing roll call but switches the creation and start time of the roll call.
   * Recomputes the roll call id to match the new creation time.
   *
   * @return copy of the roll call with switched creation and start time
   */
  public RollCall switchCreationAndStart() {
    String newId = generateCreateRollCallId(laoId, start, name);
    return new RollCall(newId, name, start, creation, end, location, description, laoId);
  }

  /**
   * Generate the id for dataCreateRollCall.
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCreateRollCall.json
   *
   * @param laoId ID of the LAO
   * @param creation creation time of RollCall
   * @param name name of RollCall
   * @return the ID of CreateRollCall computed as Hash('R'||lao_id||creation||name)
   */
  public static String generateCreateRollCallId(String laoId, long creation, String name) {
    return Hash.hash(ROLL_CALL_SUFFIX, laoId, Long.toString(creation), name);
  }

  /**
   * Generate the id for dataOpenRollCall.
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataOpenRollCall.json
   *
   * @param laoId ID of the LAO
   * @param opens id of RollCall to open
   * @param openedAt open time of RollCall
   * @return the ID of OpenRollCall computed as Hash('R'||lao_id||opens||opened_at)
   */
  public static String generateOpenRollCallId(String laoId, String opens, long openedAt) {
    return Hash.hash(ROLL_CALL_SUFFIX, laoId, opens, Long.toString(openedAt));
  }

  /**
   * Generate the id for dataCloseRollCall.
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCloseRollCall.json
   *
   * @param laoId ID of the LAO
   * @param closes id of RollCall to close
   * @param closedAt closing time of RollCall
   * @return the ID of CloseRollCall computed as Hash('R'||lao_id||closes||closed_at)
   */
  public static String generateCloseRollCallId(String laoId, String closes, long closedAt) {
    return Hash.hash(ROLL_CALL_SUFFIX, laoId, closes, Long.toString(closedAt));
  }
}


