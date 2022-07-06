@ignore @report=false
Feature: Mock Backend

  Scenario: Setup Mock-backend
    * def newBuffer =
            """
              function() {
                var Queue = Java.type("common.net.MessageQueue")
                return new Queue()
              }
            """
    * def getBackend =
            """
              function() {
                var Backend = Java.type("fe.net.MockBackend")
                return new Backend(newBuffer(), port)
              }
            """

    * def backend = call getBackend

    * def getRollCallVerification =
        """
          function(){
            var RollCallVerification = Java.type("fe.utils.verification.RollCallVerification")
            return new RollCallVerification()
          }
        """
    * def rollCallVerification = call getRollCallVerification

    * def getMessageVerification =
      """
        function (){
          var MessageVerification = Java.type("fe.utils.verification.MessageVerification")
          return new MessageVerification()
        }
      """

    * def messageVerification = call getMessageVerification

    * def getVerificationUtils =
      """
        function(){
          var VerificationUtils = Java.type("fe.utils.verification.VerificationUtils")
          return new VerificationUtils();
        }
      """

    * def verificationUtils = call getVerificationUtils

    * def  getConstants =
      """
        function(){
          var Consants = Java.type("common.utils.Constants")
          return new Consants()
        }
      """
    * def wait =
            """
                function(secs) {
                    java.lang.Thread.sleep(secs*1000)
                }
            """

    * def constants = call getConstants

    * karate.log('Backend started at ', backend.getPort())
    * def buffer = backend.getBuffer()

    * def stopBackend = function() { backend.stop() }

    # Shutdown backend automatically after the end of a scenario and  feature
    * configure afterScenario = stopBackend
    * configure afterFeature = stopBackend
