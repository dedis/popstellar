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

  Scenario: Valid transaction
    Given call read('classpath:be/utils/simpleScenarios.feature@name=setup_coin_channel')
    And def validTransaction =
      """
        {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": "_6BPyKnSBFUdMdUxZivzC2BLzM7j5d667BdQ4perTvc=",
            "transaction": {
              "version": 1,
              "inputs": [{
                "tx_out_hash": "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=",
                "tx_out_index": 0,
                "script": {
                  "type": "P2PKH",
                  "pubkey": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                  "sig": "CAFEBABE"
                }
              }],
              "outputs": [{
                "value": 32,
                "script": {
                  "type": "P2PKH",
                  "pubkey_hash": "2jmj7l5rSw0yVb-vlWAYkK-YBwk="
                }
              }],
              "lock_time": 0
            }
        }
      """
    When frontend.publish(JSON.stringify(validTransaction), cashChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validTransaction))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  Scenario: Post transaction with an invalid transaction id should fail
    Given call read('classpath:be/utils/simpleScenarios.feature@name=setup_coin_channel')
    And def validTransfer =
      """
        {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": "_6BPyKnSBFUdMdUxZivzC2BLzM7j5d667BdQ4perTvc=",
            "transaction": {
              "version": 1,
              "inputs": [{
                "tx_out_hash": "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=",
                "tx_out_index": 0,
                "script": {
                  "type": "P2PKH",
                  "pubkey": "2jmj7l5rSw0yVb-vlWAYkK-YBwk=",
                  "sig": "CAFEBABE"
                }
              }],
              "outputs": [{
                "value": 32,
                "script": {
                  "type": "P2PKH",
                  "pubkey_hash": "2jmj7l5rSw0yVb-vlWAYkK-YBwk="
                }
              }],
              "lock_time": 0
            }
        }
      """
    When frontend.publish(JSON.stringify(validTransfer), cashChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validTransaction))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  Scenario: Payment to another attendee once you have money should work
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_coin_issuance')
    And def invalidTransaction =
    """
        {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=",
            "transaction": {
              "version": 1,
              "inputs": [{
                "tx_out_hash": "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=",
                "tx_out_index": 0,
                "script": {
                  "type": "P2PKH",
                  "pubkey": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                  "sig": "CAFEBABE"
                }
              }],
              "outputs": [{
                "value": 32,
                "script": {
                  "type": "P2PKH",
                  "pubkey_hash": "2jmj7l5rSw0yVb-vlWAYkK-YBwk="
                }
              }],
              "lock_time": 0
            }
        }
      """


  Scenario: Post transaction with invalid tx_out_hash should fail
    Given call read('classpath:be/utils/simpleScenarios.feature@name=setup_coin_channel')
    And def invalidTransaction =
      """
        {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": "_6BPyKnSBFUdMdUxZivzC2BLzM7j5d667BdQ4perTvc=",
            "transaction": {
              "version": 1,
              "inputs": [{
                "tx_out_hash": "_6BPyKnSBFUdMdUxZivzC2BLzM7j5d667BdQ4perTvc=",
                "tx_out_index": 0,
                "script": {
                  "type": "P2PKH",
                  "pubkey": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                  "sig": "CAFEBABE"
                }
              }],
              "outputs": [{
                "value": 32,
                "script": {
                  "type": "P2PKH",
                  "pubkey_hash": "2jmj7l5rSw0yVb-vlWAYkK-YBwk="
                }
              }],
              "lock_time": 0
            }
        }
      """
    When frontend.publish(JSON.stringify(invalidTransaction), cashChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validTransaction))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  Scenario: Post transaction with invalid output pubkey should fail
    Given call read('classpath:be/utils/simpleScenarios.feature@name=setup_coin_channel')
    And def invalidTransaction =
      """
        {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": "_6BPyKnSBFUdMdUxZivzC2BLzM7j5d667BdQ4perTvc=",
            "transaction": {
              "version": 1,
              "inputs": [{
                "tx_out_hash": "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=",
                "tx_out_index": 0,
                "script": {
                  "type": "P2PKH",
                  "pubkey": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                  "sig": "CAFEBABE"
                }
              }],
              "outputs": [{
                "value": 32,
                "script": {
                  "type": "P2PKH",
                  "pubkey_hash": "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU="
                }
              }],
              "lock_time": 0
            }
        }
      """
    When frontend.publish(JSON.stringify(invalidTransaction), cashChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validTransaction))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true
