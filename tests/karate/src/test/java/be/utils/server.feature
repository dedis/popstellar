@ignore @report=false
Feature: This feature starts a server and stops it after every scenario.

  Background:
    * def MAX_CONNECTION_ATTEMPTS = 5

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
                    var i = 0;
                    while(i < MAX_CONNECTION_ATTEMPTS && !karate.waitForPort(host, port)){
                      //Wait 5 secs before polling again
                      wait(5)
                      i++
                    }
                    if(i >= MAX_CONNECTION_ATTEMPTS){
                      server.stop()
                      karate.fail(`Failed waiting for ${wsURL}`)
                    }
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
    * def deleteDB =
            """
              function() {
                  server.deleteDatabaseDir();
              }
            """
    # Start server
    * call startServer
    * karate.log('Waiting for server start up ....')

        # Wait for server to be ready by polling
    * call waitForPort
    * karate.log('Executing tests')

    * def convertData =
        """
          function(){
            var JsonConverter = Java.type('be.utils.JsonConverter')
            return new JsonConverter()
          }
        """
    * def converter = call convertData

        # Shutdown server automatically after the end of a scenario and  feature
    * configure afterScenario =
          """
            function() {
              stopServer();
              wait(2);
              deleteDB();
            }
          """
    # Configure an after feature function
    * configure afterFeature = karate.get('afterScenario')

