package main

import (
	"context"
	"os"
	"sync"
	"testing"
	"time"
)

const waitUp = time.Second * 2

func TestOrganizerAndWitness(t *testing.T) {

	ctx, cancel := context.WithCancel(context.Background())
	wait := sync.WaitGroup{}

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "server", "--pk", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", "serve", "--server-port", "9011", "--client-port", "8010"}
		t.Logf("running server 1: %v", args)

		run(ctx, args)
		t.Log("server 1 done")
	}()

	time.Sleep(waitUp)
	t.Log("server 1 up")

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "server", "--pk", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", "serve", "--client-port", "8020", "--server-port", "9003", "--other-servers", "localhost:9011"}
		t.Logf("running server 2: %v", args)

		run(ctx, args)
		t.Log("server 2 done")
	}()

	time.Sleep(waitUp)
	t.Log("server 2 up")

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "server", "--pk", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", "serve", "--server-port", "9004", "--client-port", "8021", "--other-servers", "localhost:9011", "localhost:9003"}
		t.Logf("running server 3: %v", args)

		run(ctx, args)
		t.Log("server 3 done")
	}()

	time.Sleep(waitUp)
	t.Log("server 3 up")

	cancel()
	wait.Wait()
}

func TestOrganizerAndWitnessWithoutPK(t *testing.T) {

	ctx, cancel := context.WithCancel(context.Background())
	wait := sync.WaitGroup{}

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "server", "serve", "--server-port", "9011", "--client-port", "8010"}
		t.Logf("running server 1: %v", args)

		run(ctx, args)
		t.Log("server 1 done")
	}()

	time.Sleep(waitUp)
	t.Log("server 1 up")

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "server", "serve", "--server-port", "9003", "--client-port", "8020", "--other-servers", "localhost:9011"}
		t.Logf("running server 2: %v", args)

		run(ctx, args)
		t.Log("server 2 done")
	}()

	time.Sleep(waitUp)
	t.Log("server 2 up")

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "server", "serve", "--server-port", "9004", "--client-port", "8021", "--other-servers", "localhost:9011", "localhost:9003"}
		t.Logf("running server 3: %v", args)

		run(ctx, args)
		t.Log("server 3 done")
	}()

	time.Sleep(waitUp)
	t.Log("server 3 up")

	cancel()
	wait.Wait()
}
