@env=go,scala
Feature: Terminate an election
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
    * def electionEndId = 42
    * string electionChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/ZVxXK2QN60uCNxNsIzShYYQmtwGttWLpQPQapYCNg4g="
    * string laoChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="

  Scenario: Sending a valid election end should succeed
    Given string electionEndData = read('classpath:data/election/data/electionEnd/valid_election_end_data.json')
    And string electionEnd = converter.publishМessageFromData(electionEndData, electionEndId, electionChannel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=cast_vote')
    And eval frontend.send(electionEnd)
    * json election_end = frontend_buffer.takeTimeout(timeout)
    Then match election_end contains deep {jsonrpc: '2.0', id: '#(electionEndId)', result: 0}

  Scenario: Sending an election end with invalid election id
    Given string badElectionEndData = read('classpath:data/election/data/electionEnd/bad_election_end_invalid_election_id_data.json')
    And string badElectionEnd = converter.publishМessageFromData(badElectionEndData, electionEndId, electionChannel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=cast_vote')
    And eval frontend.send(badElectionEnd)
    * json election_end = frontend_buffer.takeTimeout(timeout)
    Then match election_end contains deep {jsonrpc: '2.0', id: '#(electionEndId)', error: {code: -4, description: '#string'}}

  Scenario: Sending an election end message with invalid registered votes field should return an error
    Given string electionEndData = read('classpath:data/election/data/electionEnd/bad_election_end_invalid_registered_votes_data.json')
    And string electionEnd = converter.publishМessageFromData(electionEndData, electionEndId, electionChannel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=cast_vote')
    And eval frontend.send(electionEnd)
    * json election_end = frontend_buffer.takeTimeout(timeout)
    Then match election_end contains deep {jsonrpc: '2.0', id: '#(electionEndId)', error: {code: -4, description: '#string'}}

