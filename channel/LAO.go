package channel

import (
	"encoding/json"
	"github.com/boltdb/bolt"
	"student20_pop/db"
	"student20_pop/define"
)

// would be nice to have an interface that contains methods add, remove and edit for LAO, event and vote

/**
 * Function to create a new Object (LAO,Event...) and store it in the DB
 * @returns : error
 */
func writeObject(obj interface{}, secure bool) error {
	db, e := db.OpenDB(channelDatabase)
	defer db.Close()
	if e != nil {
		return e
	}

	err := db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucketIfNotExists([]byte(bucketChannel))
		if err1 != nil {
			return err1
		}
		//generic adaptation
		var objID []byte
		switch obj.(type){
		case define.LAO :
			// type assert
			objID = []byte (obj.(define.LAO).ID)
		case define.Event:
			objID = []byte (obj.(define.Event).ID)
		default:
			//TODO not sure for the error type
			return define.ErrRequestDataInvalid
		}
		//checks if there is already an entry with that ID if secure is true
		if secure {
			key := b.Get(objID)
			if key != nil {
				return define.ErrResourceAlreadyExists
			}
		} else {
			exists := b.Get(objID)
			if exists == nil {
				return define.ErrInvalidResource
			}
		}
		var dt []byte
		var err2 error
		switch obj.(type){
		case define.LAO :
			// type assert
			dt, err2 = json.Marshal(obj.(define.LAO).ID)
		case define.Event:
			dt, err2 = json.Marshal(obj.(define.Event).ID)
		default:
			//TODO not sure for the error type
			return define.ErrRequestDataInvalid
		}
		// Marshal the Obj and store it
		if err2 != nil {
			return define.ErrRequestDataInvalid
		}
		err3 := b.Put(objID, dt)
		return err3
	})

	return err
}

/**
 * Function to create a new LAO and store it in the DB
 * @returns : error
 *
func writeLAO(lao define.LAO, secure bool) error {
	db, e := db.OpenDB(channelDatabase)
	defer db.Close()
	if e != nil {
		return e
	}

	err := db.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucketIfNotExists([]byte(bucketChannel))
		if err1 != nil {
			return err1
		}
		//checks if there is already an entry with that ID if secure is true
		if secure {
			key := b.Get([]byte (canal))
			if key != nil {
				return define.ErrResourceAlreadyExists
			}
		} else {
			exists := b.Get([]byte (canal))
			if exists == nil {
				return define.ErrInvalidResource
			}
		}
		// Marshal the LAO and store it
		dt, err2 := json.Marshal(lao)
		if err2 != nil {
			return define.ErrRequestDataInvalid
		}
		err3 := b.Put([]byte (canal), dt)

		return err3
	})

	return err
}
*/

/*writes a Event to the DB, returns an error if ID already is key in DB*/
func CreateObject(obj interface{}) error {
	return writeObject(obj, true)
}

/*writes a lao to the DB, regardless of ID already exists*/
func UpdateObject(obj define.LAO) error {
	return writeObject(obj, false)
}

/*returns channel data from a given ID */
func GetFromID(id []byte) []byte {
	db, e := db.OpenDB(channelDatabase)
	defer db.Close()
	if e != nil {
		return nil
	}
	var data []byte
	e = db.View(func(tx *bolt.Tx) error {
		b := tx.Bucket([]byte(bucketChannel))
		data = b.Get(id)
		return nil
	})

	return data
}
