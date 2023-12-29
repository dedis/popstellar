package com.github.dedis.popstellar.utility.error

abstract class UnknownEventException protected constructor(eventType: String, id: String) :
    GenericException("$eventType with id $id is unknown.")
