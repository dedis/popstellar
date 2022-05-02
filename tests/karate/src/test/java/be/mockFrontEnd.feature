@ignore @report=false
Feature: Mock FrontEnd

  Scenario: creates a valid FrontEnd
    * def getMultiMsgSocket =
              """
                function(){
                  var MultiMsg = Java.type('common.net.MultiMsgWebSocketClient')
                  var Logg = Java.type('com.intuit.karate.Logger')
                  var logg =  new Logg()
                  var Queue = Java.type("common.net.MessageQueue")
                  var q =  new Queue()
                  var WebSocketOptions = Java.type('com.intuit.karate.http.WebSocketOptions')
                  var wso =  new WebSocketOptions(wsURL)
                  return new MultiMsg(wso,logg,q)
                }
              """
    * def frontend = call getMultiMsgSocket
    * def frontend_buffer = frontend.getBuffer()
    * def stopFrontend =
              """
                 function() {
                  frontend.close()
                  stopServer();
                  deleteDB();
                }
              """

    # Shutdown frontend automatically after the end of a scenario and  feature
    * configure afterScenario = stopFrontend
    * configure afterFeature = stopFrontend
