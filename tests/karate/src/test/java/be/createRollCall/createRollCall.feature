@env=go,scala
Feature: Create a Roll Call
  Background:
        ## This is feature will be called  to
        # This call makes this feature and server.feature share the same scope
        # Meaning they share def variables, configurations ...
        # Especially JS functions defined in server.feature can be directly used here thanks to Karate shared scopes
        # * call wait <timeout>
        # * karate.set(varName, newValue)
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')


  Scenario: Valid Roll Call
    Given string rollCallReq  = read('classpath:data/rollCall/valid_roll_call_create.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')

#    When eval frontend.send(laoCreateReq)
    * karate.log('Request for lao creation sent')
    * frontend_buffer.takeTimeout(timeout)
    Then eval frontend.send(rollCallReq)
    * json roll = frontend_buffer.takeTimeout(timeout)
    Then match roll contains deep {jsonrpc: '2.0', id: 3, result: 0}

  Scenario: Roll Call Creation with empty name should return an error code
    Given string badRollCallReq  = read('classpath:data/rollCall/bad_roll_call_create_empty_data_but_same_messageId_as_valid_roll_call.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    * string validRollCallReq  = read('classpath:data/rollCall/valid_roll_call_create.json')

    When eval frontend.send(validRollCallReq)
    * karate.log('Request for roll call sent')
    * frontend_buffer.takeTimeout(timeout)

    Then eval frontend.send(badRollCallReq)
    * json roll_err = frontend_buffer.takeTimeout(timeout)
    Then match roll_err contains deep {jsonrpc: '2.0', id: 3, error: {code: -4, description: '#string'}}

  Scenario: Roll Call Creation with non-organizer as sender should return an error
    Given string badRollCallReq = read('classpath:data/rollCall/bad_create_roll_call_not_organizer_sender.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    When eval frontend.send(badRollCallReq)
    * json roll_err = frontend_buffer.takeTimeout(timeout)
    Then match roll_err contains deep {jsonrpc: '2.0', id: 3, error: {code: -4, description: '#string'}}

  Scenario: Roll Call Creation sent on root channel should return an error
    Given string badRollCallReq = read('classpath:data/rollCall/bad_roll_call_create_wrong_channel.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    When eval frontend.send(badRollCallReq)
    * json roll_err = frontend_buffer.takeTimeout(timeout)
    Then match roll_err contains deep {jsonrpc: '2.0', id: null, error: {code: -6, description: '#string'}}


  Scenario: Roll Call Creation with proposed start < proposed end should return and error
    Given string badRollCallReq = read('classpath:data/rollCall/bad_roll_call_create_start_time_bigger_than_end_time.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    When eval frontend.send(badRollCallReq)
    * json roll_err = frontend_buffer.takeTimeout(timeout)
    Then match roll_err contains deep {jsonrpc: '2.0', id: 3, error: {code: -4, description: '#string'}}

  Scenario: Roll Call Creation with creation time is negative should return an error
    Given string badRollCallReq = read('classpath:data/rollCall/bad_roll_call_create_creation_time_negative.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    When eval frontend.send(badRollCallReq)
    * json roll_err = frontend_buffer.takeTimeout(timeout)
    Then match roll_err contains deep {jsonrpc: '2.0', id: 3, error: {code: -4, description: '#string'}}

  Scenario: Roll Call Creation with creation time < proposed start should return and error
    Given string badRollCallReq = read('classpath:data/rollCall/bad_roll_call_create_creation_time_less_than_start_time.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    When eval frontend.send(badRollCallReq)
    * json roll_err = frontend_buffer.takeTimeout(timeout)
    Then match roll_err contains deep {jsonrpc: '2.0', id: 3, error: {code: -4, description: '#string'}}

  Scenario: Roll Call Creation with invalid roll_call id should return an error
    Given string badRollCallReq = read('classpath:data/rollCall/bad_roll_call_create_invalid_roll_call_id.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    When eval frontend.send(badRollCallReq)
    * json roll_err = frontend_buffer.takeTimeout(timeout)
    Then match roll_err contains deep {jsonrpc: '2.0', id: 3, error: {code: -4, description: '#string'}}
