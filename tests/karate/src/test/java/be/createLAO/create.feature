@env=go,scala
Feature: Create a pop LAO

  Background:
        # This is feature will be called  to test an LAO creation
        # This call makes this feature and server.feature share the same scope
        # Meaning they share def variables, configurations ...
        # Especially JS functions defined in server.feature can be directly used here thanks to Karate shared scopes
    * call read('classpath:be/utils/server.feature')

  Scenario: Create Lao request with empty lao name should fail with an error response
    Given string  emptyNameReq = read('classpath:data/lao/bad_lao_create_empty_name.json')
    And   def socket = karate.webSocket(wsURL,handle)
    When  eval socket.send(emptyNameReq)
    *  karate.log('Sent: '+ karate.pretty(emptyNameReq))
    And  json err = socket.listen(timeout)
    * karate.log('Received: '+ err )
    Then match err contains deep {jsonrpc: '2.0', id: 1, error: {code: -4, description: '#string'}}

  Scenario: Create Lao with negative time should fail with an error response
      Given string negTimeLao = read('classpath:data/lao/bad_lao_create_negative.json')
      And   def socket = karate.webSocket(wsURL,handle)
      When  eval socket.send(negTimeLao)
      *  karate.log('Sent: '+ karate.pretty(negTimeLao))
      And  json err = socket.listen(timeout)
      *  karate.log('Received: '+ karate.pretty(err) )
      Then match err contains deep {jsonrpc: '2.0', id: 1, error: {code: -4, description: '#string'}}

  Scenario: Create Lao with invalid id hash should fail with an error response
      Given string invalidIdLao = read('classpath:data/lao/bad_lao_create_id_invalid_hash.json')
      And   def socket = karate.webSocket(wsURL,handle)
      When  eval socket.send(invalidIdLao)
      *  karate.log('Sent: '+ karate.pretty(invalidIdLao))
      And  json err = socket.listen(timeout)
      *  karate.log('Received: '+ karate.pretty(err) )
      Then match err contains deep {jsonrpc: '2.0', id: 1, error: {code: -4, description: '#string'}}

  Scenario: Create should succeed with a valid creation request
    Given def createLaoReq =
        """
            JSON.stringify(
                {
                "method": "publish",
                "id": 1,
                "params": {
                    "channel": "/root",
                    "message": {
                        "data": "eyJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUiLCJuYW1lIjoiTEFPIiwiY3JlYXRpb24iOjE2MzMwMzU3MjEsIm9yZ2FuaXplciI6Iko5ZkJ6SlY3MEprNWMtaTMyNzdVcTRDbWVMNHQ1M1dEZlVnaGFLMEhwZU09Iiwid2l0bmVzc2VzIjpbXSwiaWQiOiJwX0VZYkh5TXY2c29wSTVRaEVYQmY0ME1PX2VOb3E3Vl9MeWdCZDRjOVJBPSJ9",
                        "sender": "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=",
                        "signature": "ONylxgHA9cbsB_lwdfbn3iyzRd4aTpJhBMnvEKhmJF_niE_pUHdmjxDXjEwFyvo5WiH1NZXWyXG27SYEpkasCA==",
                        "message_id": "2mAAevx61TZJi4groVGqqkeLEQq0e-qM6PGmTWuShyY=",
                        "witness_signatures": []
                    }
                },
                "jsonrpc": "2.0"
            })
        """
    And def createLaoRes =
        """
            JSON.stringify(
                {
                    "jsonrpc":"2.0",
                    "id":1,
                    "result":0
                }
            )

        """
    And   def socket = karate.webSocket(wsURL,handle)
    * karate.log('Create Request = ' + createLaoReq)
    When  eval socket.send(createLaoReq)
    *  karate.log('Sent: '+ karate.pretty(createLaoReq))
    And   string answer = socket.listen(timeout)
    * karate.log('Received answer = ' + answer)
    Then match answer == createLaoRes

