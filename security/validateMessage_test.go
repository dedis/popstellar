// define/securityHelpers
package security

import (
	ed "crypto/ed25519"
	"crypto/sha256"
	b64 "encoding/base64"
	"encoding/json"
	"errors"
	"fmt"
	"math/rand"
	"testing"
	"time"

	"student20_pop/lib"
	message2 "student20_pop/message"
	"student20_pop/parser"
)

func TestMessageIsValidWithoutWitnesses(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey, privkey := createKeyPair()
		witnessSignatures := []message2.ItemWitnessSignatures{}
		witnessKeys := [][]byte{}
		var creation = time.Now().Unix()
		name := "My LAO"
		data, err := createDataLao(pubkey, privkey, witnessKeys, creation, name)
		if err != nil {
			t.Error(err)
		}
		err = CheckMessageIsValid(pubkey, privkey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}
func TestRollCallCreatedIsValid(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey, privkey := createKeyPair()
		witnessSignatures := []message2.ItemWitnessSignatures{}
		data, err := createRollCallNow(pubkey, privkey)
		if err != nil {
			t.Error(err)
		}
		err = CheckMessageIsValid(pubkey, privkey, data, witnessSignatures)
		if err != nil {
			t.Error(err)
		}
	}
}

func TestLAOIsValid(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey, privkey := createKeyPair()
		witnessKeys := [][]byte{}
		var creation = time.Now().Unix()
		name := "My LAO"
		data, err := createDataLao(pubkey, privkey, witnessKeys, creation, name)
		if err != nil {
			t.Error(err)
		}
		valid := LAOIsValid(data, true)
		if valid != true {
			t.Errorf("Created Lao Should be valid %#v", data)
		}
		//==================invalid Tests========================//
	}
}
// TODO (should be not empty but) not in the protospecs ?
func TestLAOEmptyLocation(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey, privkey := createKeyPair()
		witnessKeys := [][]byte{}
		var creation = time.Now().Unix()
		name := ""
		data, err := createDataLao(pubkey, privkey, witnessKeys, creation, name)
		if err != nil {
			t.Error(err)
		}
		valid := LAOIsValid(data, true)
		if valid != false {
			t.Errorf("Created Lao Should be invalid due to empty location %#v", data)
		}
	}
}
func TestLAOIInvalidCreationTime(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey, privkey := createKeyPair()
		witnessKeys := [][]byte{}
		var creation = time.Now().Unix()
		name := ""
		name = "ok"
		creation = time.Now().Unix() - MaxClockDifference - 1
		data, err := createDataLao(pubkey, privkey, witnessKeys, creation, name)
		if err != nil {
			t.Error(err)
		}
		valid := LAOIsValid(data, true)
		if valid != false {
			t.Errorf("Created Lao Should be invalid due to wrong creation time %#v", data)
		}
	}
}

//===================================================================================//
func CheckMessageIsValid(pubkey []byte, privkey ed.PrivateKey, data interface{}, witnessKeysAndSignatures []message2.ItemWitnessSignatures) error {
	var dataFlat, signed, id []byte
	var err error
	switch data.(type) {
	case message2.DataCreateLAO:
		dataFlat, signed, id, err = getIdofMessage(data.(message2.DataCreateLAO), privkey)
	case message2.DataCreateRollCall:
		dataFlat, signed, id, err = getIdofMessage(data.(message2.DataCreateRollCall), privkey)
	}
	if err != nil {
		return err
	}

	//witness signatures
	ArrayOfWitnessSignatures, err := PlugWitnessesInArray(witnessKeysAndSignatures)
	if err != nil {
		return err
	}
	var message = message2.Message{
		Data:              dataFlat, // in base 64
		Sender:            pubkey,
		Signature:         signed,
		MessageId:         id[:],
		WitnessSignatures: ArrayOfWitnessSignatures,
	}
	messageFlat, err := json.Marshal(message)
	if err != nil {
		return err
	}
	messProcessed, err := parser.ParseMessage(messageFlat)
	if err != nil {
		return err
	}
	err = MessageIsValid(messProcessed)
	if err != nil {
		return err
	}
	return nil
}

/* Basically following the last meeting (22/12/20) we are not supposed to have this case
func TestMessageIsValidWithAssessedWitnesses(t *testing.T) {
	//increase nb of tests
	for i := 0; i < 100; i++ {
		pubkey,privkey := createKeyPair()
		keyz := createArrayOfkeys()
		witnessKeys:= onlyPublicKeys(keyz)
		data, err:= createDataLao(pubkey,privkey,witnessKeys)
		if err != nil {
			t.Error(err)
		}
		id,err:= getIdofMessage(data,privkey)
		if err != nil {
			t.Error(err)
		}
		witnessSignatures := arrayOfWitnessSignatures(keyz,id)
		err = CheckMessageIsValid(pubkey,privkey,data,witnessSignatures,witnessKeys)
		if err != nil {
			t.Error(err)
		}
	}
}
*/
func PlugWitnessesInArray(witnessKeysAndSignatures []message2.ItemWitnessSignatures) ([]json.RawMessage, error) {
	ArrayOfWitnessSignatures := []json.RawMessage{}
	for i := 0; i < len(witnessKeysAndSignatures); i++ {
		witnessSignatureI, err := json.Marshal(witnessKeysAndSignatures[i])
		if err != nil {
			return nil, errors.New("Problem when Marshaling witnessKeysAndSignatures")
		}
		CoupleToAdd := witnessSignatureI[:]
		ArrayOfWitnessSignatures = append(ArrayOfWitnessSignatures, CoupleToAdd)
	}
	return ArrayOfWitnessSignatures, nil
}
func createKeyPair() ([]byte, ed.PrivateKey) {
	//randomize the key
	randomSeed := make([]byte, 32)
	rand.Read(randomSeed)
	privkey := ed.NewKeyFromSeed(randomSeed)
	return privkey.Public().(ed.PublicKey), privkey
}

func createDataLao(orgPubkey []byte, privkey ed.PrivateKey, WitnesseKeys [][]byte, creation int64, name string) (message2.DataCreateLAO, error) {
	if (len(orgPubkey) != ed.PublicKeySize) || len(privkey) != ed.PrivateKeySize {
		return message2.DataCreateLAO{}, errors.New("wrong argument -> size of public key don't respected ")
	}
	var itemsToHashForId []string
	itemsToHashForId = append(itemsToHashForId, string(orgPubkey), fmt.Sprint(creation), name)
	idData := hashOfItems(itemsToHashForId)
	var data = message2.DataCreateLAO{
		Object:    "lao",
		Action:    "create",
		ID:        idData[:],
		Name:      name,
		Creation:  creation,
		Organizer: orgPubkey,
		Witnesses: WitnesseKeys,
	}
	return data, nil
}
func createRollCallNow(pubkey []byte, privkey ed.PrivateKey) (message2.DataCreateRollCall, error) {
	var creation int64 = 123
	name := "RollCallNow"
	if (len(pubkey) != ed.PublicKeySize) || len(privkey) != ed.PrivateKeySize {
		return message2.DataCreateRollCall{}, errors.New("wrong argument -> size of public key don't respected ")
	}

	idData := sha256.Sum256([]byte(string(pubkey) + fmt.Sprint(creation) + name))
	var data = message2.DataCreateRollCall{
		Object:              "roll_call",
		Action:              "create",
		ID:                  idData[:],
		Name:                name,
		Creation:            creation,
		Location:            "pas loin",
		Start:               6,
		RollCallDescription: "un roll call",
	}
	return data, nil
}

func getIdofMessage(data interface{}, privkey ed.PrivateKey) (dataFlat, signed, id []byte, err error) {
	dataFlat, err = json.Marshal(data)
	if err != nil {
		return nil, nil, nil, errors.New("Error : Impossible to marshal data")
	}
	signed = ed.Sign(privkey, dataFlat)

	var itemsToHashForMessageId []string
	itemsToHashForMessageId = append(itemsToHashForMessageId, string(dataFlat), b64.StdEncoding.EncodeToString(signed))
	hash := hashOfItems(itemsToHashForMessageId)
	return dataFlat, signed, hash, nil

}

func hashOfItems(itemsToHash []string) []byte {
	hash := sha256.Sum256([]byte(lib.ComputeAsJsonArray(itemsToHash)))
	return hash[:]
}
