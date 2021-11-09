Feature: Create a pop LAO

    Background: 
        * def handle = function(msg){ karate.signal(msg); return msg.startsWith('{')}
        * def ssProc = karate.fork(["bash", "-c" , "\"" + serverScript + "\""]) 
        * configure afterScenario = function(){ssProc.close(true) }
    
    Scenario: Process a valid creation request
        Given  string jsonRequest = {"jsonrpc":"2.0","method":"publish","params":{"message":{"message_id":"f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE=","sender":"to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=","signature":"2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==","data":"eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ==","witness_signatures":[]},"channel":"/root"},"id":1}
        And string expectedRes = {"jsonrpc":"2.0","id":1,"result":0}
            * def socket = karate.webSocket(wsUrl)  
        When eval socket.send(jsonRequest)
        And string res = socket.listen(timeout)
            * karate.log('res = ' + res)
        Then assert res == expectedRes
        