/*
rule = IncorrectlyNamedExceptions
 */
package fix

// Test values taken from Scapegoat test
object IncorrectlyNamedExceptions {
  class NotException // scalafix: ok;

  class IsException extends Exception // scalafix: ok;

  class IsNotNamedCorrectly extends Exception // assert: IncorrectlyNamedExceptions

  class Is extends Exception // assert: IncorrectlyNamedExceptions

  class IsChild extends Is // assert: IncorrectlyNamedExceptions

  class IsChildException // scalafix: ok;

  class IsRuntimeException extends RuntimeException // scalafix: ok;

  class IsRuntime extends Exception // assert: IncorrectlyNamedExceptions

}

