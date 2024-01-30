@env=go,scala
Feature: Request messages by id from other servers

  Background:
    # This feature will be called to test how servers request messages in response to heartbeat messages.
    # Call read(...) makes this feature and the called feature share the same scope
    # Meaning they share def variables, configurations ...
    # Especially JS functions defined in the called features can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/features/utils/constants.feature')
    * call read(serverFeature)
    * call read(mockClientFeature)
    * def mockServer = call createMockBackend
    * def mockFrontend = call createMockFrontend
    * def lao = mockFrontend.createValidLao()

    # Create the template for heartbeat message
    # This is used in combination with 'eval' to dynamically resolve the channel keys in the heartbeat JSON
    # The format used in other features only works to dynamically resolve values, not keys.
    * eval var heartbeat = { method: "heartbeat", params: {}, jsonrpc: "2.0" }

    # Create a list of messages with a random message id
    * def randomMessageId = random.generateHash()
    * def messageIds = []
    * eval messageIds.push(randomMessageId)

    # This call executes all the steps to create a valid lao on the server before every scenario
    # (lao creation, subscribe, catchup)
    * call read(createLaoScenario) { organizer: '#(mockFrontend)', lao: '#(lao)' }

  # Check that after sending a heartbeat message with unknown message id, the server responds with a
  # getMessagesByID requesting this message
  Scenario: Server should request the missing message ids in a heartbeat
    Given eval heartbeat.params[lao.channel] = messageIds

    When mockServer.send(heartbeat)
    And def getMessagesByIdMessages = mockServer.getGetMessagesById()

    Then assert getMessagesByIdMessages.length == 1
    And match getMessagesByIdMessages[0] contains randomMessageId

  # Check that after sending a heartbeat message with unknown message id on a channel missing the /root/'
  # prefix, the server does not request the messages
  Scenario: Server should not request messages if channel is missing '/root/' prefix
    Given eval heartbeat.params[lao.id] = messageIds

    When mockServer.send(heartbeat)
    And def getMessagesByIdMessages = mockServer.getGetMessagesById()

    Then assert getMessagesByIdMessages.length == 0

  # Check that after sending a heartbeat message with invalid message ids, the server does not request the messages
  Scenario: Server should not request messages for invalid lao ids
    Given def invalidMessageIds = []
    And eval invalidMessageIds.push('invalid message id')
    And eval heartbeat.params[lao.channel] = invalidMessageIds

    When mockServer.send(heartbeat)

    Then assert mockServer.getGetMessagesById().length == 0


  # Check that after the server confirms it received a message, sending a heartbeat containing that message id does not
  # trigger a getMessagesById anymore
  Scenario: Server should not request messages that it already has
    Given def validRollCall = mockFrontend.createValidRollCall(lao)
    And def validCreateRollCall =
      """
        {
          "object": "roll_call",
          "action": "create",
          "id": '#(validRollCall.id)',
          "name": '#(validRollCall.name)',
          "creation": '#(validRollCall.creation)',
          "proposed_start": '#(validRollCall.start)',
          "proposed_end": '#(validRollCall.end)',
          "location": '#(validRollCall.location)',
          "description": '#(validRollCall.description)',
        }
      """

    And mockFrontend.publish(validCreateRollCall, lao.channel)
    And json answer = mockFrontend.getBackendResponse(validCreateRollCall)
    And def message_id = mockFrontend.getPublishMessageId(validCreateRollCall)
    And def messageIds = []
    And eval messageIds.push(message_id)
    And eval heartbeat.params[lao.channel] = messageIds

    When mockServer.send(heartbeat)

    Then match answer contains VALID_MESSAGE
    And assert mockServer.getGetMessagesById().length == 0



