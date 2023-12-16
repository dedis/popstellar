package com.github.dedis.popstellar.model.network.method.message.data.election

import java.util.Collections

/**
 * Version of an election: - OPEN_BALLOT: normal process - SECRET_BALLOT: vote is encrypted with
 * EL_GAMAL encryption scheme
 */
enum class ElectionVersion(val stringBallotVersion: String) {
    OPEN_BALLOT("open-ballot"), SECRET_BALLOT("secret-ballot");

    companion object {
        val allElectionVersion = Collections.unmodifiableList(listOf(*values()))
    }
}