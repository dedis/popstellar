@ignore @report=false
Feature: Constants
  Scenario: Creates constants that will be used by other features
    * def createLaoId =
      """
        function(){
          return "p8TW08AWlBScs9FGXK3KbLQX7Fbgz8_gLwX-B5VEWS0="
        }
      """

    * def getLaoIdEmptyName  = call createLaoId

    * def organizerPk =
      """
        function(){
          return "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM="
        }
      """
