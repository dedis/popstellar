@ignore @report=false
Feature: Constants
  Scenario: Creates constants that will be used by other features
    # TODO: make the function depend on all the attributes the lao id depends on
    * def createLaoIdEmptyName =
      """
        function(){
          return "p8TW08AWlBScs9FGXK3KbLQX7Fbgz8_gLwX-B5VEWS0="
        }
      """
    * def createLaoIdNegativeTime =
      """
        function(){
          return "p8TW08AWlBScs9FGXK3KbLQX7Fbgz8_gLwX-B5VEWS0="
        }
      """
    * def createLaoValid =
      """
        function(){
          return "p_EYbHyMv6sopI5QhEXBf40MO_eNoq7V_LygBd4c9RA="
        }
      """
    * def createRollCallValid =
      """
        function(){
          return "Slj7C1LBEXlRC8ItV2B0zWfUSD6YiGJt6N_I_m02uw4="
        }
      """
    * def createRollCallInvalid =
      """
        function(){
          return "Dui7C1LBEXlRC8ItV2B0zWfUSD6YiGJt6N_I_m02uw4="
        }
      """
    * def createValidRollCallOpenId =
    """
        function(){
          return "VSsRrcHoOTQJ-nU_VT_FakiMkezZA86z2UHNZKCxbN8="
        }
      """
    * def createValidRollCallOpenUpdateId =
    """
        function(){
          return "l2OYtZueg1xkjvh3RCWw0nSZrrPNThuaz3U3ys7MjHI="
        }
      """
    * def createInvalidRollCallOpenUpdateId =
    """
        function(){
          return "krCHh6OFWIjSHQiUSrWyx1FV0Jp8deC3zUyelhPG-Yk="
        }
      """
    * def organizerPk =
      """
        function(){
          return "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM="
        }
      """
    * def getLaoIdEmptyName = call createLaoIdEmptyName
    * def getLaoIdNegativeTime = call createLaoIdNegativeTime
    * def getLaoValid = call createLaoValid

    * def getRollCallValidId = call createRollCallValid
    * def getRollCallInvalidId = call createRollCallInvalid

    * def getRollCallOpenValidId = call createValidRollCallOpenId
    * def getRollCallOpenValidUpdateId = call createValidRollCallOpenUpdateId
    * def getRollCallOpenInvalidUpdateId = call createInvalidRollCallOpenUpdateId

    * def getOrganizer = call organizerPk

    * def INVALID_ACTION =          {error: {code: -1, description: '#string'}}
    * def INVALID_RESOURCE =        {error: {code: -2, description: '#string'}}
    * def RESOURCE_ALREADY_EXISTS = {error: {code: -3, description: '#string'}}
    * def INVALID_MESSAGE_FIELD =   {error: {code: -4, description: '#string'}}
    * def ACCESS_DENIED =           {error: {code: -5, description: '#string'}}
    * def INTERNAL_SERVER_ERROR =   {error: {code: -6, description: '#string'}}
    * def VALID_MESSAGE =           {result: 0}
