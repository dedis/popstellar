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

Scenario: Create an election should succeed with a valid election creation request and a valid lao
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
  And   def socket = karate.webSocket(wsURL,handle)
  * karate.log('Create Request = ' + createLaoReq)
  When  eval socket.send(createLaoReq)
  *  karate.log('Sent: '+ karate.pretty(createLaoReq))
  And   string answer = socket.listen(timeout)
  * karate.log('Received answer = ' + answer)
  And def createElectionReq =
          """
            JSON.stringify(
                {
                "method": "publish",
                "id": 1,
                "params": {
                    "channel": "/root/",
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
