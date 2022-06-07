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
	args := os.Args[0:1]
	args = append(args, "organizer", "--pk", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", "serve", "--witness-port", "9011", "--client-port", "8010")

	ctx, cancel := context.WithCancel(context.Background())
	wait := sync.WaitGroup{}

	t.Logf("running Organizer: %v", args)
	wait.Add(1)
	go func() {
		defer wait.Done()

		run(ctx, args)
		t.Log("organizer done")
	}()

	time.Sleep(waitUp)
	t.Log("organizer up")

	// Test Witness -> Organizer
	args = args[0:1]
	args = append(args, "witness", "--pk", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", "serve", "--witness-port", "9003", "--organizer-address", "localhost:9011", "--client-port", "8020")

	t.Logf("running Witness 1: %v", args)
	wait.Add(1)
	go func() {
		defer wait.Done()

		run(ctx, args)
		t.Log("witness 1 done")
	}()

	time.Sleep(waitUp)
	t.Log("witness 1 up")

	// Test Witness -> Witness
	args = args[0:1]
	args = append(args, "witness", "--pk", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", "serve", "--witness-port", "9004", "--organizer-address", "localhost:9011", "--client-port", "8021", "--other-witness", "localhost:9003")

	t.Logf("running Witness 2: %v", args)
	wait.Add(1)
	go func() {
		defer wait.Done()

		run(ctx, args)
		t.Log("witness 2 done")
	}()

	time.Sleep(waitUp)
	t.Log("witness 2 up")

	cancel()
	wait.Wait()
}
