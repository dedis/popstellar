package com.github.dedis.student20_pop.model.event;

import java.util.List;
import java.util.Objects;

/** Class modeling a Poll Event */
public final class PollEvent extends Event {

  private final long endTime;
  private final List<String> choices;
  private final boolean oneOfN;

  /**
   * Constructor for a Poll Event
   *
   * @param question the question of the poll event, considered as the name
   * @param startTime the start time of the poll event
   * @param endTime the end time of the poll event
   * @param lao the ID of the associated LAO
   * @param location the location of the poll event
   * @param choices the list of possible choices
   * @param oneOfN true if it is a choose one type of poll
   * @throws IllegalArgumentException if any of the parameters is null
   */
  public PollEvent(
      String question,
      long startTime,
      long endTime,
      String lao,
      String location,
      List<String> choices,
      boolean oneOfN) {
    super(question, lao, startTime, location, EventType.POLL);
    if (choices == null || choices.contains(null)) {
      throw new IllegalArgumentException("Trying to create a meeting event with null parameters");
    }
    this.endTime = endTime;
    this.choices = choices;
    this.oneOfN = oneOfN;
  }

  /** Returns the end time of the Poll. */
  public long getEndTime() {
    return endTime;
  }

  /** Returns the list of choices of the Poll. */
  public List<String> getChoices() {
    return choices;
  }

  /** True if the Poll is one choice, false if multi-choice. */
  public boolean isOneOfN() {
    return oneOfN;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;
    PollEvent pollEvent = (PollEvent) o;
    return Objects.equals(endTime, pollEvent.endTime)
        && Objects.equals(choices, pollEvent.choices)
        && oneOfN == pollEvent.oneOfN;
  }

  @Override
  public int hashCode() {
    return Objects.hash(super.hashCode(), endTime, choices, oneOfN);
  }
}
