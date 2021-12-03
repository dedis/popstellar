@ignore @report=false
Feature: This feature starts a server and stops it after every scenario.

  Scenario: Start the server and configure Karate
        # Handler can be used to filter websocket messages
    * def handle = function(msg){ karate.signal(msg); return msg.startsWith('{')}

        # Method that waits/pauses for x seconds
    * def wait =
            """
                function(secs) {
                    java.lang.Thread.sleep(secs*1000)
                }
            """

        # Get the server depending on the environment
    * def GoServer = Java.type("be.utils.GoServer")
    * def ScalaServer = Java.type("be.utils.ScalaServer")
    * def getServer =
            """
                function() {
                    if(env == 'go')
                        return new GoServer();
                    else if(env == 'scala')
                        return new ScalaServer();
                    else
                        karate.fail("Unknown environment for server");
                }
            """
    * def server = call getServer

        # Method that waits until host:port is available
    * def waitForPort =
            """
                function() {
                    karate.waitForPort(host, port)
                    // Scala takes more time to start the server
                    if(env == 'scala')
                        wait(10)
                }
            """

        # Method that starts the server
    * def startServer =
            """
                function() {
                    var success = server.start();
                    if(success)
                        return;
                    else
                        karate.fail("Unable to start server in Karate");
                }
            """

        # Method that stops the server
    * def stopServer =
            """
                function() {
                    server.stop();
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
