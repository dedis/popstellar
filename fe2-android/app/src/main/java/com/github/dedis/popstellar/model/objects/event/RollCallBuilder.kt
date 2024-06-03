package com.github.dedis.popstellar.model.objects.event

import com.github.dedis.popstellar.model.objects.RollCall
import com.github.dedis.popstellar.model.objects.security.PublicKey

class RollCallBuilder {
  private var id: String? = null
  private var persistentId: String? = null
  private var name: String? = null
  private var creation: Long = 0
  private var start: Long = 0
  private var end: Long = 0
  private var state: EventState? = null
  private var attendees: MutableSet<PublicKey>? = null
  private var location: String? = null
  private var description: String? = null

  fun setId(id: String?): RollCallBuilder {
    this.id = id
    return this
  }

  constructor()

  constructor(rollCall: RollCall) {
    id = rollCall.id
    persistentId = rollCall.persistentId
    name = rollCall.name
    creation = rollCall.creation
    start = rollCall.startTimestamp
    end = rollCall.end
    state = rollCall.state
    attendees = HashSet(rollCall.attendees)
    location = rollCall.location
    description = rollCall.description
  }

  fun setPersistentId(id: String?): RollCallBuilder {
    persistentId = id
    return this
  }

  fun setName(name: String?): RollCallBuilder {
    this.name = name
    return this
  }

  fun setCreation(creation: Long): RollCallBuilder {
    this.creation = creation
    return this
  }

  fun setStart(start: Long): RollCallBuilder {
    this.start = start
    return this
  }

  fun setEnd(end: Long): RollCallBuilder {
    this.end = end
    return this
  }

  fun setState(state: EventState?): RollCallBuilder {
    this.state = state
    return this
  }

  fun setAttendees(attendees: Set<PublicKey>): RollCallBuilder {
    this.attendees = LinkedHashSet(attendees)
    return this
  }

  fun setEmptyAttendees(): RollCallBuilder {
    attendees = LinkedHashSet()
    return this
  }

  fun setLocation(location: String?): RollCallBuilder {
    this.location = location
    return this
  }

  fun setDescription(description: String?): RollCallBuilder {
    this.description = description
    return this
  }

  fun build(): RollCall {
    checkNotNull(id) { "Id is null" }
    checkNotNull(persistentId) { "Persistent Id is null" }
    checkNotNull(name) { "Name is null" }
    checkNotNull(state) { "State is null" }
    checkNotNull(description) { "Description is null" }
    checkNotNull(location) { "Location is null" }
    checkNotNull(attendees) { "Attendee set is null" }

    return RollCall(
        id!!,
        persistentId!!,
        name!!,
        creation,
        start,
        end,
        state!!,
        attendees!!,
        location!!,
        description!!)
  }
}
