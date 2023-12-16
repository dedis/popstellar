package com.github.dedis.popstellar.model.objects.event

import com.github.dedis.popstellar.model.objects.Meeting

/** This class is a builder for the Meeting object  */
class MeetingBuilder {
    private var id: String? = null
    private var name: String? = null
    private var creation: Long = 0
    private var start: Long = 0
    private var end: Long = 0
    private var location: String? = null
    private var lastModified: Long = 0
    private var modificationId: String? = null
    private var modificationSignatures: List<String>? = null

    constructor()
    constructor(meeting: Meeting) {
        id = meeting.id
        name = meeting.name
        creation = meeting.creation
        start = meeting.startTimestamp
        end = meeting.endTimestamp
        location = meeting.location
        lastModified = meeting.lastModified
        modificationId = meeting.modificationId
        modificationSignatures = meeting.modificationSignatures
    }

    fun setId(id: String?): MeetingBuilder {
        this.id = id
        return this
    }

    fun setName(name: String?): MeetingBuilder {
        this.name = name
        return this
    }

    fun setCreation(creation: Long): MeetingBuilder {
        this.creation = creation
        return this
    }

    fun setStart(start: Long): MeetingBuilder {
        this.start = start
        return this
    }

    fun setEnd(end: Long): MeetingBuilder {
        this.end = end
        return this
    }

    fun setLocation(location: String?): MeetingBuilder {
        requireNotNull(location) { "Location is null" }
        this.location = location
        return this
    }

    fun setLastModified(lastModified: Long): MeetingBuilder {
        this.lastModified = lastModified
        return this
    }

    fun setModificationId(modificationId: String?): MeetingBuilder {
        requireNotNull(modificationId) { "Modification Id is null" }
        this.modificationId = modificationId
        return this
    }

    fun setModificationSignatures(modificationSignatures: List<String>?): MeetingBuilder {
        requireNotNull(modificationSignatures) { "Modification signatures list is null" }
        this.modificationSignatures = modificationSignatures
        return this
    }

    fun build(): Meeting {
        checkNotNull(location) { "Location is null" }
        requireNotNull(modificationId) { "Modification Id is null" }
        requireNotNull(modificationSignatures) { "Modification signatures list is null" }
        return Meeting(
                id!!,
                name!!,
                creation,
                start,
                end,
                location!!,
                lastModified,
                modificationId!!,
                modificationSignatures)
    }
}