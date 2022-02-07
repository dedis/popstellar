# popstellar: Protocol Specifications

Proof-of-personhood, Spring 2021: Protocol specifications

See `/docs/protocol.md` for documentation about the protocol.

See `./examples` to see some examples.

See `./test` to run tests on the examples.

# Format

We use [prettier](https://prettier.io/) as the source of truth for the
formatting of JSON files. 

Install and format with:

```sh
npm install --save-dev --save-exact prettier
prettier --write *.json
```

In any case, prettier will be run by the continuous integration pipeline
when you open a pull request and any necessary changes will be suggested
to you for inclusion.
