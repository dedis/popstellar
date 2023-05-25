package be.model;

import be.utils.Hash;

import java.util.ArrayList;
import java.util.List;

/** Models a pop lao */
public class Lao {
  public String id;
  public String name;
  // Organizer public key
  public String organizerPk;
  public long creation;
  public List<String> witnesses = new ArrayList<>();
  public String channel;

  public Lao(String organizerPk, Long creation, String name){
    this.creation = creation;
    this.organizerPk = organizerPk;
    this.name = name;

    if(name.isEmpty()){
      // Cannot create a matching id with empty name, because empty string cannot be hashed
      name = "empty";
    }
    this.id = generateLaoId(organizerPk, creation, name);
    // The organizer is always a witness
    this.witnesses.add(organizerPk);
    this.channel = "/root/" + id;
  }

  /**
   * Copies the existing lao but overwrites the name with the given new name.
   * Recomputes the lao id to match the new name (Except for the empty string which cannot be hashed).
   *
   * @param newName the new lao name
   * @return copy of the lao with new name
   */
  public Lao setName(String newName) {
    return new Lao(organizerPk, creation, newName);
  }

  /**
   * Copies the existing lao but overwrites the creation with the given new creation time.
   * Recomputes the lao id to match the new creation time.
   *
   * @param newCreation the new lao creation time
   * @return copy of the lao with new creation time
   */
  public Lao setCreation(long newCreation) {
    return new Lao(organizerPk, newCreation, name);
  }

  /**
   * Generate the id for dataCreateLao and dataUpdateLao.
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataCreateLao.json
   * https://github.com/dedis/popstellar/blob/master/protocol/query/method/message/data/dataUpdateLao.json
   *
   * @param organizerPublicKey ID of the organizer
   * @param creation creation time of the LAO
   * @param name original or updated name of the LAO
   * @return the ID of CreateLao or UpdateLao computed as Hash(organizer||creation||name)
   */
  public static String generateLaoId(String organizerPublicKey, long creation, String name) {
    return Hash.hash(organizerPublicKey, Long.toString(creation), name);
  }

}
