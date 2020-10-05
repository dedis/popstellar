package main

import (
	"log"

	"github.com/sciter-sdk/go-sciter"
	"github.com/sciter-sdk/go-sciter/window"
)

func main() {

	rect := sciter.NewRect(200, 200, 300, 400)
	//create sciter window object
	win, _ := window.New(sciter.SW_MAIN|sciter.SW_TITLEBAR, rect)

	//set window title
	win.SetTitle("Load HTML page as UI")

	win.LoadFile("./index.html")

	win.DefineFunction("ReadFromSciter", ReadFromSciter)

	win.Show()

	win.Run()
}

//ReadFromSciter is a function which read input from sciter textarea
func ReadFromSciter(val ...*sciter.Value) *sciter.Value {
	//basic idea
	imput := val[0].String()
	log.Println(imput)
	return nil
}
