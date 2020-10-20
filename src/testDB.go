package main

import (
	"fmt"
	"github.com/boltdb/bolt"
	"log"
	"strconv"
)

func main() {
	db, err := bolt.Open("src/TestPublishSubscribe/test.db", 0777, nil)
	if err != nil {
		log.Fatal("there was an error : ", err)
	}

	defer db.Close()

	er := db.Update(func(tx *bolt.Tx) error {
		// Assume bucket exists and has keys
		b := tx.Bucket([]byte("MyBucket"))

		for i := 0; i < 10; i++ {
			v := b.Get([]byte(strconv.Itoa(i)))
			fmt.Printf("%s \n", v)
		}

		return nil
	})

	if er != nil {
		log.Fatal(er)
	}
}
