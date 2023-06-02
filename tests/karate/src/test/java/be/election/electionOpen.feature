@env=go,scala
Feature: Open an Election
  Background:
    # This feature will be called to test Election Open
    # Call read(...) makes this feature and the called feature share the same scope
    # Meaning they share def variables, configurations ...
    # Especially JS functions defined in the called features can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockClient.feature')
    * call read('classpath:be/constants.feature')
    * def organizer = call createMockClient
    * def lao = organizer.createValidLao()
    * def rollCall = organizer.createValidRollCall(lao)
    * def election = organizer.createValidElection(lao)
    * def question = election.addRandomQuestion()

    # This call executes all the steps to set up a lao, complete a roll call and create an election
    * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)',  election: '#(election)' }

  # Testing after creating an election, the backend returns an result
  # upon an open election message
  Scenario: Opening a valid election
    And def validElectionOpen =
      """
        {
            "object": "election",
            "action": "open",
            "lao": '#(lao.id)',
            "election": '#(election.id)',
            "opened_at": '#(election.creation)'
        }
      """
    When organizer.publish(validElectionOpen, election.channel)
    And json answer = organizer.getBackendResponse(validElectionOpen)
    Then match answer contains VALID_MESSAGE
    And match organizer.receiveNoMoreResponses() == true

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
    When frontend.publish(validElectionOpen, electionChannel)
    And json answer = frontend.getBackendResponse(validElectionOpen)
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
    When frontend.publish(validElectionOpen, electionChannel)
    And json answer = frontend.getBackendResponse(validElectionOpen)
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
    When frontend.publish(validElectionOpen, electionChannel)
    And json answer = frontend.getBackendResponse(validElectionOpen)
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
    When frontend.publish(validElectionOpen, electionChannel)
    And json answer = frontend.getBackendResponse(validElectionOpen)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

