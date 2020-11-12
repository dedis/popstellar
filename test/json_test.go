// json.go
package main

import (
    "encoding/json"
    "fmt"
    "net/http"
)

type Person struct {
	Firstname 	string 	`json:"firstname"`
	Lastname 	string 	`json:"lastname"`
}

type LAO struct {
    Name 		string 			`json:"name"`
    Organizer  	Person 			`json:"org"`
    Attendees	List[Person]	`json:"attendees"`
}



func main() {
    http.HandleFunc("/decode", func(w http.ResponseWriter, r *http.Request) {
        var lao LAO
        json.NewDecoder(r.Body).Decode(&lao)

        fmt.Fprintf(w, "The LAO %s is hosted by %s", lao.Name, lao.org)
    })

    http.HandleFunc("/encode", func(w http.ResponseWriter, r *http.Request) {
        newlao := User{
            Name: 		"Vote",
            Organizer: 	"Doe",
            Attendees:  nil,
        }

        json.NewEncoder(w).Encode(newlao)
    })

    http.ListenAndServe(":8080", nil)
}