# popstellar: be2-scala branch
Proof-of-personhood, spring 2021: Scala language back-end

[TOC]

## Running the project
<span style="color:red;font-weight:bold">
Make sure to open be2-scala project folder as the root of your IDE workspace.
</span>



There are two main possible ways of running the project :

### Option 1: Intellij / VSCode

1. Import the project using your editor
2. Modify the default  Run configuration 'Server', to include the following __VM option__: <br>
__```-Dscala.config=src/main/scala/ch/epfl/pop/config```__

![](docs/images/intellij-vm.png)

### Option 2: SBT
Using `sbt -Dscala.config="path/to/config/file" run`.

 There is a default configuration ready to use in `src/main/scala/ch/epfl/pop/config` which contains an __application.config__ where the configuration lives. This can be updated if needed.
 ```apacheconf
# Snapshot of application.config

ch_epfl_pop_Server {
	http {
		interface = "127.0.0.1"
		port = "8000"
		path = ""
	}
}
 ```
Consequently, from **be2-scala/** folder run the following:
```bash
 sbt -Dscala.config="src/main/scala/ch/epfl/pop/config" run
```

---



## Preprocessor flags

We introduced two custom [preprocessor flags](https://gcc.gnu.org/onlinedocs/gcc/Preprocessor-Options.html), one of which you already encountered:

- Config file location (**mandatory**): location of the config file on the system with respect to the be2-scala folder
- Database auto-cleanup (optional). By adding the `-Dclean` flag, the database will be recreated everytime the server starts running

---



## External libraries

The project relies on several sbt dependencies (external libraries) :

- websockets : [**akka-http**](https://doc.akka.io/docs/akka-http/current/introduction.html) for websocket server. It uses [akka-streams](https://doc.akka.io/docs/akka/current/stream/index.html) as dependency.
- database : [**leveldb**](https://github.com/codeborui/leveldb-scala) which relies on both [snappy](https://search.maven.org/artifact/org.xerial.snappy/snappy-java/1.1.7.3/jar) (for compression/decompression) and [akka-persistence](https://doc.akka.io/docs/akka/current/persistence.html)
- Json parser : [**spray-json**](https://github.com/spray/spray-json) for Json encoding/decoding
- encryption : [**tink**](https://github.com/google/tink/blob/master/docs/JAVA-HOWTO.md) to verify signatures
- testing : [**scalatest**](https://www.scalatest.org/) for unit tests
- Json schema validator : [**networknt**](https://github.com/networknt/json-schema-validator) for Json schema validation

---



## Coding convention

Our coding guidelines can be found [here](https://docs.scala-lang.org/style/).
