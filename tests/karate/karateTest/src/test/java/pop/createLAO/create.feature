Feature: Create a pop LAO

    Background: 
        # Handler can be used to filter websocket messages 
        * def handle = function(msg){ karate.signal(msg); return msg.startsWith('{')}
        # Wait/pause for x seconds
        * def wait = 
            """
                function(secs){ java.lang.Thread.sleep(secs*1000) }
            """
        * def ServerStart = Java.type("pop.utils.ServerLaunch")
        # Wait until host:port is available.
        * def waitForPort = 
            """
                function(){
                    var Command = Java.type("com.intuit.karate.shell.Command")
                    var cmd = new Command("exit 0");
                    cmd.waitForPort(host,port) 
                }
            """
        ## Method to start server
        * def startServer = 
            """
                function() {
                    var success = ServerStart.startServer(serverCmd, serverDIR, logPath);
                    if(success)
                        return;
                    else{
                        karate.fail("Unable to start server in Karate");
                    }
                }
            """      
        * call startServer
        * karate.log('Waiting for server start up ....')
        # Wait for server to be ready by polling 
        * call waitForPort
        * karate.log('Executing tests') 
        # Shuttdown server
        * configure afterScenario = 
            """     
                function(){
                     ServerStart.stopServer();
                     wait(2);
                }
            """
        * configure afterFeature = karate.get('afterScenario')
    Scenario:
        * print 'test for auto scala launch'
        * print 'End scenario'
    
    Scenario: Process a valid creation request
        Given  string jsonRequest = read('classpath:pop/data/laoCreate/publish.json')
        And string expectedRes = read('classpath:pop/data/laoCreate/answer.json')
        * def socket = karate.webSocket(wsUrl,handle)  
        When eval socket.send(jsonRequest)
        And string res = socket.listen(timeout)
        * karate.log('res = ' + res)
        Then match res == expectedRes    

    
