@env=go,scala
Feature: Create a Roll Call
  Background:
        ## This is feature will be called  to
        # This call makes this feature and server.feature share the same scope
        # Meaning they share def variables, configurations ...
        # Especially JS functions defined in server.feature can be directly used here thanks to Karate shared scopes
        # * call wait <timeout>
        # * karate.set(varName, newValue)
    * call read('classpath:be/utils/server.feature')
#    * call read('classpath:be/createLAO/create.feature@name=valid_lao')


#  Scenario: Create Roll call on root channel should fail
#    Given string badChannelReq = read('classpath:data/rollCall/bad_roll_call_create_wrong_channel.json')
#    And def socket = karate.webSocket(wsURL,handle)
#    * karate.log('bad request is created')
#    When eval socket.send(badChannelReq)
#    * karate.log('Request for bad roll call sent')
#    And json err = socket.listen(timeout)
#    * karate.log('Answer received is '+ err)
#    Then match err contains deep {jsonrpc: '2.0', id: 3, error: {code: -6, description: '#string'}}
  Scenario: Valid Roll Call
    Given string rollCallReq  = read('classpath:data/lao/bad_lao_create_empty_name.json')
    * karate.log('Create Request = ' + rollCallReq)
    * def newLogg =
            """
              function() {
                var Logg = Java.type('com.intuit.karate.Logger')
                return new Logg()
              }
            """
    * def newBuffer =
            """
              function() {
                var Queue = Java.type("common.net.MessageQueue")
                return new Queue()
              }
            """
    * def multiOptions =
            """
              function(){
                var WebSocketOptions = Java.type('com.intuit.karate.http.WebSocketOptions')
                return new WebSocketOptions(wsURL)
              }
            """
    * def buffer = call newBuffer
    * def logge = call newLogg
    * def multi = call multiOptions
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
    * karate.log('defining multiSocket')
    * def multiSocket = call getMultiMsgSocket
    When eval multiSocket.send(rollCallReq)
    * karate.log('Request for roll call sent')
    * def buffer2 = multiSocket.getBuffer()
    And json lao = multiSocket.listen(timeout)
    And json roll = buffer.takeTimeout(timeout)
    * karate.log("lao is "+ lao+ " and roll "+ roll)


