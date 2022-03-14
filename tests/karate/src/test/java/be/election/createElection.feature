@env=go,scala

Feature: Create an election
  Background:
    * call read('classpath:be/utils/server.feature')
    * def createLAO = read('classpath:be/createLAO/create.feature@name=createLAO')
    * def validElectionCreation =
      """
       JSON.stringify(
                {
                "method": "publish",
                "id": 10,
                "params": {
                    "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=",
                    "message": {
                        "data": "eyJvYmplY3QiOiJlbGVjdGlvbiIsImFjdGlvbiI6InNldHVwIiwiaWQiOiIxNzIxYjdlM2Q2YWM4ZDc0MmViNWJkY2E2Nzc5MWQ3NGI4Yzc3NWM1MDYxYzYxMzE4OWUxMmUyMjhjMDI2ODczIiwibGFvIjoicF9FWWJIeU12NnNvcEk1UWhFWEJmNDBNT19lTm9xN1ZfTHlnQmQ0YzlSQT0iLCJuYW1lIjoiRWxlY3Rpb24iLCJ2ZXJzaW9uIjoxLCJjcmVhdGVkX2F0IjoxNjMzMDM1NzUxLCJzdGFydF90aW1lIjoxNjMzMDM1NzcxLCJlbmRfdGltZSI6MTYzMzAzNTg3MSzigJxxdWVzdGlvbnPigJ06IFt7ImlkIjoiZDlmMWJiMWYyYzRkN2FlNTIxNWExMDIxZjYwODc4MjcxMWNiYzdlZTA2YTI3ZmJiOTc5MWEzYzJlNTI0MmY2NCIsInF1ZXN0aW9uIjoiSG93Iiwidm90aW5nX21ldGhvZCI6IlBsdXJhbGl0eSIsImJhbGxvdF9vcHRpb25zIjpbInllcyIsIm5vIl0sIndyaXRlX2luIjpmYWxzZX1dLCAic2VuZGVyIjoiSjlmQnpKVjcwSms1Yy1pMzI3N1VxNENtZUw0dDUzV0RmVWdoYUswSHBlTT0iLCJzaWduYXR1cmUiOiJPTnlseGdIQTljYnNCX2x3ZGZibjNpeXpSZDRhVHBKaEJNbnZFS2htSkZfbmlFX3BVSGRtanhEWGpFd0Z5dm81V2lIMU5aWFd5WEcyN1NZRXBrYXNDQT09In0=",
                        "sender": "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=",
                        "signature": "ONylxgHA9cbsB_lwdfbn3iyzRd4aTpJhBMnvEKhmJF_niE_pUHdmjxDXjEwFyvo5WiH1NZXWyXG27SYEpkasCA==",
                        "message_id": "2mAAevx61TZJi4groVGqqkeLEQq0e-qM6PGmTWuShyY=",
                        "witness_signatures": []
                    }
                },
                "jsonrpc": "2.0"
            })
      """
  @name=createElection
  Scenario: Create election with valid lao should succeed
    Given call createLAO
    And def createElectionRes =
        """
            JSON.stringify(
                {
                    "jsonrpc":"2.0",
                    "id":1,
                    "result":0
                }
            )

        """
    * karate.log('lao created')
    When eval socket.send(validElectionCreation)
    And string answer = socket.listen(timeout)
    * karate.log('Received answer = ' + answer)
    Then match 1  == 1
