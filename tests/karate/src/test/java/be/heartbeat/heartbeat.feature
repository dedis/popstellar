@env=go,scala
Feature: Send heartbeats to other servers

  Background:
    # This feature will be called to test sending heartbeats and getting messages by id's from other servers.
    # Call read(...) makes this feature and the called feature share the same scope
    # Meaning they share def variables, configurations ...
    # Especially JS functions defined in the called features can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockClient.feature')
    * call read('classpath:be/constants.feature')
    * def server = call createMockClient
    * def lao = server.createValidLao()

    # This call executes all the steps to create a valid lao on the server before every scenario
    # (lao creation, subscribe, catchup)
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao') { organizer: '#(server)', lao: '#(lao)' }

  # After lao creation, wait and do nothing (40 seconds for now) and check that a heartbeat message was received
  Scenario: Server should send heartbeat messages automatically after a time interval
    # Take timeout to make sure the heartbeat received is not the one triggered by the create lao
    Given server.takeTimeout(3)
    When def initial = server.getMessagesByMethod('heartbeat')
    Then assert initial.length == 0
    And wait(40)
    When def heartbeatMessages = server.getMessagesByMethod('heartbeat')
    Then assert heartbeatMessages.length > 0

  # Check that after sending a heartbeat message with unknown message, the server responds with a getMessagesByID request
  Scenario: Server should request the missing message id's in a heartbeat
    Given def randomMessage = random.generateHash()
    Given def heartbeat =
            """
              {
              "method": "heartbeat",
              "params": {
                  '#(lao.id)': [
                     '#(randomMessage)'
                  ],
              },
              "jsonrpc": "2.0"
              }
        """
    When server.send(heartbeat)
    And def getMessagesByIdMessages = server.getMessagesByMethod('get_messages_by_id')
    Then assert getMessagesByIdMessages.length == 1
    And match getMessagesByIdMessages.get(0) contains randomMessage
