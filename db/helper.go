package db

import (
	"github.com/boltdb/bolt"
)

/**
Functions that writes a pair (key, val) in the bucket "bkt" in the database
*/
func Write(key []byte, val []byte, bkt []byte, database *bolt.DB) error {
	err := database.Update(func(tx *bolt.Tx) error {
		b, err1 := tx.CreateBucketIfNotExists(bkt)
		if err1 != nil {
			return err1
		}
		err2 := b.Put(key, val)
		if err2 != nil {
			return err2
		}
		return nil
	})
	return err
}
