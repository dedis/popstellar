# student20_pop: be2-scala branch
Proof-of-personhood, fall 2020: Scala language back-end

[TOC]

## Running the project

There are two main possible ways of running the project :
* import the project using IntelliJ
* using **sbt**, execute `sbt compile` and `sbt run`

---



## External libraries

The project relies on several sbt dependencies (external libraries) :

- websockets : [**akka-http**](https://doc.akka.io/docs/akka-http/current/introduction.html) for websocket server. It uses [akka-streams](https://doc.akka.io/docs/akka/current/stream/index.html) as dependency.
- database : [**leveldb**](https://github.com/codeborui/leveldb-scala) which relies on both [snappy](https://search.maven.org/artifact/org.xerial.snappy/snappy-java/1.1.7.3/jar) (for compression/decompression) and [akka-persistence](https://doc.akka.io/docs/akka/current/persistence.html)
- Json parser : [**spray-json**](https://github.com/spray/spray-json) for Json encoding/decoding
- encryption : [**scrypto**](https://index.scala-lang.org/input-output-hk/scrypto/scrypto/2.1.9?target=_2.13) -- *not sure yet about this library*

---



## Coding convention

Our coding guidelines can be found [here](https://docs.scala-lang.org/style/).
