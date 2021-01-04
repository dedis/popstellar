# student20_pop: proto-spec branch
Proof-of-personhood, fall 2020: Protocol specification

_There is a general README at the top-level of the branch._

# High-level (data) communication

## Hashes
Hashes are made on arrays of base64 encoded strings which look like that ["Base64{Foo}", "Base64{Bar}"]. This is necessary to avoid some issues with inner "