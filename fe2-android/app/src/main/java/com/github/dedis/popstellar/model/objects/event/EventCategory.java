package com.github.dedis.popstellar.model.objects.event;

import androidx.annotation.NonNull;

/** Enum class modeling the Event Categories */
public enum EventCategory {
  PAST {
    @NonNull
    @Override
    public String toString() {
      return "Past Events";
    }
  },

  PRESENT {
    @NonNull
    @Override
    public String toString() {
      return "Present Events";
    }
  },

  FUTURE {
    @NonNull
    @Override
    public String toString() {
      return "Future Events";
    }
  }
}
