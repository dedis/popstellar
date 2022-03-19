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

  Scenario: Close a valid roll cad
    Given string rol_call_close = read('classpath:data/rollCall/close/valid_roll_call_close.json')
    * def roll_call_open = call read('classpath:be/utils/simpleScenarios.feature@name=open_roll_call')
    Then  eval frontend.send(rol_call_close)
    Then json close_roll_result = frontend_buffer.takeTimeout(timeout)
    * match close_roll_result contains deep {jsonrpc: '2.0', id: 33, result: 0}
