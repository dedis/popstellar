@env=go,scala
Feature: Cast a vote
  Background:
        # This is feature will be called to test Cast Vote
        # For every test a file containing the json representation of the message is read
        # and is sent to the backend this is done via :
        # eval frontend.send(<message>) where a mock frontend sends a message to backend
        # Then the response sent by the backend and stored in a buffer :
        # json response = frontend_buffer.takeTimeout(timeout)
        # and checked if it contains the desired fields with :
        # match response contains deep <desired fields>

    # The following calls makes this feature, mockFrontEnd.feature and server.feature share the same scope
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')
    * def id = 41
    * string channel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/ZVxXK2QN60uCNxNsIzShYYQmtwGttWLpQPQapYCNg4g="
    * string rootChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="

  # Testing if after setting up a valid lao, subscribing to it, sending a catchup
  # creating a valid election setup that casting a valid vote succeeds
  Scenario: Casting a valid vote on a started election
    Given string castVoteData = read('classpath:data/election/data/castVote/valid_cast_vote_data.json')
    And string castVote = converter.publishМessageFromData(castVoteData, id, channel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    And eval frontend.send(castVote)
    * json cast_vote = frontend_buffer.takeTimeout(timeout)
    Then match cast_vote contains deep {jsonrpc: '2.0', id: '#(id)', result: 0}

  Scenario: Casting a valid vote on non existent election should return an error
    Given string badCastVoteData = read('classpath:data/election/data/castVote/bad_cast_vote_invalid_election_id_data.json')
    And string badCastVote = converter.publishМessageFromData(badCastVoteData, id, channel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    And eval frontend.send(badCastVote)
    * json cast_vote = frontend_buffer.takeTimeout(timeout)
    Then match cast_vote contains deep {jsonrpc: '2.0', id: '#(id)', error: {code: -4, description: '#string'}}

  Scenario: Casting a valid vote with wrong vote id should return an error
    Given string badCastVoteData = read('classpath:data/election/data/castVote/bad_cast_vote_invalid_vote_id_data.json')
    And string badCastVote = converter.publishМessageFromData(badCastVoteData, id, channel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    And eval frontend.send(badCastVote)
    * json cast_vote = frontend_buffer.takeTimeout(timeout)
    Then match cast_vote contains deep {jsonrpc: '2.0', id: '#(id)', error: {code: -4, description: '#string'}}

  Scenario: Casting a valid vote too late should return an error
    Given string badCastVoteData = read('classpath:data/election/data/castVote/bad_cast_vote_late_vote_data.json')
    And string badCastVote = converter.publishМessageFromData(badCastVoteData, id, channel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    And eval frontend.send(badCastVote)
    * json cast_vote = frontend_buffer.takeTimeout(timeout)
    Then match cast_vote contains deep {jsonrpc: '2.0', id: '#(id)', error: {code: -4, description: '#string'}}

