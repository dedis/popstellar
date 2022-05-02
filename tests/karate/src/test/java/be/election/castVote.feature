@env=go,scala
Feature: Cast a vote
  Background:
        # This feature will be called to test Cast Vote
        # For every test a file containing the json representation of the message is read
        # then sent to the backend and stored there.
        # This is done via :
        # eval frontend.send(<message>) where a mock frontend sends a message to backend
        # Then the response sent by the backend and stored in a buffer :
        # json response = frontend_buffer.takeTimeout(timeout)
        # and checked if it contains the desired fields with :
        # match response contains deep <desired fields>

    # The following calls makes this feature, mockFrontEnd.feature and server.feature share the same scope
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')
    * def castVoteId = 41
    * string electionChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/ZVxXK2QN60uCNxNsIzShYYQmtwGttWLpQPQapYCNg4g="
    * string laoChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="

  # Testing if after creating an election correctly, casting a valid vote succeeds
  Scenario: Casting a valid vote on a started election
    Given string castVoteData = read('classpath:data/election/data/castVote/valid_cast_vote_data.json')
    And string castVote = converter.publishМessageFromData(castVoteData, castVoteId, electionChannel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    When eval frontend.send(castVote)
    * json cast_vote = frontend_buffer.takeTimeout(timeout)
    Then match cast_vote contains deep {jsonrpc: '2.0', id: '#(castVoteId)', result: 0}

  # Testing if after creating an election correctly, the backend returns an error
  # upon casting a vote on an LAO channel instead of an election one
  Scenario: Casting a vote on a lao channel should return an error
    Given string castVoteData = read('classpath:data/election/data/castVote/valid_cast_vote_2_data.json')
    And string castVote = converter.publishМessageFromData(castVoteData, castVoteId, laoChannel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    When eval frontend.send(castVote)
    * json cast_vote = frontend_buffer.takeTimeout(timeout)
    Then match cast_vote contains deep {jsonrpc: '2.0', id: '#(castVoteId)', error: {code: -4, description: '#string'}}

  # Testing if before creating an election, the backend returns an error
  # upon casting a vote
  Scenario: Casting a valid vote on non existent election should return an error
    Given string badCastVoteData = read('classpath:data/election/data/castVote/bad_cast_vote_invalid_election_id_data.json')
    And string badCastVote = converter.publishМessageFromData(badCastVoteData, castVoteId, electionChannel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    When eval frontend.send(badCastVote)
    * json cast_vote = frontend_buffer.takeTimeout(timeout)
    Then match cast_vote contains deep {jsonrpc: '2.0', id: '#(castVoteId)', error: {code: -4, description: '#string'}}

  # Testing if after creating an election correctly, the backend returns an error
  # upon casting a vote but with wrong vote id
  Scenario: Casting a valid vote with wrong vote id should return an error
    Given string badCastVoteData = read('classpath:data/election/data/castVote/bad_cast_vote_invalid_vote_id_data.json')
    And string badCastVote = converter.publishМessageFromData(badCastVoteData, castVoteId, electionChannel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    When eval frontend.send(badCastVote)
    * json cast_vote = frontend_buffer.takeTimeout(timeout)
    Then match cast_vote contains deep {jsonrpc: '2.0', id: '#(castVoteId)', error: {code: -4, description: '#string'}}

  # Testing if after creating an election correctly, the backend returns an error
  # upon casting a valid vote but after the election end time
  Scenario: Casting a valid vote too late should return an error
    Given string badCastVoteData = read('classpath:data/election/data/castVote/bad_cast_vote_late_vote_data.json')
    And string badCastVote = converter.publishМessageFromData(badCastVoteData, castVoteId, electionChannel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    And eval frontend.send(badCastVote)
    * json cast_vote = frontend_buffer.takeTimeout(timeout)
    Then match cast_vote contains deep {jsonrpc: '2.0', id: '#(castVoteId)', error: {code: -4, description: '#string'}}

  # Testing if after creating an election correctly, the backend returns an error
  # upon a non-attendee casting a valid vote.
  Scenario: Non attendee casting a vote should return an error
    Given string castVoteData = read('classpath:data/election/data/castVote/valid_cast_vote_data.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    * string nonAttendeePk = "oKHk3AivbpNXk_SfFcHDaVHcCcY8IBfHE7auXJ7h4ms="
    * string nonAttendeeSkHex = "0cf511d2fe4c20bebb6bd51c1a7ce973d22de33d712ddf5f69a92d99e879363b"
    * converter.setSenderSk(nonAttendeeSkHex)
    * converter.setSenderPk(nonAttendeePk)
    And string castVote = converter.publishМessageFromData(castVoteData, castVoteId, electionChannel)
    When eval frontend.send(castVote)
    * json cast_vote = frontend_buffer.takeTimeout(timeout)
    Then match cast_vote contains deep {jsonrpc: '2.0', id: '#(castVoteId)', error: {code: -4, description: '#string'}}

