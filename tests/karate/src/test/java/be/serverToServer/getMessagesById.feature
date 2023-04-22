@env=go,scala
Feature: Update a LAO

  Background:
    # This is feature will be called to test Update LAO messages
    # This call makes this feature and server.feature share the same scope
    # Meaning they share def variables, configurations ...
    # Especially JS functions defined in server.feature can be directly used here thanks to Karate shared scopes
    # This also sets up a valid lao by sending a create lao message, subscribing to the lao and sending a catchup message
    * call read('classpath:be/utils/server.feature')
    * call read('classpath:be/mockFrontEnd.feature')
    * call read('classpath:be/constants.feature')
    * call read('classpath:be/utils/simpleScenarios.feature@name=valid_lao')
    * string channel = "/root/p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="
