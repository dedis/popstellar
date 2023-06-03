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
    * def question = election.createQuestion()

    # This call executes all the steps to set up a lao, complete a roll call and create an election with one question
    * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)',  election: '#(election)', question: '#(question)' }
    * def electionOpen = election.open()

  # Testing after creating an election, the backend returns an result
  # upon an open election message
  Scenario: Opening a valid election
    Given def validElectionOpen =
      """
        {
            "object": "election",
            "action": "open",
            "lao": '#(lao.id)',
            "election": '#(election.id)',
            "opened_at": '#(electionOpen.openedAt)'
        }
      """
    When organizer.publish(validElectionOpen, election.channel)
    And json answer = organizer.getBackendResponse(validElectionOpen)
    Then match answer contains VALID_MESSAGE
    And match organizer.receiveNoMoreResponses() == true

  # Testing before creating an election, the backend returns an error
  # upon an open election message
  Scenario: Opening the election without a setup should result in an error
    Given def newElection = organizer.createValidElection(lao)
    And def newElectionOpen = newElection.open()
    And def validElectionOpen =
      """
        {
            "object": "election",
            "action": "open",
            "lao": '#(lao.id)',
            "election": '#(newElection.id)',
            "opened_at": '#(newElectionOpen.createdAt)'
        }
      """
    When organizer.publish(validElectionOpen, newElection.channel)
    And json answer = organizer.getBackendResponse(validElectionOpen)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Testing after creating an election, the backend returns an error
  # upon an open election message containing an open time before election creation time
  Scenario: Opening the election too early results in an error
    Given def invalidElectionOpen =
      """
        {
            "object": "election",
            "action": "open",
            "lao": '#(lao.id)',
            "election": '#(election.id)',
            "opened_at": '#(election.creation - 1)'
        }
      """
    When organizer.publish(invalidElectionOpen, election.channel)
    And json answer = organizer.getBackendResponse(invalidElectionOpen)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Testing after creating an election, the backend returns an error
  # if an open election message is sent by a non-organizer
  Scenario: Non organizer opening the election should result in an error
    Given def notOrganizer = call createMockClient
    And def validElectionOpen =
      """
        {
            "object": "election",
            "action": "open",
            "lao": '#(lao.id)',
            "election": '#(election.id)',
            "opened_at": '#(electionOpen.openedAt)'
        }
      """
    When notOrganizer.publish(validElectionOpen, election.channel)
    And json answer = notOrganizer.getBackendResponse(validElectionOpen)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match notOrganizer.receiveNoMoreResponses() == true

  # Testing after creating an election, the backend returns an error
  # if an open election message has the lao id field computed wrongly
  Scenario: Opening the election with wrong lao id should result in an error
    Given def invalidElectionOpen =
       """
        {
            "object": "election",
            "action": "open",
            "lao": '#(random.generateLaoId())',
            "election": '#(election.id)',
            "opened_at": '#(electionOpen.openedAt)'
        }
      """
    When organizer.publish(invalidElectionOpen, election.channel)
    And json answer = organizer.getBackendResponse(invalidElectionOpen)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  # Testing after creating an election, the backend returns an error
  # if an open election message has the election id field computed wrongly
  Scenario: Opening the election with wrong election id should result in an error
    Given def invalidElectionOpen =
       """
        {
            "object": "election",
            "action": "open",
            "lao": '#(lao.id)',
            "election": '#(random.generateElectionId())',
            "opened_at": '#(electionOpen.openedAt)'
        }
      """
    When organizer.publish(invalidElectionOpen, election.channel)
    And json answer = organizer.getBackendResponse(invalidElectionOpen)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

