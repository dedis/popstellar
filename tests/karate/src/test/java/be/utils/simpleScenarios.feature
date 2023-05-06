@ignore @report=false
  Feature: Create valid simple Scenarios

    Background:
    # This file contains a set of simple scenarios that can be used when
    # testing the validity of other features. By calling one scenario from
    # this file simply use the allocated name for the particular feature.
      * call read('classpath:be/mockFrontEnd.feature')
      * def laoCreateId = 1
      * def rollCallCreateId = 3
      * def openRollCallId = 32
      * def closeRollCallId = 33
      * def electionSetupId = 4
      * def castVoteId = 41
      * def frontend = call createFrontend

    @name=valid_lao
    Scenario: Creates valid lao

      * string laoCreateReq  = read('classpath:data/lao/valid_lao_create.json')
      * eval frontend.send(laoCreateReq)
      * frontend.takeTimeout(timeout)

      * def subscribe =
        """
          {
            "method": "subscribe",
            "id": 2,
            "params": {
                "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=",
            },
            "jsonrpc": "2.0"
          }
        """
      * frontend.send(subscribe)
      * def subs = frontend.takeTimeout(timeout)
      * karate.log("subscribe message received : " + subs)
      * def catchup =
        """
          {
            "method": "catchup",
            "id": 5,
            "params": {
                "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=",
              },
            "jsonrpc": "2.0"
          }
        """
      * frontend.send(catchup)
      * def catchup_response = frontend.takeTimeout(timeout)
      * karate.log("catchup message received : " + catchup_response)

    @name=valid_roll_call
    Scenario: Creates a valid Roll Call
      * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
      * def validCreateRollCall =
        """
          {
            "object": "roll_call",
            "action": "create",
            "id": "VSsRrcHoOTQJ-nU_VT_FakiMkezZA86z2UHNZKCxbN8=",
            "name": "Roll Call 2",
            "creation": 1633098863,
            "proposed_start": 1633099126,
            "proposed_end": 1633099141,
            "location": "EPFL cafeteria",
            "description": "Food is welcome anytime!"
          }
        """
      * frontend.publish(validCreateRollCall, laoChannel)
      * json answer = frontend.getBackendResponse(validCreateRollCall)

    @name=open_roll_call
    Scenario: Opens a valid Roll Call
      * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')

      * def validCreateRollCall =
        """
          {
            "object": "roll_call",
            "action": "create",
            "id": "pKbXASZI6NYbzhFKoGd4HUpIBLF-CMSGAbfdYkd09PM=",
            "name": "Roll Call 3",
            "creation": 1633098864,
            "proposed_start": 1633099127,
            "proposed_end": 1633099148,
            "location": "Lausanne",
            "description": "Nice city!"
          }
        """
      * def validOpenRollCall =
        """
          {
            "object": "roll_call",
            "action": "open",
            "update_id": "N9DNfliEA9lrcDNAnw5PXjOS84kbq2fLFz8GzIxzCwU=",
            "opens": "pKbXASZI6NYbzhFKoGd4HUpIBLF-CMSGAbfdYkd09PM=",
            "opened_at": 1633099127
          }
        """
      * frontend.publish(validCreateRollCall, laoChannel)
      * json answer = frontend.getBackendResponse(validCreateRollCall)
      * frontend.publish(validOpenRollCall, laoChannel)
      * json answer2 = frontend.getBackendResponse(validOpenRollCall)

    @name=close_roll_call
    Scenario: Closes a valid Roll Call
      * call read('classpath:be/utils/simpleScenarios.feature@name=open_roll_call')
      * def validRollCallClose =
        """
          {
            "object": "roll_call",
            "action": "close",
            "update_id": "IGLB3pipK0p0G5E_wFxedEk4IpyM3L7XIQoFummhj0Y=",
            "closes": "N9DNfliEA9lrcDNAnw5PXjOS84kbq2fLFz8GzIxzCwU=",
            "closed_at": 1633099135,
            "attendees": ["M5ZychEi5rwm22FjwjNuljL1qMJWD2sE7oX9fcHNMDU=", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM="]
          }
        """
      * frontend.publish(validRollCallClose, laoChannel)
      * json answer = frontend.getBackendResponse(validRollCallClose)

    @name=election_setup
    Scenario: Sets up a valid election
      Given call read('classpath:be/utils/simpleScenarios.feature@name=valid_roll_call')
      And def validElectionSetup =
      """
        {
          "object": "election",
          "action": "setup",
          "id": '#(getValidElectionSetupId)',
          "lao": '#(getLaoValid)',
          "name": "Election",
          "version": "OPEN_BALLOT",
          "created_at": 1633098941,
          "start_time": 1633098941,
          "end_time": 1633099812,
          "questions": [
            {
              "id": '#(getIsThisProjectFunQuestionId)',
              "question": "Is this project fun?",
              "voting_method": "Plurality",
              "ballot_options": ["Yes", "No"],
              "write_in": false
            }
          ]
        }
      """
      When frontend.publish(validElectionSetup, laoChannel)
      And json answer = frontend.getBackendResponse(validElectionSetup)
      * def subscribe =
        """
          {
            "method": "subscribe",
            "id": 200,
            "params": {
                "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/rdv-0minecREM9XidNxnQotO7nxtVVnx-Zkmfm7hm2w=",
            },
            "jsonrpc": "2.0"
          }
        """
      * frontend.send(subscribe)
      * def subs = frontend.takeTimeout(timeout)
      * def catchup =
        """
          {
            "method": "catchup",
            "id": 500,
            "params": {
                "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/rdv-0minecREM9XidNxnQotO7nxtVVnx-Zkmfm7hm2w=",
            },
            "jsonrpc": "2.0"
           }
        """
      * frontend.send(catchup)
      * def catchup_response = frontend.takeTimeout(timeout)

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
      * frontend.publish(validElectionOpen, electionChannel)
      * json answer = frontend.getBackendResponse(validElectionOpen)
    @name=cast_vote
    Scenario: Casts a valid vote
      * call read('classpath:be/utils/simpleScenarios.feature@name=election_open')
      * def validCastVote =
        """
          {
            "object": "election",
            "action": "cast_vote",
            "lao": "p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=",
            "election": "rdv-0minecREM9XidNxnQotO7nxtVVnx-Zkmfm7hm2w=",
            "created_at": 1633098941,
            "votes": [
              {
                "id": "d60B94lVWm84lBHc9RE5H67oH-Ad3O1WFflK3NSY3Yk=",
                "question": "3iPxJkdUiCgBd0c699KA9tU5U0zNIFau6spXs5Kw6Pg=",
                "vote": [0]
              }
            ]
          }
        """
      * frontend.publish(validCastVote, electionChannel)
      * json answer = frontend.getBackendResponse(validCastVote)

    @name=setup_coin_channel
    Scenario: Sets up the coin channel and subscribes to it
      * call read('classpath:be/utils/simpleScenarios.feature@name=close_roll_call')
      * def subscribe =
        """
          {
            "method": "subscribe",
            "id": 233,
            "params": {
                "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/coin",
            },
            "jsonrpc": "2.0"
          }
        """
      * frontend.send(subscribe)
      * def subs = frontend.takeTimeout(timeout)
      * karate.log("subscribe message received : " + subs)
      * def catchup =
        """
          {
            "method": "catchup",
            "id": 533,
            "params": {
                "channel": "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA=/coin",
            },
            "jsonrpc": "2.0"
          }
        """
      * frontend.send(catchup)
      * def catchup_response = frontend.takeTimeout(timeout)

    @name=valid_coin_issuance
    Scenario: Issues a certain amount of coins to an attendee
      * call read('classpath:be/utils/simpleScenarios.feature@name=setup_coin_channel')
      * def validTransaction =
        """
          {
            "object": "coin",
            "action": "post_transaction",
            "transaction_id": "yVMgw2E9IMX7JtNfizTqTOR1scMVSHfEe8WBbiAgsA8=",
            "transaction": {
              "version": 1,
              "inputs": [{
                "tx_out_hash": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=",
                "tx_out_index": 0,
                "script": {
                  "type": "P2PKH",
                  "pubkey": "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=",
                  "sig": "wVoIYoQFoepkosPxXK8CmnvhRmq0IUczGQR5JOJjX8R7vqrgMOdI311bgzrOIwtACMfGFTJcnryiHiOuB5Z3Dg=="
                }
              }],
              "outputs": [{
                "value": 32,
                "script": {
                  "type": "P2PKH",
                  "pubkey_hash": "-_qR4IHwsiq50raa8jURNArds54="
                }
              }],
              "lock_time": 0
            }
          }
        """
      * frontend.publish(validTransaction, cashChannel)
      * json answer = frontend.getBackendResponse(validTransaction)
