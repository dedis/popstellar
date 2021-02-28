package student20_pop

import "go.dedis.ch/kyber/v3/suites"

// Suite points to the `Ed25519` suite in Kyber.
var Suite = suites.MustFind("Ed25519")
