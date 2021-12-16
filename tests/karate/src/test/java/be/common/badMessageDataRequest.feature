@ignore
# This feature is used to modularize tests only
# The args should be passed as a json {bad_request: <string>, err_code: <int>}
# upon calling this feature file.
# Assumes server is already running.
# For more info on calling *.feature files: https://github.com/karatelabs/karate#calling-other-feature-files
# Careful with shared Scopes: https://github.com/karatelabs/karate#shared-scope
Feature: Test behavior of server given invalid message data
        for expected error-valued answers

  Background:
    * assert isServerRunning
    * call read('classpath:be/createLAO/create.feature@name=createLAO')
    * def socket = karate.webSocket(wsURL,handle)

  Scenario: Test invalid request messages should fail
    Given eval bad_request
    * karate.log("Invalid request: "+ karate.pretty(bad_request))
    When eval socket.send(bad_request)
    * json answer = socket.listen(timeout)
    * karate.log("Answer: "+ karate.pretty(answer))
    Then match answer == deep {jsonrpc: '2.0', id: 1, error: {code: #(err_code), description: '#string'}
