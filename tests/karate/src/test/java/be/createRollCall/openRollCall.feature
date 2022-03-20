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

  Scenario: Open a valid Roll Call
    Given string rollCallOpenReq  = read('classpath:data/rollCall/open/valid_roll_call_open.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    When eval frontend.send(rollCallOpenReq)
    * karate.log("Open request has been sent : "+ rollCallOpenReq)
    * json open_roll_broadcast = frontend_buffer.takeTimeout(timeout)
    * karate.log("Received an open roll call result message : ")
    * karate.log(open_roll_broadcast)
    * json open_roll = frontend_buffer.takeTimeout(timeout)
    * karate.log(open_roll)
    Then match open_roll contains deep {jsonrpc: '2.0', id: 32, result: 0}

  Scenario: Opening a Roll Call that does not exist should return an error
    Given string rollCallOpenReq  = read('classpath:data/rollCall/open/valid_roll_call_open_2.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    When eval frontend.send(rollCallOpenReq)
    * karate.log("Open request has been sent without create beforehand: "+ rollCallOpenReq)
    * json err_open = frontend_buffer.takeTimeout(timeout)
    Then match err_open contains deep {jsonrpc: '2.0', id: 32, error: {code: -4, description: '#string'}}


  Scenario: Opening a Roll Call with invalid update_id should return an error
    Given string rollCallOpenReq  = read('classpath:data/rollCall/open/bad_roll_call_open_invalid_update_id.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    When eval frontend.send(rollCallOpenReq)
    * karate.log("Open request has been sent for invalid update id: "+ rollCallOpenReq)
    * json err_open = frontend_buffer.takeTimeout(timeout)
    Then match err_open contains deep {jsonrpc: '2.0', id: 32, error: {code: -4, description: '#string'}}
