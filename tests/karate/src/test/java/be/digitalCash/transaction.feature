@env=go,scala
Feature: Simple Transactions for digital cash
  Background:
    # This feature will be called to test some simple transactions
    # Call read(...) makes this feature and the called feature share the same scope
    # Meaning they share def variables, configurations ...
    # Especially JS functions defined in the called features can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockClient.feature')
    * call read('classpath:be/constants.feature')
    * def organizer = call createMockClient
    * def recipient = call createMockClient
    * def lao = organizer.createValidLao()
    * def rollCall = organizer.createValidRollCall(lao)

    # This call executes all the steps to set up a lao, complete a roll call and subscribe to the coin channel
    * call read('classpath:be/utils/simpleScenarios.feature@name=setup_coin_channel') { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)' }

  Scenario: Valid transaction: issue 32 mini-Laos to an attendee
    Given def transaction = organizer.issueCoins(recipient, 32);
    And def postTransaction = transaction.post()
    And def input = transaction.inputs[0]
    And def output = transaction.outputs[0]
    And def validTransaction =
      """
        {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": '#(postTransaction.transactionId)',
            "transaction": {
              "version": '#(transaction.version)',
              "inputs": [{
                "tx_out_hash": '#(input.txOutHash)',
                "tx_out_index": '#(input.txOutIndex)',
                "script": {
                  "type": '#(input.script.type)',
                  "pubkey": '#(input.script.pubKeyRecipient)',
                  "sig": '#(input.script.sig)'
                }
              }],
              "outputs": [{
                "value": '#(output.value)',
                "script": {
                  "type": '#(output.script.type)',
                  "pubkey_hash": '#(output.script.pubKeyHash)',
                }
              }],
              "lock_time": '#(transaction.lockTime)',
            }
        }
      """
    When organizer.publish(validTransaction, lao.cashChannel)
    And json answer = organizer.getBackendResponse(validTransaction)
    Then match answer contains VALID_MESSAGE
    And match organizer.receiveNoMoreResponses() == true

  Scenario: Transfer valid amount should work
    Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_coin_issuance')
    And def validTransfer =
      """
        {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": "X6dMVyy-4YZ3jyePMlqyo53-eYBkO-gVgy7TybVgd78=",
            "transaction": {
              "version": 1,
              "inputs": [{
                "tx_out_hash": "yVMgw2E9IMX7JtNfizTqTOR1scMVSHfEe8WBbiAgsA8=",
                "tx_out_index": 1,
                "script": {
                  "type": "P2PKH",
                  "pubkey": '#(getCoinIssuancePubKey)',
                  "sig": "uTrQk9yt-pmG7eWA0dQ50Q4_aloIAwkY_smQml1lswjHp8ckUXAF3Th6xxJY_3-7uLNxpRtTzwBcAixPGjThDg=="
                }
              }],
              "outputs": [{
                "value": 20,
                "script": {
                  "type": "P2PKH",
                  "pubkey_hash": "-_qR4IHwsiq50raa8jURNArds54="
                }
              }],
              "lock_time": 0
            }
        }
      """
    When frontend.publish(validTransfer, cashChannel)
    And json answer = frontend.getBackendResponse(validTransfer)
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
                "tx_out_hash": "yVMgw2E9IMX7JtNfizTqTOR1scMVSHfEe8WBbiAgsA8=",
                "tx_out_index": 1,
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
    When frontend.publish(validTransaction, cashChannel)
    And json answer = frontend.getBackendResponse(validTransaction)
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
    When frontend.publish(invalidTransaction, cashChannel)
    And json answer = frontend.getBackendResponse(invalidTransaction)
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
    When frontend.publish(invalidTransaction, cashChannel)
    And json answer = frontend.getBackendResponse(invalidTransaction)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match frontend.receiveNoMoreResponses() == true
