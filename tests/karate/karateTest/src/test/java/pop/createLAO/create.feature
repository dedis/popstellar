Feature: Create a pop LAO

    Background:
        ## This is feature will be called  to
        # This call makes this feature and server.feature share the same scope
        # Meaning they share def variables, configurations ...
        # Espicially JS functions defined in server.feature can be directly used here thanks to Karate shared scopes
        # * call wait <timeout>
        # * karate.set(varName, newValue)
        * call read('classpath:pop/utils/server.feature')
        * string createLaoReq =  read('classpath:pop/data/laoCreate/publish.json')
        * string createLaoRes =  read('classpath:pop/data/laoCreate/answer.json')
        ## NOTE: How to get the laoID from the publih.json it's encoded in the data64 
        * string laoID = 'p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA='

    Scenario:
         * print `Test for auto ${karate.get('env')} launch`
         * print 'End scenario'

    Scenario: Create a duplicated LAO should fail with an error responce
        Given createLaoReq, createLaoRes
        * def socket = karate.webSocket(wsUrl,handle)
        * karate.log("Sending first request..")
        When eval socket.send(createLaoReq)
        ## Using listen instead of socket.listen gives more control
        ## for example sending multiple messages,
        ## result of a listen will be returned in the magic var listenResult
        * listen timeout
        * karate.log('First = ' + listenResult)
        And eval socket.send(createLaoReq)
        * karate.log("Sending Second duplicate request..")
        * listen timeout
        ## Reads listen websocket result
        * json err = listenResult 
        * karate.log('Second = ' + listenResult)
 
        ## match X contains deep Y for nested jsons
        # partial comparison of res /!\ needs lao ID. 
        Then match err contains deep {id: 1, error: {code: -3}}  


    Scenario: Create should succeed with a valid creation request
        Given createLaoReq, createLaoRes
        * def socket = karate.webSocket(wsUrl,handle)
        When eval socket.send(createLaoReq)
        And string res = socket.listen(timeout)
        * karate.log('res = ' + res)
        Then match res == createLaoRes



