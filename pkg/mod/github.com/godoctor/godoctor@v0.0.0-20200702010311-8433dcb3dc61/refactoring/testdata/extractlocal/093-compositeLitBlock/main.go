package main

import "fmt"

func apple() string {
	a := "apple +"
	return a
}

type fruit struct {
	name    string
	vars    map[string]string
	isthere bool
}

func (f *fruit) orange() string {
	return "helloz worldz"
}

func main() {

	o2 := fruit{name: "os", // <<<<< var,22,14,24,17,newVar,pass
		vars:    map[string]string{"apple": "orange", "pineapple": "strawberry"},
		isthere: true}
	s := o2.orange()
	fmt.Println(s)
}
