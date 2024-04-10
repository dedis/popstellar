@ignore @report=false
Feature: Mock Client

  Scenario: Creates mock clients that can connect to a server as a frontend or server
    * def create_client =
      """
        function(){
          var MockClient = Java.type('be.utils.MockClient')
          var mockFrontend = new MockClient(serverURL)
          return mockFrontend
        }
      """

    * def create_lao =
      """
        function(client, lao){
          const request = {
            "object": "lao",
            "action": "create",
            "id": lao.id,
            "name": lao.name,
            "creation": lao.creation,
            "organizer": lao.organizerPk,
            "witnesses": lao.witnesses
          };
          organizer.publish(request, "/root");
          organizer.getBackendResponse(request)
          organizer.send({
            "method": "subscribe",
            "id": 2,
            "params": {
                "channel": lao.channel,
            },
            "jsonrpc": "2.0"
          })

          organizer.takeTimeout(1000)
          organizer.send( {
            "method": "catchup",
            "id": 5,
            "params": {
                "channel": lao.channel,
              },
            "jsonrpc": "2.0"
          })
          organizer.takeTimeout(1000)
        }
      """
