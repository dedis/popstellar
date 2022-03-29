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
    * def id = 3
    * string channel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="

  # Testing if after setting up a valid lao, subscribing to it and sending a catchup
  # we send a valid roll call create request and expect to receive a valid response
  # from the backend
  Scenario: Valid Roll Call
    Given string rollCallData = read('classpath:data/rollCall/data/rollCallCreate/valid_roll_call_create_data.json')
    And string rollCallCreate = converter.publishМessageFromData(rollCallData, id, channel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    * karate.log('Request for lao creation sent')
    * frontend_buffer.takeTimeout(timeout)
    Then eval frontend.send(rollCallCreate)
    * json roll_call_broadcast = frontend_buffer.takeTimeout(timeout)
    * json roll_call_result = frontend_buffer.takeTimeout(timeout)
    Then match roll_call_result contains deep {jsonrpc: '2.0', id: id, result: 0}

  # Setting up the lao correctly but send an invalid roll call create request, containing
  # an empty roll call name should result in an error message from the backend.
  Scenario: Roll Call Creation with empty name should return an error code
    Given string badRollCallReq  = read('classpath:data/rollCall/bad_roll_call_create_empty_data_but_same_messageId_as_valid_roll_call.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    * string rollCallData = read('classpath:data/rollCall/data/rollCallCreate/valid_roll_call_create_data.json')
    * string rollCallCreate = converter.publishМessageFromData(rollCallData, id, channel)

    When eval frontend.send(rollCallCreate)
    * karate.log('Request for roll call sent')
    * frontend_buffer.takeTimeout(timeout)

    Then eval frontend.send(badRollCallReq)
    * json roll_err = frontend_buffer.takeTimeout(timeout)
    Then match roll_err contains deep {jsonrpc: '2.0', id: id, error: {code: -4, description: '#string'}}

  # Setting up the lao correctly and sending a roll call create message that comes from
  # a non-organizer should result in an error message being sent by the backend.
  Scenario: Roll Call Creation with non-organizer as sender should return an error
    Given string badRollCallReq = read('classpath:data/rollCall/bad_create_roll_call_not_organizer_sender.json')
    And string badRollCallData = read('classpath:data/rollCall/data/rollCallCreate/valid_roll_call_create_data.json')
    * string badRollCallCreate = converter.publishМessageFromData(badRollCallData, id, channel)

    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    When eval frontend.send(badRollCallReq)
    * json roll_err = frontend_buffer.takeTimeout(timeout)
    Then match roll_err contains deep {jsonrpc: '2.0', id: id, error: {code: -4, description: '#string'}}

  # Setting up a lao correctly but sending a valid roll call create message on the
  # root channel should result in backend rejecting the message and sending an error message
  Scenario: Roll Call Creation sent on root channel should return an error
    Given string badRollCallReq = read('classpath:data/rollCall/bad_roll_call_create_wrong_channel.json')
    And call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')

    * string badRollCallData = read('classpath:data/rollCall/data/rollCallCreate/valid_roll_call_create_data.json')
    * string rootChannel = "/root"
    * string badRollCallCreate = converter.publishМessageFromData(badRollCallData, id, rootChannel)

    When eval frontend.send(badRollCallCreate)
    * json roll_err = frontend_buffer.takeTimeout(timeout)
    Then match roll_err contains deep {jsonrpc: '2.0', id: id, error: {code: -6, description: '#string'}}


  # Setting up the lao correctly but send an invalid roll call create request, containing
  # a proposed start time larger than proposed end time should result in an error message
  # from the backend.
  Scenario: Roll Call Creation with proposed start > proposed end should return and error
    Given string badRollCallData = read('classpath:data/rollCall/data/rollCallCreate/bad_roll_call_create_start_time_bigger_than_end_time_data.json')
    And string badRollCallCreate = converter.publishМessageFromData(badRollCallData, id, channel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    When eval frontend.send(badRollCallCreate)
    * json roll_err = frontend_buffer.takeTimeout(timeout)
    Then match roll_err contains deep {jsonrpc: '2.0', id: id, error: {code: -4, description: '#string'}}

  # Setting up the lao correctly but send an invalid roll call create request, containing
  # a negative creation time should result in an error message from the backend.
  Scenario: Roll Call Creation with creation time is negative should return an error
    Given string badRollCallData = read('classpath:data/rollCall/data/rollCallCreate/bad_roll_call_create_creation_time_negative_data.json')
    And string badRollCallCreate = converter.publishМessageFromData(badRollCallData, id, channel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    When eval frontend.send(badRollCallCreate)
    * json roll_err = frontend_buffer.takeTimeout(timeout)
    Then match roll_err contains deep {jsonrpc: '2.0', id: id, error: {code: -4, description: '#string'}}

  # Setting up the lao correctly but send an invalid roll call create request, containing
  # a creation time larger than proposed start time should result in an error message
  # from the backend.
  Scenario: Roll Call Creation with creation time < proposed start should return and error
    Given string badRollCallData = read('classpath:data/rollCall/data/rollCallCreate/bad_roll_call_create_creation_time_less_than_start_time_data.json')
    And string badRollCallCreate = converter.publishМessageFromData(badRollCallData, id, channel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    When eval frontend.send(badRollCallCreate)
    * json roll_err = frontend_buffer.takeTimeout(timeout)
    Then match roll_err contains deep {jsonrpc: '2.0', id: id, error: {code: -4, description: '#string'}}

  # Setting up the lao correctly but send an invalid roll call create request, containing
  # an invalid roll_call id should result in an error message from the backend.
  Scenario: Roll Call Creation with invalid roll_call id should return an error
    Given string badRollCallData = read('classpath:data/rollCall/data/rollCallCreate/bad_roll_call_create_invalid_roll_call_id_data.json')
    And string badRollCallCreate = converter.publishМessageFromData(badRollCallData, id, channel)
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    When eval frontend.send(badRollCallCreate)
    * json roll_err = frontend_buffer.takeTimeout(timeout)
    Then match roll_err contains deep {jsonrpc: '2.0', id: id, error: {code: -4, description: '#string'}}
