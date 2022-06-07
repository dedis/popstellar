@ignore @report=false
  Feature: Create valid simple Scenarios

    Background:
    # This file contains a set of simple scenarios that can be used when
    # testing the validity of other features. By calling one scenario from
    # this file simply use the allocated name for the particular feature.
      * def laoCreateId = 1
      * def rollCallCreateId = 3
      * def openRollCallId = 32
      * def closeRollCallId = 33
      * def electionSetupId = 4
      * def castVoteId = 41
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
      * string rollCallCreate = converter.publishМessageFromData(rollCallData, rollCallCreateId, laoChannel)
      * frontend_buffer.takeTimeout(timeout)
      * eval frontend.send(rollCallCreate)
      * def roll_call_broadcast = frontend_buffer.takeTimeout(timeout)
      * def roll_call = frontend_buffer.takeTimeout(timeout)
      * karate.log("roll call create : " + roll_call_broadcast)

    @name=open_roll_call
    Scenario: Opens a valid Roll Call
      * string rollCallCreateReq  = read('classpath:data/rollCall/valid_roll_call_create_3.json')
      * string rollCallOpenReq  = read('classpath:data/rollCall/open/valid_roll_call_open_3.json')

      * string rollCallCreateData = read('classpath:data/rollCall/data/rollCallCreate/valid_roll_call_create_3_data.json')
      * string rollCallCreate = converter.publishМessageFromData(rollCallCreateData, rollCallCreateId, laoChannel)
      * string rollCallOpenData = read('classpath:data/rollCall/data/rollCallOpen/valid_roll_call_open_3_data.json')
      * string rollCallOpen = converter.publishМessageFromData(rollCallOpenData, openRollCallId, laoChannel)
      * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')

      * eval frontend.send(rollCallCreate)
      * json create_roll_broadcast = frontend_buffer.takeTimeout(timeout)
      * json create_roll_result = frontend_buffer.takeTimeout(timeout)
      * eval frontend.send(rollCallOpen)
      * json open_roll_broadcast = frontend_buffer.takeTimeout(timeout)
      * json open_roll_result = frontend_buffer.takeTimeout(timeout)
      * karate.log("Received in simple scenarios open roll call :")
      * karate.log(open_roll_result)

    @name=close_roll_call
    Scenario: Closes a valid Roll Call
      * string rollCallCloseData = read('classpath:data/rollCall/data/rollCallClose/valid_roll_call_close_data.json')
      * string rollCallClose = converter.publishМessageFromData(rollCallCloseData, closeRollCallId, laoChannel)
      * call read('classpath:be/utils/simpleScenarios.feature@name=open_roll_call')
      * eval frontend.send(rollCallClose)
      * def close_roll_broadcast = frontend_buffer.takeTimeout(timeout)
      * def close_roll_result = frontend_buffer.takeTimeout(timeout)

    @name=election_setup
    Scenario: Sets up a valid election
      * string electionSetupData = read('classpath:data/election/data/electionSetup/valid_election_setup_data.json')
      * string electionSetup = converter.publishМessageFromData(electionSetupData, electionSetupId, laoChannel)
      * call read('classpath:be/utils/simpleScenarios.feature@name=close_roll_call')
      * eval frontend.send(electionSetup)
      * def election_create_broadcast = frontend_buffer.takeTimeout(timeout)
      * def election_create = frontend_buffer.takeTimeout(timeout)
      * def subscribe =
            """
          JSON.stringify(
              {
              "method": "subscribe",
              "id": 200,
              "params": {
                  "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/rdv-0minecREM9XidNxnQotO7nxtVVnx-Zkmfm7hm2w=",
              },
              "jsonrpc": "2.0"
          })
        """
      * frontend.send(subscribe)
      * def subs = frontend_buffer.takeTimeout(timeout)
      * def catchup =
      """
          JSON.stringify(
              {
              "method": "catchup",
              "id": 500,
              "params": {
                  "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/rdv-0minecREM9XidNxnQotO7nxtVVnx-Zkmfm7hm2w=",
              },
              "jsonrpc": "2.0"
          })
      """
      * frontend.send(catchup)
      * def catchup_response = frontend_buffer.takeTimeout(timeout)

    @name=election_open
    Scenario: Opens an election
      * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
      * def validElectionOpen =
            """
              {
                  "object": "election",
                  "action": "open",
                  "lao": "p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=",
                  "election": "rdv-0minecREM9XidNxnQotO7nxtVVnx-Zkmfm7hm2w=",
                  "opened_at": 1633098944
              }
            """
      * frontend.publish(JSON.stringify(validElectionOpen), electionChannel)
      * json answer = frontend.getBackendResponse(JSON.stringify(validElectionOpen))
    @name=cast_vote
    Scenario: Casts a valid vote
      * string castVoteData = read('classpath:data/election/data/castVote/valid_cast_vote_2_data.json')
      * string castVote = converter.publishМessageFromData(castVoteData, castVoteId, electionChannel)
      * call read('classpath:be/utils/simpleScenarios.feature@name=election_setup')
      * eval frontend.send(castVote)
      * def cast_vote_broadcast = frontend_buffer.takeTimeout(timeout)
      * def cast_vote = frontend_buffer.takeTimeout(timeout)

    @name=setup_coin_channel
    Scenario: Sets up the coin channel and subscribes to it
      * call read('classpath:be/utils/simpleScenarios.feature@name=close_roll_call')
      * def subscribe =
            """
          JSON.stringify(
              {
              "method": "subscribe",
              "id": 233,
              "params": {
                  "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/coin",
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
              "id": 533,
              "params": {
                  "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/coin",
              },
              "jsonrpc": "2.0"
          })
      """
      * frontend.send(catchup)
      * def catchup_response = frontend_buffer.takeTimeout(timeout)

    @name=valid_coin_issuance
    Scenario: Issues a certain amount of coins to an attendee
      * call read('classpath:be/utils/simpleScenarios.feature@name=setup_coin_channel')
      * def validTransaction =
      """
        {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": "_6BPyKnSBFUdMdUxZivzC2BLzM7j5d667BdQ4perTvc=",
            "transaction": {
              "version": 1,
              "inputs": [{
                "tx_out_hash": "47DEQpj8HBSa--TImW-5JCeuQeRkm5NMpJWZG3hSuFU=",
                "tx_out_index": 0,
                "script": {
                  "type": "P2PKH",
                  "pubkey": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                  "sig": "CAFEBABE"
                }
              }],
              "outputs": [{
                "value": 32,
                "script": {
                  "type": "P2PKH",
                  "pubkey_hash": "2jmj7l5rSw0yVb-vlWAYkK-YBwk="
                }
              }],
              "lock_time": 0
            }
        }
      """
      * frontend.publish(JSON.stringify(validTransaction), cashChannel)
      * json answer = frontend.getBackendResponse(JSON.stringify(validTransaction))
