package be.utils;

public class Lao {
  public String id;
  public String name;

  // Organizer public key
  public String organizerPk;
  public long creation;

  public Lao(String organizerPk, Long creation, String name){
    this.creation = creation;
    this.organizerPk = organizerPk;
    this.name = name;

    if(name.isEmpty()){
      // Cannot create id with empty name
      name = "empty";
    }
    this.id = generateLaoId(organizerPk, creation, name);
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


  public Lao setName(String newName) {
    return new Lao(organizerPk, creation, newName);
  }

  public Lao setCreation(long creation) {
    return new Lao(organizerPk, creation, name);
  }

}
