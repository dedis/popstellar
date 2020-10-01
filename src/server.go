package main

import (
	"bufio"
	"fmt"
	"net"
	"strings"
	"time"
)

func main() {
	l, err := net.Listen("tcp", "188.154.180.55:8080")
	if err != nil {
		fmt.Println(err)
		return
	}

	l.Close()

	c, err := l.Accept()

	if err != nil {
		fmt.Println(err)
		return
	}

	for {
		netData, err := bufio.NewReader(c).ReadString('\n')
		if err != nil {
			fmt.Println(err)
			return
		}
		if strings.TrimSpace(netData) == "STOP" {
			fmt.Println("Exiting TCP server!")
			return
		}

		fmt.Print("-> ", netData)
		t := time.Now()
		myTime := t.Format(time.RFC3339) + "\n"
		_, err = c.Write([]byte(myTime))
		if err != nil {
			fmt.Println("unable to write to TCP stream")
		}
	}

}
