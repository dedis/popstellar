package main

import "fmt"

// Test for renaming method with same name  in different  interfaces implemented by differnet structs

type simple interface {

mymethod()

} 

type complex interface {

mymethod(n int)

}

type mystruct struct {

myvar string

}

type otherstruct struct {

othervar string

}



func main() {

mystructvar := mystruct {"helloo" }

mystructvar.mymethod()		// <<<<< rename,37,13,37,13,renamed,pass

otherstructvar := otherstruct {"hiiiiii" }

otherstructvar.mymethod(100)		

}

func (mystructvar *mystruct)mymethod() {


fmt.Println(mystructvar.myvar)


}

func (otherstructvar *otherstruct)mymethod(n int) {

fmt.Println(n)

fmt.Println(otherstructvar.othervar)


}

