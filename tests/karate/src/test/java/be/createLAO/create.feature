@env=go,scala
Feature: Create a pop LAO

  Background:
        # This is feature will be called  to test an LAO creation
        # This call makes this feature and server.feature share the same scope
        # Meaning they share def variables, configurations ...
        # Especially JS functions defined in server.feature can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/utils/server.feature')
    * def id = 1
    * string channel = "/root"

  Scenario: Create Lao request with empty lao name should fail with an error response
    Given string laoCreateData = read('classpath:data/lao/data/bad_lao_create_empty_name_data.json')
    * string laoCreate = converter.publishМessageFromData(laoCreateData, id, channel)
    And   def socket = karate.webSocket(wsURL,handle)
    When  eval socket.send(laoCreate)
    *  karate.log('Sent: '+ karate.pretty(laoCreate))
    And  json err = socket.listen(timeout)
    * karate.log('Received: '+ err )
    Then match err contains deep {jsonrpc: '2.0', id: id, error: {code: -4, description: '#string'}}

  Scenario: Create Lao with negative time should fail with an error response
      Given string badLaoCreateData = read('classpath:data/lao/data/bad_lao_create_negative_data.json')
      * string badLaoCreate = converter.publishМessageFromData(badLaoCreateData, id, channel)
      And   def socket = karate.webSocket(wsURL,handle)
      When  eval socket.send(badLaoCreate)
      *  karate.log('Sent: '+ karate.pretty(badLaoCreate))
      And  json err = socket.listen(timeout)
      *  karate.log('Received: '+ karate.pretty(err) )
      Then match err contains deep {jsonrpc: '2.0', id: id, error: {code: -4, description: '#string'}}

  Scenario: Create Lao with invalid id hash should fail with an error response
      Given string badLaoCreateData = read('classpath:data/lao/data/bad_lao_create_id_invalid_hash_data.json')
      * string badLaoCreate = converter.publishМessageFromData(badLaoCreateData, id, channel)
      And   def socket = karate.webSocket(wsURL,handle)
      When  eval socket.send(badLaoCreate)
      *  karate.log('Sent: '+ karate.pretty(badLaoCreate))
      And  json err = socket.listen(timeout)
      *  karate.log('Received: '+ karate.pretty(err) )
      Then match err contains deep {jsonrpc: '2.0', id: id, error: {code: -4, description: '#string'}}

  Scenario: Create should succeed with a valid creation request
    Given string laoCreateData = read('classpath:data/lao/data/valid_lao_create_data.json')
    And string laoCreate = converter.publishМessageFromData(laoCreateData, id, channel)
    And   def socket = karate.webSocket(wsURL,handle)
    * karate.log('Create Request = ' + laoCreate)
    When  eval socket.send(laoCreate)
    *  karate.log('Sent: '+ karate.pretty(laoCreate))
    And   json answer = socket.listen(timeout)
    * karate.log('Received answer = ' + answer)
    Then match answer contains deep {jsonrpc: '2.0', id: id, result: 0}

