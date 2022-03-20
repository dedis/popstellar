@ignore @report=false
  Feature: create lao
    @name=valid_lao
    Scenario: Creates valid lao

      * string laoCreateReq  = read('classpath:data/lao/valid_lao_create.json')
      * eval frontend.send(laoCreateReq)
      * frontend_buffer.takeTimeout(timeout)

      * def subscribe =
            """
          JSON.stringify(
              {
              "method": "subscribe",
              "id": 2,
              "params": {
                  "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=",
              },
              "jsonrpc": "2.0"
          })
        """
      * frontend.send(subscribe)
      * def subs = frontend_buffer.takeTimeout(timeout)
      * karate.log("subscribe message received : " + subs)
      * def catchup =
      """
          JSON.stringify(
              {
              "method": "catchup",
              "id": 5,
              "params": {
                  "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=",
              },
              "jsonrpc": "2.0"
          })
      """
      * frontend.send(catchup)
      * def catchup_response = frontend_buffer.takeTimeout(timeout)
      * karate.log("catchup message received : " + catchup_response)

    @name=valid_roll_call
    Scenario: Creates a valid Roll Call
      * string rollCallReq  = read('classpath:data/rollCall/valid_roll_call_create.json')
      * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')

      * frontend_buffer.takeTimeout(timeout)
      * eval frontend.send(rollCallReq)
      * def roll_call_broadcast = frontend_buffer.takeTimeout(timeout)
      * def roll_call = frontend_buffer.takeTimeout(timeout)
      * karate.log("roll call create broadcast message received : "+roll_call_broadcast)
      * karate.log("roll call create message received : "+roll_call)

    @name=open_roll_call
    Scenario: Opens a valid Roll Call
      * string rollCallOpenReq  = read('classpath:data/rollCall/open/valid_roll_call_open.json')
      * def roll_call = call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
      * eval frontend.send(rollCallOpenReq)
      * json create_roll_broadcast = frontend_buffer.takeTimeout(timeout)
      * json open_roll = frontend_buffer.takeTimeout(timeout)
