package main

import (
	"os"
	"syscall"
	"testing"
	"time"

	"github.com/stretchr/testify/require"
)

func Test_Pop(t *testing.T) {
	// set up a organizer server
	args := os.Args[0:1]
	args = append(args, "organizer", "--pk", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")
	args = append(args, "--tt", "true")
	args = append(args, "--spa", "127.0.0.1")
	args = append(args, "--sla", "127.0.0.1")
	args = append(args, "--cp", "9000")
	args = append(args, "--wp", "9002")
	args = append(args, "serve")

	os.Setenv("LLVL", "warn")

	pr, err := os.StartProcess(args[0], args, &os.ProcAttr{})
	require.NoError(t, err)

	time.Sleep(time.Second * 2)
	pr.Signal(syscall.SIGINT)

	// set up a witness server
	args = os.Args[0:1]
	args = append(args, "witness", "--pk", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=")
	args = append(args, "--org", "127.0.0.1:9002")
	args = append(args, "serve")

	os.Setenv("LLVL", "debug")

	pr, err = os.StartProcess(args[0], args, &os.ProcAttr{})
	require.NoError(t, err)

	time.Sleep(time.Second * 2)
	pr.Signal(syscall.SIGINT)

}
