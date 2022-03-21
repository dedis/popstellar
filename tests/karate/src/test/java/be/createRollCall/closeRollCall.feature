@env=go,scala
Feature: Close a Roll Call
  Background:
        ## This is feature will be called  to
        # This call makes this feature and server.feature share the same scope
        # Meaning they share def variables, configurations ...
        # Especially JS functions defined in server.feature can be directly used here thanks to Karate shared scopes
        # * call wait <timeout>
        # * karate.set(varName, newValue)
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')

  Scenario: Close a valid roll should succeed
    Given string rol_call_close = read('classpath:data/rollCall/close/valid_roll_call_close.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=open_roll_call')
    And  eval frontend.send(rol_call_close)
    * json close_roll_broadcast = frontend_buffer.takeTimeout(timeout)
    * json close_roll_result = frontend_buffer.takeTimeout(timeout)
    Then match close_roll_result contains deep {jsonrpc: '2.0', id: 33, result: 0}

  Scenario: Close a valid roll call with wrong update_id should return an error message
    Given string rol_call_close = read('classpath:data/rollCall/close/bad_roll_call_close_invalid_update_id.json')
    And  eval frontend.send(rol_call_close)
    * json close_roll_err = frontend_buffer.takeTimeout(timeout)
    Then  match close_roll_err contains deep {jsonrpc: '2.0', id: 33, error: {code: -4, description: '#string'}}

  Scenario: Close a valid roll call that was never opened should return an error
    Given string rol_call_close = read('classpath:data/rollCall/close/valid_roll_call_close_2.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    And eval frontend.send(rol_call_close)
    * json close_roll_err = frontend_buffer.takeTimeout(timeout)
    Then  match close_roll_err contains deep {jsonrpc: '2.0', id: 33, error: {code: -4, description: '#string'}}

