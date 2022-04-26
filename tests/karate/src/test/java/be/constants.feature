@ignore @report=false
Feature: Constants
  Scenario: Creates constants that will be used by other features
    # TODO: make the function depend on all the attributes the lao id depends on
    * def createLaoId =
      """
        function(){
          return "p8TW08AWlBScs9FGXK3KbLQX7Fbgz8_gLwX-B5VEWS0="
        }
      """

    * def organizerPk =
      """
        function(){
          return "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM="
        }
      """
    * def getLaoIdEmptyName = call createLaoId
    * def getOrganizer = call organizerPk
    * def INTERNAL_SERVER_ERROR = -6
    * def INVALID_MESSAGE_FIELD = -4
