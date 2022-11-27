# POPstellar - Proof-of-Personhood System Demonstrator



The main work in this repository is to be done
in the context of the following folders:

* [fe1-web](https://github.com/dedis/popstellar/tree/master/fe1-web): Web-based front-end implementation in TypeScript/ReactJS [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=dedis_popstellar_fe1&metric=coverage)](https://sonarcloud.io/summary/new_code?id=dedis_popstellar_fe1)

* [fe2-android](https://github.com/dedis/popstellar/tree/master/fe2-android): Android native front-end implementation in Java [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=dedis_popstellar_fe2&metric=coverage)](https://sonarcloud.io/summary/new_code?id=dedis_popstellar_fe2)

* [be1-go](https://github.com/dedis/popstellar/tree/master/be1-go): Back-end implementation in Go [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=dedis_popstellar_be1&metric=coverage)](https://sonarcloud.io/summary/new_code?id=dedis_popstellar_be1)

* [be2-scala](https://github.com/dedis/popstellar/tree/master/be2-scala): Back-end implementation in Scala [![Coverage](https://sonarcloud.io/api/project_badges/measure?project=dedis_popstellar_be2&metric=coverage)](https://sonarcloud.io/summary/new_code?id=dedis_popstellar_be2)

* [protocol](https://github.com/dedis/popstellar/tree/master/protocol): The protocol definition in JSON-Schema

* [docs](https://github.com/dedis/popstellar/tree/master/docs): The system & protocol documentation

* [tests](https://github.com/dedis/popstellar/tree/master/tests): (WIP) the system-wide, cross-implementation tests

## Integration Tests
[![be1-go](https://github.com/dedis/popstellar/actions/workflows/karate/be1-go.yaml/badge.svg)](https://github.com/dedis/popstellar/actions/workflows/karate/be1-go.yaml) [Report](https://htmlpreview.github.io/?https://github.com/dedis/popstellar/blob/report-karate-be1-go/overview-features.html)

[![be2-scala](https://github.com/dedis/popstellar/actions/workflows/karate/be2-scala.yaml/badge.svg)](https://github.com/dedis/popstellar/actions/workflows/karate/be2-scala.yaml) [Report](https://htmlpreview.github.io/?https://github.com/dedis/popstellar/blob/report-karate-be2-scala/overview-features.html)

## Branch organization
Everyone working on the project,
please create your own private "working" branches as needed
using the naming convention
`work-(fe*|be*|evoting|consensus|etc.)-<yourname>[-optional-variant]`.
For example,
a branch I create to contribute to the `fe1-web` project
might be called `work-fe1-bford` by default,
or `work-fe1-bford-random-experiment` if I need an additional temporary branch
for a random experiment for example.
As another example,
a branch I create to as part of my work on the `e-voting` project,
and which spans both a back-end and a front-end,
might be called `work-evoting-bford` by default,
or `work-evoting-bford-ballot-casting`
if I need a branch to develop the ballot casting feature.


This project is licensed under the terms of the AGPL licence. If this license is not suitable for your project, please contact us to discuss licensing terms.