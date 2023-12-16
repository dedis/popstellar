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
    private var attendees: Set<PublicKey>? = null
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
        start = rollCall.start
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

    fun setAttendees(attendees: Set<PublicKey>?): RollCallBuilder {
        requireNotNull(attendees) { "Attendee set is null" }
        this.attendees = attendees
        return this
    }

    fun setEmptyAttendees(): RollCallBuilder {
        attendees = HashSet()
        return this
    }

    fun setLocation(location: String?): RollCallBuilder {
        requireNotNull(location) { "Location is null" }
        this.location = location
        return this
    }

    fun setDescription(description: String?): RollCallBuilder {
        requireNotNull(description) { "Description is null" }
        this.description = description
        return this
    }

    fun build(): RollCall {
        checkNotNull(description) { "Description is null" }
        checkNotNull(location) { "Location is null" }
        checkNotNull(attendees) { "Attendee set is null" }
        return RollCall(
                id!!, persistentId!!, name!!, creation, start, end, state!!, attendees!!, location!!, description!!)
    }
}