package main

import (
	"bufio"
	"fmt"
	"net"
	"os"
)

func main() {
	reader := bufio.NewReader(os.Stdin)

	// init
	tcpAddr, err := net.ResolveTCPAddr("tcp", "127.0.0.1:8080")
	if err != nil {
		fmt.Printf("TCP resolve failed:", err.Error())
		os.Exit(1)
	}
	conn, err := net.DialTCP("tcp", nil, tcpAddr)
	if err != nil {
		fmt.Printf("TCP dial failed:", err.Error())
		os.Exit(1)
	}
	for {
		fmt.Printf("Send Message :")
		text, _ := reader.ReadString('\n')
		// send message
		_, err = conn.Write([]byte(text))
		if err != nil {
			println("Write to server failed:", err.Error())
			os.Exit(1)
		}

		fmt.Println("Write to server  : ", text)

		reply := make([]byte, 1024)

		_, err = conn.Read(reply)
		if err != nil {
			println("Write to server failed:", err.Error())
			os.Exit(1)
		}

		println("reply from server=", string(reply))
	}

	conn.Close()
}
