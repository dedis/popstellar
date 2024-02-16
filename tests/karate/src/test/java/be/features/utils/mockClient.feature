@ignore @report=false
Feature: Mock Client

  Scenario: Creates mock clients that can connect to a server as a frontend or server
    * def mockClients = []
    * def createMockFrontend =
      """
        function(){
          var MockClient = Java.type('be.utils.MockClient')
          var mockFrontend = new MockClient(frontendWsURL)
          mockClients.push(mockFrontend)
          return mockFrontend
        }
      """
    * def createMockBackend =
      """
        function(){
          var MockClient = Java.type('be.utils.MockClient')
          var mockBackend = new MockClient(backendWsURL)
          mockClients.push(mockBackend)
          return mockBackend
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
