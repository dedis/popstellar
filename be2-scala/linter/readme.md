# Scalafix rules for PoPStellar

## Versions:

* **Scala 3**:
[![maven](https://img.shields.io/maven-central/v/io.github.dedis/scapegoat-scalafix_3)](https://search.maven.org/artifact/io.github.dedis/scapegoat-scalafix_3)


* **Scala 2.13**:
[![maven](https://img.shields.io/maven-central/v/io.github.dedis/scapegoat-scalafix_2.13)](https://search.maven.org/artifact/io.github.dedis/scapegoat-scalafix_2.13)

[version]: 1.0
## Description
This subfolder contains a set of custom linter rules developed for the Scala backend of [PopStellar](https://github.com/dedis/popstellar).
They are built on top of the [Scalafix](https://scalacenter.github.io/scalafix/) framework.
Since these rules can also be used by other projects that use Scalafix, they are published on Maven Central, for Scala 2.13 and Scala 3.

## Installation

To install the rules, simply add the following to your `build.sbt` file:
```
ThisBuild / scalafixDependencies += "io.github.dedis" %% "scapegoat-scalafix" % "1.0"
```

**The rules are compatible with Scala 2.13 and Scala 3 (tested for Scala 3.3.1).**

To check proper installation, run `scalafix OptionGet` which should execute the OptionGet rule and succeed if everything went well.

### Note
You might need to add the following lines:
```
inThisBuild(
    List(
        semanticdbEnabled := true,
        semanticdbVersion := scalafixSemanticdb.revision
    )
)
```

This is necessary to enable the SemanticDB, which is required for the rules to work. Only add these lines if SemanticDB is not already enabled in the `build.sbt`.

## Rule list
1. **ArraysInFormat**: Checks if arrays are used in the format method of a string.
2. **CatchNpe**: Checks if a catch block catches a NullPointerException.
3. **ComparingFloatingPointTypes**: Checks if floating point types (float or double) are compared with == or !=.
4. **EmptyInterpolatedString**: Checks if an interpolated string is empty.
5. **IllegalFormatString**: Checks for illegal format strings.
6. **ImpossibleOptionSizeCondition**: Checks if an Option is compared with a size.
7. **IncorrectlyNamedExceptions**: Checks if exceptions are named correctly (end with Exception).
8. **IncorrectNumberOfArgsToFormat**: Checks if the number of arguments in a format string matches the number of arguments in the format method.
9. **LonelySealedTrait**: Checks if a sealed trait has only one implementation.
10. **MapGetGetOrElse**: Checks if a map.get is followed by a getOrElse.
11. **NanComparison**: Checks if a floating point number (float or double) is compared with Double.NaN.
12. **OptionGet**: Checks if an Option is used with get.
13. **OptionSize**: Checks if an Option is used with size.
14. **StripMarginOnRegex**: Checks if a regex is used with stripMargin.
15. **TryGet**: Checks if a Try is used with get.

## Usage

After installation, to run any of these rules, simply call:
```
sbt scalafix RuleName
```

You can also create a `.scalafix.conf` file and enable rules in them. Here is an example with all of the rules enabled:
```
rules = [
  ArraysInFormat,
  CatchNpe,
  ComparingFloatingPointTypes,
  EmptyInterpolatedString,
  IllegalFormatString,
  ImpossibleOptionSizeCondition,
  IncorrectlyNamedExceptions,
  IncorrectNumberOfArgsToFormat,
  LonelySealedTrait,
  MapGetGetOrElse,
  NanComparison,
  OptionGet,
  OptionSize,
  StripMarginOnRegex,
  TryGet
]
```

With this, you can simply run all the rules in the configuration file by calling:
```
sbt scalafix
```

## Usage

To run the rules, simply execute the following command:
```
sbt scalafix
```

## Modifying and adding rules

Scalafix provides some documentation on how to write rules, following their [tutorial](https://scalacenter.github.io/scalafix/docs/developers/tutorial.html) is recommended.

To add a rule, you need to
* Create rule test cases in the `input/src/main/scala/fix` folder
* Create the rule in the `rules/src/main/scala/fix` folder
* Add the rule to the list of rules in `rules/src/main/resources/META-INF/services/scalafix.v1.ScalafixRule`


Output folder is ignored since this is a linter.

## Testing

To test the rules run:
```
sbt test
```

## Publishing

To publish the rules, follow the tutorial on the Scalafix website.
In short you need to:
* Generate a gpg key to sign
* Publish the GPG key to https://keyserver.ubuntu.com
* Create an account on Maven central (see [tutorial](https://central.sonatype.org/register/central-portal/#choosing-a-namespace))
* Verify namespace access for io.github.dedis (see [tutorial](https://central.sonatype.org/register/namespace/#create-an-account))
* Modify the version in `build.sbt`
* Run `sbt publishSigned`
* ZIP the `target/sonatype-staging/VERSION/io/` folder (only include starting from io) and publish it to Maven:
simply click "Publish Component" on Sonatype, set the name to "io.github.dedis:scapegoat-scalafix:VERSION" and upload the ZIP file.

_Don't forget to update the version in the installation section_


