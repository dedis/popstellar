Feature: Create a RollCall
  # TODO: Add missing test data
  Background:
    * call read('classpath:be/utils/server.feature')
    * def createLAO = read('classpath:be/createLAO/create.feature@name=createLAO')
    * def badMessageDataTest = read('classpath:be/common/badMessageDataRequest.feature')
    * def valideRollCall = {} /
    * def success = {jsonrpc: "2.0", id: #(valideRollCall.id), result: 0}
    * def socket = karate.webSocket(wsURL,handle)

  @name=rollcall
  Scenario: Create RollCall with valid request succeeds
    Given call createLAO
    * karate.log('RollCall: ' + karate.pretty(valideRollCall))
    And string success = ""
    When eval socket.send(valideRollCall)
    And string answer = socket.listen(timeout)
    * karate.log('Received answer = ' + answer)
    Then match answer == success

  Scenario: Creating two different RollCalls succeeds with valid ID
    Given call createLAO
    # First roll call same lao
    And valideRollCall, success
    # Second different rollcall same lao
    And string createRollCall_2nd = ""
    *   json succes_2nd = {"jsonrpc": "2.0","id":  #(valideRollCall.id),"result": 0}
    * karate.log('Creating first rollcall...')
    When eval socket.send(valideRollCall)
    * listen timeout
    * karate.log('First response = ' + listenResult')
    Then match listenResult == success

    * karate.log('Creating second rollcall...')
    When eval socket.send(createRollCall_2nd)
    * karate.log('Second response = ' + listenResult)
    * listen timeout
    Then match listenResult == success_2nd

  Scenario: Creating same/duplicate RollCalls should fail with correct error
    Given call createLAO
    And valideRollCall, success
    * karate.log('Creating rollcall...')
    When eval socket.send(createRollCall)
    * listen timeout
    * karate.log('Response = ' + listenResult')
    Then match listenResult == success
    * karate.log('Creating same rollcall again...')
    When eval socket.send(createRollCall)
    * listen timeout
    * karate.log('Response = ' + listenResult')
    * json answer = listenResult
    Then match answer == {jsonrpc: '2.0', id: 1, error: {code: -3, description: '#string'}}

  Scenario: Create RollCall without existing LAO should failwith correct error code
    Given valideRollCall
    * def socket = karate.webSocket(wsURL,handle)
    When eval socket.send(valideRollCall)
    And json answer = socket.listen(timeout)
    * karate.log('Received answer = ' + answer)
    Then match answer == {jsonrpc: '2.0', id: 1, error: {code: -2, description: '#string'}}

  Scenario: Create RollCall with invalid/empty name should fail with correct error code
    Given def invalidRollCall = ""
    And   json args = {bad_request: #(invalidRollCall), err_code: -4}
    Then  call badMessageDataTest args

  Scenario: Create RollCall with invalid creation timestamp should fail with correct error code
    Given def invalidRollCall = ""
    And   json args = {bad_request: #(invalidRollCall), err_code: -4}
    Then  call badMessageDataTest args

  Scenario: Create RollCall with invalid proposed start timestamp should fail with correct error code
    Given def invalidRollCall = ""
    And   json args = {bad_request: #(invalidRollCall), err_code: -4}
    Then  call badMessageDataTest args

  Scenario: Create RollCall with invalid proposed end timestamp should fail with correct error code
    Given def invalidRollCall = ""
    And   json args = {bad_request: #(invalidRollCall), err_code: -4}
    Then  call badMessageDataTest args

  Scenario: Create RollCall with empty location should fail with correct error code
    Given def invalidRollCall = ""
    And   json args = {bad_request: #(invalidRollCall), err_code: -4}
    Then  call badMessageDataTest args
