package be.utils;

public class Lao {
  private String id;
  private String name;
  private String organizerPublicKey;
  private long creation;

  public Lao(String organizerPublicKey, Long creation, String name){
    this.creation = creation;
    this.organizerPublicKey = organizerPublicKey;
    this.name = name;
    this.id = generateLaoId(organizerPublicKey, creation, name);
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

  public String getName(){
    return name;
  }

  public String getOrganizerPublicKey(){
    return organizerPublicKey;
  }

  public String getLaoId(){
    return id;
  }

  public long getCreation(){
    return creation;
  }
}
