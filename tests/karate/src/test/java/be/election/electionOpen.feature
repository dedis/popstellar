@env=go,scala
Feature: Open an Election
  Background:
        # This feature will be called to test Election Open
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

  # Testing after creating an election, the backend returns an result
  # upon an open election message
  Scenario: Opening a valid election
    Given call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    And def validElectionOpen =
      """
        {
            "object": "election",
            "action": "open",
            "lao": '#(getLaoValid)',
            "election": '#(getValidElectionSetupId)',
            "opened_at": 1633098944
        }
      """
    When frontend.publish(JSON.stringify(validElectionOpen), electionChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validElectionOpen))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  # Testing before creating an election, the backend returns an error
  # upon an open election message
  Scenario: Opening the election without a setup should result in an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=close_roll_call')
    And def validElectionOpen =
      """
        {
            "object": "election",
            "action": "open",
            "lao": '#(getLaoValid)',
            "election": '#(getValidElectionSetupId)',
            "opened_at": 1633098944
        }
      """
    When frontend.publish(JSON.stringify(validElectionOpen), electionChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validElectionOpen))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # Testing after creating an election, the backend returns an error
  # upon an open election message containing an open time before election creation time
  Scenario: Opening the election too early results in an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    And def validElectionOpen =
      """
        {
            "object": "election",
            "action": "open",
            "lao": '#(getLaoValid)',
            "election": '#(getValidElectionSetupId)',
            "opened_at": 1620000000
        }
      """
    When frontend.publish(JSON.stringify(validElectionOpen), electionChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validElectionOpen))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # Testing after creating an election, the backend returns an error
  # if an open election message is sent by a non-organizer
  Scenario: Non organizer opening the election should result in an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    And def validElectionOpen =
      """
        {
            "object": "election",
            "action": "open",
            "lao": '#(getLaoValid)',
            "election": '#(getValidElectionSetupId)',
            "opened_at": 1633098944
        }
      """
    And frontend.changeSenderToBeNonAttendee()
    When frontend.publish(JSON.stringify(validElectionOpen), electionChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validElectionOpen))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  # Testing after creating an election, the backend returns an error
  # if an open election message has the lao id field computed wrongly
  Scenario: Opening the election with wrong election id should result in an error
    Given call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
    And def validElectionOpen =
      """
        {
            "object": "election",
            "action": "open",
            "lao": '#(getLaoIdNegativeTime)',
            "election": '#(getValidElectionSetupId)',
            "opened_at": 1633098944
        }
      """
    When frontend.publish(JSON.stringify(validElectionOpen), electionChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validElectionOpen))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

