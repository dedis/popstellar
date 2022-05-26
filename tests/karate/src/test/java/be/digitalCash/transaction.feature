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
    * string laoChannel =  "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="
    * string cashChannel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/coin"

  Scenario: Valid transaction: issue 32 mini-Laos to an attendee
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
                  "pubkey": '#(getCoinIssuancePubKey)',
                  "sig": '#(getCreateSignatureForCoinIssuance)'
                }
              }],
              "outputs": '#(getValidOutputs)',
              "lock_time": 0
            }
        }
      """
    When frontend.publish(JSON.stringify(validTransaction), cashChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validTransaction))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  Scenario: Transfer valid amount should work
    Given call read('classpath:be/utils/simpleScenarios.feature@name=setup_coin_channel')
    And def validTransfer =
      """
        {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": "lCCBbko3JyV5RT6zlIJVv1IpyST3Kc5wByzXvOsyLic=",
            "transaction": {
              "version": 1,
              "inputs": [{
                "tx_out_hash": "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=",
                "tx_out_index": 0,
                "script": {
                  "type": "P2PKH",
                  "pubkey": "2jmj7l5rSw0yVb-vlWAYkK-YBwk=",
                  "sig": '#(getCreateSignatureForCoinIssuance)'
                }
              }],
              "outputs": [{
                "value": 20,
                "script": {
                  "type": "P2PKH",
                  "pubkey_hash": "2jmj7l5rSw0yVb-vlWAYkK-YBRk="
                }
              }],
              "lock_time": 0
            }
        }
      """
    When frontend.publish(JSON.stringify(validTransfer), cashChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validTransfer))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  Scenario: Post transaction with invalid transaction id should fail
    Given call read('classpath:be/utils/simpleScenarios.feature@name=setup_coin_channel')
    And def validTransaction =
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
                  "pubkey": '#(getCoinIssuancePubKey)',
                  "sig": '#(getCreateSignatureForCoinIssuance)'
                }
              }],
              "outputs": '#(getValidOutputs)',
              "lock_time": 0
            }
        }
      """
    When frontend.publish(JSON.stringify(validTransaction), cashChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(validTransaction))
    Then match answer contains VALID_MESSAGE
    And match frontend.receiveNoMoreResponses() == true

  Scenario: Post transaction with invalid tx_out_hash should fail
    Given call read('classpath:be/utils/simpleScenarios.feature@name=setup_coin_channel')
    And def invalidTransaction =
      """
        {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": "fcDVZofQwuSUs5jz_LXGRtSz-xAV8ss4axY4GsHWnVM=",
            "transaction": {
              "version": 1,
              "inputs": [{
                "tx_out_hash": "_6BPyKnSBFUdMdUxZivzC2BLzM7j5d667BdQ4perTvc=",
                "tx_out_index": 0,
                "script": {
                  "type": "P2PKH",
                  "pubkey": '#(getCoinIssuancePubKey)',
                  "sig": '#(getCreateSignatureForCoinIssuance)'
                }
              }],
              "outputs": '#(getValidOutputs)',
              "lock_time": 0
            }
        }
      """
    When frontend.publish(JSON.stringify(invalidTransaction), cashChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(invalidTransaction))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true

  Scenario: Post transaction with invalid output pubKey should fail
    Given call read('classpath:be/utils/simpleScenarios.feature@name=setup_coin_channel')
    And def invalidTransaction =
      """
        {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": "S-UTUqrPfUVw8Ywv6AOb7Qv0M01s7-BcYCSa4SIl9bQ=",
            "transaction": {
              "version": 1,
              "inputs": [{
                "tx_out_hash": "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=",
                "tx_out_index": 0,
                "script": {
                  "type": "P2PKH",
                  "pubkey": '#(getCoinIssuancePubKey)',
                  "sig": '#(getCreateSignatureForCoinIssuance)'
                }
              }],
              "outputs": '#(getInvalidOutputs)',
              "lock_time": 0
            }
        }
      """
    When frontend.publish(JSON.stringify(invalidTransaction), cashChannel)
    And json answer = frontend.getBackendResponse(JSON.stringify(invalidTransaction))
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true
