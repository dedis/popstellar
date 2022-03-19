@env=go,scala
Feature: Join a Roll Call
  Background:
        ## This is feature will be called  to
        # This call makes this feature and server.feature share the same scope
        # Meaning they share def variables, configurations ...
        # Especially JS functions defined in server.feature can be directly used here thanks to Karate shared scopes
        # * call wait <timeout>
        # * karate.set(varName, newValue)
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')

  Scenario: Create a valid open Roll Call
    Given string rollCallOpenReq  = read('classpath:data/rollCall/open/valid_roll_call_open.json')
    * def roll_call = call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    When eval frontend.send(rollCallOpenReq)
    * karate.log("Open request has been sent : "+ rollCallOpenReq)
    * def create_roll_broadcast = frontend_buffer.takeTimeout(timeout)
    * json open_roll = frontend_buffer.takeTimeout(timeout)
    Then match open_roll contains deep {jsonrpc: '2.0', id: 32, result: 0}
