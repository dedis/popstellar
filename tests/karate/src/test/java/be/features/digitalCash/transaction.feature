@env=go,scala
Feature: Simple Transactions for digital cash
  Background:
    # This feature will be called to test some simple transactions
    * call read('classpath:be/features/utils/constants.feature')
    * call read(serverFeature)
    * call read(mockClientFeature)
    * def organizer = call createMockFrontend
    * def recipient = call createMockFrontend
    * def lao = organizer.generateValidLao()
    * def rollCall = organizer.generateValidRollCall(lao)

    # This call executes all the steps to set up a lao, complete a roll call and subscribe to the coin channel
    * call read(setupCoinChannelScenario) { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)' }

  @transaction1
  Scenario: Valid transaction: issue 32 mini-Laos to an attendee
    Given def transaction = organizer.issueCoins(recipient, 32);
    And def postTransaction = transaction.post()
    # Because this is the first transaction, input and output used are the first elements of the list
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

  # This test fails since multiple transactions are not supported in the transaction class (05.06.2023)
  # TODO: Add support for multiple transactions in the Transaction class
  @transaction2
  Scenario: Transfer valid amount should work
    # This call issues initialAmount coins to the recipient
    Given def initialAmount = 32
    And call read(validCoinIssuanceScenario) { organizer: '#(organizer)', lao: '#(lao)', rollCall: '#(rollCall)', recipient: '#(recipient)', amount: '#(initialAmount)' }
    And def transaction = organizer.issueCoins(recipient, 20);
    And def postTransaction = transaction.post()
    And def input = transaction.inputs[1]
    And def output = transaction.outputs[1]
    And def validTransfer =
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
    When organizer.publish(validTransfer, lao.cashChannel)
    And json answer = organizer.getBackendResponse(validTransfer)
    Then match answer contains VALID_MESSAGE
    And match organizer.receiveNoMoreResponses() == true

  @transaction3
  Scenario: Post transaction with invalid transaction id should fail
    Given def transaction = organizer.issueCoins(recipient, 32);
    And def postTransaction = transaction.post()
     # Because this is the first transaction, input and output used are the first elements of the list
    And def input = transaction.inputs[0]
    And def output = transaction.outputs[0]
    And def invalidTransaction =
      """
        {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": '#(random.generateHash())',
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
    When organizer.publish(invalidTransaction, lao.cashChannel)
    And json answer = organizer.getBackendResponse(invalidTransaction)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  @transaction4
  Scenario: Post transaction with invalid tx_out_hash should fail
    Given def transaction = organizer.issueCoins(recipient, 32);
    And def postTransaction = transaction.post()
     # Because this is the first transaction, input and output used are the first elements of the list
    And def input = transaction.inputs[0]
    And def output = transaction.outputs[0]
    And def invalidTransaction =
      """
        {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": '#(postTransaction.transactionId)',
            "transaction": {
              "version": '#(transaction.version)',
              "inputs": [{
                "tx_out_hash": '#(random.generateHash())',
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
    When organizer.publish(invalidTransaction, lao.cashChannel)
    And json answer = organizer.getBackendResponse(invalidTransaction)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true

  @transaction5
  Scenario: Post transaction with invalid output pubKey should fail
    Given def transaction = organizer.issueCoins(recipient, 32);
    And def postTransaction = transaction.post()
     # Because this is the first transaction, input and output used are the first elements of the list
    And def input = transaction.inputs[0]
    And def output = transaction.outputs[0]
    And def invalidTransaction =
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
                  "pubkey_hash": '#(random.generateHash())',
                }
              }],
              "lock_time": '#(transaction.lockTime)',
            }
        }
      """
    When organizer.publish(invalidTransaction, lao.cashChannel)
    And json answer = organizer.getBackendResponse(invalidTransaction)
    Then match answer contains INVALID_MESSAGE_FIELD
    And match organizer.receiveNoMoreResponses() == true
