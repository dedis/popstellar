@env=go,scala
Feature: Open an Election
  Background:
    # This feature will be called to test Election Open
    * call read('classpath:be/features/utils/constants.feature')
    * call read(serverFeature)
    * call read(mockClientFeature)
    * def organizer = call createMockFrontend
    * def lao = organizer.createValidLao()
    * def rollCall = organizer.createValidRollCall(lao)
    * def election = organizer.createValidElection(lao)
    * def question = election.createQuestion()

    # This call executes all the steps to set up a lao, complete a roll call and create an election with one question
    * call read(setupElectionScenario) { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)',  election: '#(election)', question: '#(question)' }
    * def electionOpen = election.open()

  # Testing after creating an election, the backend returns an result
  # upon an open election message
  @electionOpen1
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
  @electionOpen2
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
  @electionOpen3
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
  @electionOpen4
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
  @electionOpen5
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
  @electionOpen6
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

