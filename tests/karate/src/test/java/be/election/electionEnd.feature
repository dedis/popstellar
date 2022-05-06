@env=go,scala
Feature: Terminate an election
  Background:
        # This feature will be called to test End Election
        # For every test a file containing the json representation of the message is read
        # then sent to the backend and stored there.
        # This is done via :
        # eval frontend.send(<message>) where a mock frontend sends a message to backend
        # Then the response sent by the backend and stored in a buffer :
        # json response = frontend_buffer.takeTimeout(timeout)
        # and checked if it contains the desired fields with :
        # match response contains deep <desired fields>

    # The following calls make this feature, mockFrontEnd.feature and server.feature share the same scope
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')
    * call read('classpath:be/constants.feature')
    * string electionChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/rdv-0minecREM9XidNxnQotO7nxtVVnx-Zkmfm7hm2w="
    * string laoChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="

  # After a successful election setup and cast vote sending a valid election end
  # message should succeed
  Scenario: Sending a valid election end should succeed
    Given call read('classpath:be/utils/simpleScenarios.feature@name=cast_vote')
    And def validElectionEnd =
      """
        {
          "object": "election",
          "action": "end",
          "lao": '#(getLaoValid)',
          "election": '#(getValidElectionSetupId)',
          "created_at": 1633099883,
          "registered_votes": "GX9slST3yY_Mltkjimp-eNq71mfbSbQ9sruABYN8EoM="
        }
      """
    When frontend.publish(JSON.stringify(validElectionEnd), electionChannel)
    And json answer = frontend.getBackendResponseWithBroadcast()
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true
  # After having a successful election setup and vote casts, sending an election end
  # message that has an invalid election id should return an error form the backend
#  Scenario: Sending an election end with invalid election id should return an error
#    Given string badElectionEndData = read('classpath:data/election/data/electionEnd/bad_election_end_invalid_election_id_data.json')
#    And string badElectionEnd = converter.publishМessageFromData(badElectionEndData, electionEndId, electionChannel)
#    * call read('classpath:be/utils/simpleScenarios.feature@name=cast_vote')
#    When eval frontend.send(badElectionEnd)
#    * json election_end = frontend_buffer.takeTimeout(timeout)
#    Then match election_end contains deep {jsonrpc: '2.0', id: '#(electionEndId)', error: {code: -4, description: '#string'}}
#
#  # After having a successful election setup and vote casts, sending an election end
#  # message with a wrongly computed registered votes field should return an error form the backend
#  Scenario: Sending an election end message with invalid registered votes field should return an error
#    Given string electionEndData = read('classpath:data/election/data/electionEnd/bad_election_end_invalid_registered_votes_data.json')
#    And string electionEnd = converter.publishМessageFromData(electionEndData, electionEndId, electionChannel)
#    * call read('classpath:be/utils/simpleScenarios.feature@name=cast_vote')
#    When eval frontend.send(electionEnd)
#    * json election_end = frontend_buffer.takeTimeout(timeout)
#    Then match election_end contains deep {jsonrpc: '2.0', id: '#(electionEndId)', error: {code: -4, description: '#string'}}

