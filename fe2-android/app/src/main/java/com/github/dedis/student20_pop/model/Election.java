package com.github.dedis.student20_pop.model;


import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Class modeling an Election */
public final class Election {

  private final String name;
  private final long time;
  private final String id;
  private final String lao;
  private final List<String> options;

  /**
   * Constructor of an Election
   *
   * @param name the name of the election, can be empty
   * @param lao the LAO associated to the election
   * @param options the default ballot options
   * @throws IllegalArgumentException if any of the parameters is null
   */
  public Election(String name, String lao, List<String> options) {
    if (name == null || lao == null || options == null || options.contains(null)) {
      throw new IllegalArgumentException("Trying to create an Election with a null value");
    }
    this.name = name;
    this.time = Instant.now().getEpochSecond();
    this.id = ""; // Hash.hash(lao, time, name);
    this.lao = lao;
    this.options = options;
  }

  /** Returns the name of an Election. */
  public String getName() {
    return name;
  }

  /** Returns the creation time of the Election as Unix Timestamp, can't be modified. */
  public long getTime() {
    return time;
  }

  /** Returns the ID of the Election, can't be modified. */
  public String getId() {
    return id;
  }

  /** Returns the associated LAO's ID. */
  public String getLao() {
    return lao;
  }

  /** Returns the default ballot options. */
  public List<String> getOptions() {
    return options;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    Election election = (Election) o;
    return time == election.time
        && Objects.equals(name, election.name)
        && Objects.equals(id, election.id)
        && Objects.equals(lao, election.lao)
        && Objects.equals(options, election.options);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, time, id, lao, options);
  }
}
