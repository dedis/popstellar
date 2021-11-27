@ignore @report=false
Feature: This feature starts a server and stops it after every scenario.

    Scenario: Start the server and configure Karate
        # Handler can be used to filter websocket messages 
        * def handle = function(msg){ karate.signal(msg); return msg.startsWith('{')}

        # Method that waits/pauses for x seconds
        * def wait = 
            """
                function(secs){
                    java.lang.Thread.sleep(secs*1000)
                }
            """

        * def Server = Java.type("be.utils.Server")

        # Method that waits until host:port is available
        * def waitForPort = 
            """
                function() {
                    karate.waitForPort(host,port) 
                }
            """

        # Method that starts the server
        * def startServer = 
            """
                function() {
                    var success = Server.start(serverCmd, serverDIR, logPath);
                    if(success)
                        return;
                    else
                        karate.fail("Unable to start server in Karate");
                }
            """

        # Method that stops the server
        * def stopServer = 
            """     
                function(){
                    Server.stop();
                    wait(2);
                }
            """

        # Start server 
        * call startServer
        * karate.log('Waiting for server start up ....')

        # Wait for server to be ready by polling 
        * call waitForPort
        * karate.log('Executing tests')

        # Shutdown server automatically after the end of a scenario and  feature
        * configure afterScenario = stopServer

        # Configure an after feature function
        * configure afterFeature = karate.get('afterScenario')