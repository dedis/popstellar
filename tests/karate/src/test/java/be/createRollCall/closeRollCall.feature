@env=go,scala
Feature: Close a Roll Call
  Background:
        # This is feature will be called to test Roll Call close
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

  # Testing if after setting up a valid lao, subscribing to it, sending a catchup
  # creating a valid roll call and opening it, we send a valid rollc call close
  # message and expect to receive a valid response from the backend
  Scenario: Close a valid roll should succeed
    Given string rollCallClose = read('classpath:data/rollCall/close/valid_roll_call_close.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=open_roll_call')
    And  eval frontend.send(rollCallClose)
    * json close_roll_broadcast = frontend_buffer.takeTimeout(timeout)
    * json close_roll_result = frontend_buffer.takeTimeout(timeout)
    Then match close_roll_result contains deep {jsonrpc: '2.0', id: 33, result: 0}

  # After the usual setup open a valid roll call and then send an invalid request for roll call close, here
  # we provide an invalid update_id field in the message. We expect an error message in return
  Scenario: Close a valid roll call with wrong update_id should return an error message
    Given string rollCallClose = read('classpath:data/rollCall/close/bad_roll_call_close_invalid_update_id.json')
    And  eval frontend.send(rollCallClose)
    * json close_roll_err = frontend_buffer.takeTimeout(timeout)
    Then  match close_roll_err contains deep {jsonrpc: '2.0', id: 33, error: {code: -4, description: '#string'}}

  # After the usual setup, create a roll cal but never open it. Then trying to send a valid
  # roll call close message should result in an error sent by the backend
  Scenario: Close a valid roll call that was never opened should return an error
    Given string rollCallClose = read('classpath:data/rollCall/close/valid_roll_call_close_2.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
    And eval frontend.send(rollCallClose)
    * json close_roll_err = frontend_buffer.takeTimeout(timeout)
    Then  match close_roll_err contains deep {jsonrpc: '2.0', id: 33, error: {code: -4, description: '#string'}}

