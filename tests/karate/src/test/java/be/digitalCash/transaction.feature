@env=go,scala
Feature: Create a Roll Call
  Background:
      # This is feature will be called  to test a Roll Call Creation
      # The following calls makes this feature, mockFrontEnd.feature and server.feature
      # share the same scope
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
    * call read('classpath:be/constants.feature')
    * string laoChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="
    * string cashChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/coin"

  Scenario: Valid transaction sent by the organiser
  Scenario: Valid Roll Call
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    * def subscribe =
            """
          JSON.stringify(
              {
              "method": "subscribe",
              "id": 233,
              "params": {
                  "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/coin",
              },
              "jsonrpc": "2.0"
          })
        """
    * frontend.send(subscribe)
    * def subs = frontend_buffer.takeTimeout(timeout)
    * karate.log("subscribe message received : " + subs)
    * def catchup =
      """
          JSON.stringify(
              {
              "method": "catchup",
              "id": 533,
              "params": {
                  "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/coin",
              },
              "jsonrpc": "2.0"
          })
      """
    * frontend.send(catchup)
    * def catchup_response = frontend_buffer.takeTimeout(timeout)
    And def validTransaction =
      """
        {
            "object": "transaction",
            "action": "post",
            "transaction_id": "zxNUqE_8PFK-Yb8LmXWtm4ZX0Mo6QsC3ugtg-9kRf4w=",
            "transaction": {
              "Version": 1,
              "TxIn": [],
              "TxOut": [{
                "Value": 32,
                "Script": {
                  "Type": "P2PKH",
                  "PubkeyHash": "2jmj7l5rSw0yVb-vlWAYkK-YBwk="
                }
              }],
              "LockTime": 0
            }
        }
      """
    When frontend.publish(JSON.stringify(validTransaction), cashChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validTransaction))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true
