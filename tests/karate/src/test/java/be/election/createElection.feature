@env=go

Feature: Create an election
Background:
  ## This is feature will be called  to
        # This call makes this feature and server.feature share the same scope
        # Meaning they share def variables, configurations ...
        # Especially JS functions defined in server.feature can be directly used here thanks to Karate shared scopes
        # * call wait <timeout>
        # * karate.set(varName, newValue)
  * call read('classpath:be/utils/server.feature')
  * def createLAO = read('classpath:be/createLAO/create.feature@name=createLAO')
  * def lao_id = ""
  * def createElectionReq =
          """
            JSON.stringify(
                {
                "method": "publish",
                "id": 2,
                "params": {
                    "channel": "/root/" + lao_id,
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
  * def createElectionRes =
          """
              JSON.stringify(
                  {
                      "jsonrpc":"2.0",
                      "id":2,
                      "result":0
                  }
              )

          """

Scenario: Create an election should succeed with a valid election creation request and a valid lao
  Given  call createLAO
  And createElectionReq
  And   def socket = karate.webSocket(wsURL,handle)
  * karate.log('Create Request = ' + createElectionReq)
  When  eval socket.send(createElectionReq)
  *  karate.log('Sent: '+ karate.pretty(createElectionReq))
  And   string answer = socket.listen(timeout)
  * karate.log('Received answer = ' + answer)
  * Then match answer == createElectionRes

