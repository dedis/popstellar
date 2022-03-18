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
#    * call read('classpath:be/createLAO/create.feature@name=valid_lao')


#  Scenario: Valid Roll Call 2:
#    Given string laoCreateReq = read('classpath:data/rollCall/valid_roll_call_create.json')
#    And string rollCallReq  = read('classpath:data/rollCall/valid_roll_call_create.json')
#    And   def socket = karate.webSocket(wsURL,handle)
#    * karate.log('Create Request = ' + laoCreateReq)
#    When  eval socket.send(laoCreateReq)
#    *  karate.log('Sent: '+ karate.pretty(laoCreateReq))
#    And   string answer = socket.listen(timeout)
#    * karate.log("The received answer before sending roll call is "+ answer)
#    Then   def socket = karate.webSocket(wsURL,handle)
#    And socket.send(rollCallReq)
#    * karate.log("Roll call Request sent")
#    When eval socket.send(rollCallReq)
#    And   string answer2 = socket.listen(timeout)
#    * karate.log("The received after sending roll call is "+ answer2)


  Scenario: Valid Roll Call
    Given string rollCallReq  = read('classpath:data/rollCall/valid_roll_call_create.json')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')

    When eval frontend.send(laoCreateReq)
    * karate.log('Request for roll call sent')
    * frontend_buffer.takeTimeout(timeout)
    Then eval frontend.send(rollCallReq)
    * json roll = frontend_buffer.takeTimeout(timeout)
    * karate.log(roll)

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



    ############## INVALID ROLL CALL MESSAGE TEST ####################
  #  Scenario: Create Roll call on root channel should fail
#    Given string badChannelReq = read('classpath:data/rollCall/bad_roll_call_create_wrong_channel.json')
#    And def socket = karate.webSocket(wsURL,handle)
#    * karate.log('bad request is created')
#    When eval socket.send(badChannelReq)
#    * karate.log('Request for bad roll call sent')
#    And json err = socket.listen(timeout)
#    * karate.log('Answer received is '+ err)
#    Then match err contains deep {jsonrpc: '2.0', id: 3, error: {code: -6, description: '#string'}}


