@ignore @report=false
Feature: Mock Client

  Scenario: Creates mock clients that can connect to a server as a frontend or server. Close the connections after each scenario.
    * def mockClients = []
    * def createMockClient =
      """
        function(){
          var MockClient = Java.type('be.utils.MockClient')
          var mockFrontend = new MockClient(serverURL)
          return mockFrontend
        }
      """
    * def stopAllMockClients =
      """
        function(){
          for (var i = 0; i < mockClients.length; i++) {
            mockClients[i].close();
          }
        }
      """
    * configure afterScenario = stopAllMockClients
