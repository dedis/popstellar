@ignore @report=false
Feature: This feature should be called to start a server 
    Scenario: Start the server and configure Karate
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
                    karate.waitForPort(host,port) 
                }
            """
        ## Method to start server
        * def startServer = 
            """
                function() {
                    var success = ServerStart.startServer(serverCmd, serverDIR, logPath);
                    if(success)
                        return;
                    else
                        karate.fail("Unable to start server in Karate");
                }
            """     
        * def stopServer = 
            """     
                function(){
                    ServerStart.stopServer();
                    wait(2);
                }
            """ 
        # Start server 
        * call startServer
        * karate.log('Waiting for server start up ....')
        # Wait for server to be ready by polling 
        * call waitForPort
        * karate.log('Executing tests') 
        # Shuttdown server automatically after the end of a scenario and  feature
        * configure afterScenario = stopServer
        # Configure an after feature function 
        * configure afterFeature = karate.get('afterScenario')