package src

import( 
	"hash"
	"github.com/gorilla/websocket"
)
type pKey int32 //TODO type ?
type token hash.Hash

type LAO struct {
	// name of LAO
	name string
	//Creation Date/Time
	timestamp int64 //  Unix timestamp (uint64)
	//ID hash : Name || Creation Date/Time Unix Timestamp
	id hash.Hash
	//Organiser: Public Key
	organizerPKey pKey
	//List of public keys where each public key belongs to one witness
	witnesses []pKey
	//List of public keys where each public key belongs to one member (physical person)
	members []pKey
	//List of public keys where each public key belongs to an event
	events []pKey
	//signature (hash)
	attestation hash.Hash // TODO 32 64
	//tab with all created tokens
	tokensEmitted []token
}

//3 nested bucket
//LAO
/*  key : ID => Value new Bucket(LAO_1){
	key  : ID => value = 0xA123DD
	key organizerpkyey => ...LAO
	key memeber => new Bucket(LAO_MEMBERS){
		key : 1 => ...
		key : 2 => ...
	}
*/


/*Private information (*state stored only on the client*):
Authentication: Private Key
LAOs: [] Network
Ownership, membership (regular, witness) can easily be determined when accessing the LAO.
Participating events can also be determined when accessing the LAO object.*/
type Person struct {
	// name of person
	name string
	//public key
	id pKey 
}