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
          "registered_votes": '#(getValidRegisteredVotes)'
        }
      """
    When frontend.publish(validElectionEnd, electionChannel)
    And json answer = frontend.getBackendResponseWithElectionResults(validElectionEnd)
    Then match answer contains ELECTION_RESULTS
    And match frontend.receiveNoMoreResponses() == true

   # After having a successful election setup and vote casts, sending an election end
   # message that has an invalid election id should return an error form the backend
  Scenario: Sending an election end with invalid election id should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=cast_vote')
    And def invalidElectionEnd =
      """
        {
          "object": "election",
          "action": "end",
          "lao": '#(getLaoValid)',
          "election": '#(getInvalidElectionSetupId)',
          "created_at": 1633099883,
          "registered_votes": '#(getValidRegisteredVotes)'
        }
      """
    When frontend.publish(invalidElectionEnd, electionChannel)
    And json answer = frontend.getBackendResponse(invalidElectionEnd)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # After having a successful election setup and vote casts, sending an election end
  # message with a wrongly computed registered votes field should return an error form the backend
  Scenario: Sending an election end message with invalid registered votes field should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=cast_vote')
    And def invalidElectionEnd =
      """
        {
          "object": "election",
          "action": "end",
          "lao": '#(getLaoValid)',
          "election": '#(getValidElectionSetupId)',
          "created_at": 1633099883,
          "registered_votes": '#(getInvalidRegisteredVotes)'
        }
      """
    When frontend.publish(invalidElectionEnd, electionChannel)
    And json answer = frontend.getBackendResponse(invalidElectionEnd)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # After having a successful election setup and vote casts, sending an election end
  # message with a wrongly computed registered votes field should return an error form the backend
  Scenario: Sending an election end message with registered votes field set to valid election setup id should return an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=cast_vote')
    And def invalidElectionEnd =
      """
        {
          "object": "election",
          "action": "end",
          "lao": '#(getLaoValid)',
          "election": '#(getValidElectionSetupId)',
          "created_at": 1633099883,
          "registered_votes": '#(getValidElectionSetupId)'
        }
      """
    When frontend.publish(invalidElectionEnd, electionChannel)
    And json answer = frontend.getBackendResponse(invalidElectionEnd)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # Testing if sending an election end message with timestamp that is before the election setup
  # creation time should result in an error from the back-end
  Scenario: Sending a valid election end too early should fail
    Given call read('classpath:be/utils/simpleScenarios.feature@name=cast_vote')
    And def invalidElectionEnd =
      """
        {
          "object": "election",
          "action": "end",
          "lao": '#(getLaoValid)',
          "election": '#(getValidElectionSetupId)',
          "created_at": 1633098900,
          "registered_votes": '#(getValidRegisteredVotes)'
        }
      """
    When frontend.publish(invalidElectionEnd, electionChannel)
    And json answer = frontend.getBackendResponse(invalidElectionEnd)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true
