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
    * def id = 42
    * string channel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/rdv-0minecREM9XidNxnQotO7nxtVVnx-Zkmfm7hm2w="
    * string laoChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="

  Scenario: Sending a valid election end should succeed
    Given string electionEndData = read('classpath:data/election/data/electionEnd/valid_election_end_data.json')
    And string electionEnd = converter.publishМessageFromData(electionEndData, id, channel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    And eval frontend.send(electionEnd)
    * json election_end = frontend_buffer.takeTimeout(timeout)
    Then match election_end contains deep {jsonrpc: '2.0', id: '#(id)', result: 0}


