package ch.epfl.pop

/** This package is used in order to tell spray-json how to convert our custom types in two ways: 1) convert from JSON to one of our custom object (e.g. JsonRequest) 2) convert from on of our custom object to JSON
  *
  * A short explanation on spray-json conversion is provided in `docs/README.md`
  *
  * @note
  *   conversion declaration order matters! If we have the new case class
  * {{{
  *   case class A(b: B)
  * }}}
  * Then the conversion for type `B` must be defined before the conversion for case class `A`
  */
package object json {}
