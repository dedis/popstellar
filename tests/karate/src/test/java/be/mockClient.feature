@ignore @report=false
Feature: Mock Client

  Scenario: Creates mock clients that can connect to a server as a frontend or server
    * def mockClients = []
    * def createMockClient =
      """
        function(){
          var MockClient = Java.type('be.utils.MockClient')
          var mockClient = new MockClient(wsURL)
          mockClients.push(mockClient)
          return mockClient
        }
      """

    * def stopAllMockClients =
      """
        function() {
          for (var i = 0; i < mockClients.length; i++) {
            mockClients[i].close();
          }
          stopServer();
          deleteDB();
        }
      """

    # Shutdown all mock clients automatically after the end of a scenario and feature
    * configure afterScenario = stopAllMockClients
    * configure afterFeature = stopAllMockClients
