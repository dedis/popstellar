@env=go,scala
Feature: Open an Election
  Background:
        # This is feature will be called to test Election Open
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

  # Testing if after a successful election setup, sending a valid election
  # open results in a valid response from the backend
  Scenario: Opening a valid election
    Given call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    And def validElectionOpen =
      """
        {
            "object": "election",
            "action": "open",
            "lao": "p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=",
            "election": "rdv-0minecREM9XidNxnQotO7nxtVVnx-Zkmfm7hm2w=",
            "opened_at": 1633098944
        }
      """
    When frontend.publish(JSON.stringify(validElectionOpen), electionChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validElectionOpen))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true
