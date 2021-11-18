Feature: Create a pop LAO

    Background: 
        # Handler can be used to filter websocket messages 
        * def handle = function(msg){ karate.signal(msg); return msg.startsWith('{')}
        # Forked process to launch the server
        # Passing through the shell to launch te server is required 
        * def ssProc = karate.fork({line: serverCmd, workingDir: serverDIR, useShell: true})
        * karate.log('Waiting for server start up ....')
        # Wait for server to be ready by polling 
        * eval ssProc.waitForPort(host,port) 
        * karate.log('Executing tests') 
        # Shuttdown server
        * configure afterScenario = 
            """     
                function(){
                    //FIXME: This seems to be killing the shell and not the server
                    karate.signal(ssProc.sysOut);
                    karate.log("Server process ended"); 
                }
            """

    Scenario:
        * print 'test for auto scala launch'
        * print 'End scenario'
    
    Scenario: Process a valid creation request
        Given  string jsonRequest = {"jsonrpc":"2.0","method":"publish","params":{"message":{"message_id":"f1jTxH8TU2UGUBnikGU3wRTHjhOmIEQVmxZBK55QpsE=","sender":"to_klZLtiHV446Fv98OLNdNmi-EP5OaTtbBkotTYLic=","signature":"2VDJCWg11eNPUvZOnvq5YhqqIKLBcik45n-6o87aUKefmiywagivzD4o_YmjWHzYcb9qg-OgDBZbBNWSUgJICA==","data":"eyJjcmVhdGlvbiI6MTYzMTg4NzQ5NiwiaWQiOiJ4aWdzV0ZlUG1veGxkd2txMUt1b0wzT1ZhODl4amdYalRPZEJnSldjR1drPSIsIm5hbWUiOiJoZ2dnZ2dnIiwib3JnYW5pemVyIjoidG9fa2xaTHRpSFY0NDZGdjk4T0xOZE5taS1FUDVPYVR0YkJrb3RUWUxpYz0iLCJ3aXRuZXNzZXMiOltdLCJvYmplY3QiOiJsYW8iLCJhY3Rpb24iOiJjcmVhdGUifQ==","witness_signatures":[]},"channel":"/root"},"id":1}
        And string expectedRes = {"jsonrpc":"2.0","id":1,"result":0}
        * def socket = karate.webSocket(wsUrl,handle)  
        When eval socket.send(jsonRequest)
        And string res = socket.listen(timeout)
        * karate.log('res = ' + res)
        Then match res == expectedRes    

    
