@ignore @report=false
  Feature: Create valid simple Scenarios
    # This file contains a set of simple scenarios that can be used when
    # testing the validity of other features. By calling one scenario from
    # this file simply use the allocated name for the particular feature.
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
      * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
      * string rollCallData = read('classpath:data/rollCall/data/rollCallCreate/valid_roll_call_create_2_data.json')
      * string rollCallCreate = converter.publishМessageFromData(rollCallData, id, channel)
      * frontend_buffer.takeTimeout(timeout)
      * eval frontend.send(rollCallCreate)
      * def roll_call_broadcast = frontend_buffer.takeTimeout(timeout)
      * def roll_call = frontend_buffer.takeTimeout(timeout)
      * karate.log("roll call create : "+roll_call_broadcast)

    @name=open_roll_call
    Scenario: Opens a valid Roll Call
      * string rollCallCreateReq  = read('classpath:data/rollCall/valid_roll_call_create_3.json')
      * string rollCallOpenReq  = read('classpath:data/rollCall/open/valid_roll_call_open_3.json')

      * string rollCallCreateData = read('classpath:data/rollCall/data/rollCallCreate/valid_roll_call_create_3_data.json')
      * string rollCallCreate = converter.publishМessageFromData(rollCallCreateData, id, channel)
      * string rollCallOpenData = read('classpath:data/rollCall/data/rollCallOpen/valid_roll_call_open_3_data.json')
      * string rollCallOpen = converter.publishМessageFromData(rollCallOpenData, id, channel)
      * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')

      * eval frontend.send(rollCallCreate)
      * json create_roll_broadcast = frontend_buffer.takeTimeout(timeout)
      * json create_roll_result = frontend_buffer.takeTimeout(timeout)
      * eval frontend.send(rollCallOpen)
      * json open_roll_broadcast = frontend_buffer.takeTimeout(timeout)
      * json open_roll_result = frontend_buffer.takeTimeout(timeout)
      * karate.log("Received in simple scenarios open roll call :")
      * karate.log(open_roll_result)
