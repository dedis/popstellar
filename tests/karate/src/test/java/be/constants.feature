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

    * def organizerPk =
      """
        function(){
          return "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM="
        }
      """
    * def getLaoIdEmptyName = call createLaoIdEmptyName
    * def getLaoIdNegativeTime = call createLaoIdNegativeTime
    * def getLaoValid = call createLaoValid
    * def getOrganizer = call organizerPk
    * def INTERNAL_SERVER_ERROR = {error: {code: -6, description: '#string'}}
    * def INVALID_MESSAGE_FIELD = {error: {code: -4, description: '#string'}}
    * def VALID_MESSAGE = {result: 0}
