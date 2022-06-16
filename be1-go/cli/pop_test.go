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

		args := []string{os.Args[0], "organizer", "--pk", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", "serve", "--witness-port", "9011", "--client-port", "8010"}
		t.Logf("running Organizer: %v", args)

		run(ctx, args)
		t.Log("organizer done")
	}()

	time.Sleep(waitUp)
	t.Log("organizer up")

	// Test Witness -> Organizer

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "witness", "--pk", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", "serve", "--witness-port", "9003", "--organizer-address", "localhost:9011", "--client-port", "8020"}
		t.Logf("running Witness 1: %v", args)

		run(ctx, args)
		t.Log("witness 1 done")
	}()

	time.Sleep(waitUp)
	t.Log("witness 1 up")

	// Test Witness -> Witness

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "witness", "--pk", "J9fBzJV70Jk5c-i3277Uq4CmeL4t53WDfUghaK0HpeM=", "serve", "--witness-port", "9004", "--organizer-address", "localhost:9011", "--client-port", "8021", "--other-witness", "localhost:9003"}
		t.Logf("running Witness 2: %v", args)

		run(ctx, args)
		t.Log("witness 2 done")
	}()

	time.Sleep(waitUp)
	t.Log("witness 2 up")

	cancel()
	wait.Wait()
}

func TestOrganizerAndWitnessWithoutPK(t *testing.T) {

	ctx, cancel := context.WithCancel(context.Background())
	wait := sync.WaitGroup{}

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "organizer", "serve", "--witness-port", "9011", "--client-port", "8010"}
		t.Logf("running Organizer: %v", args)

		run(ctx, args)
		t.Log("organizer done")
	}()

	time.Sleep(waitUp)
	t.Log("organizer up")

	// Test Witness -> Organizer

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "witness", "serve", "--witness-port", "9003", "--organizer-address", "localhost:9011", "--client-port", "8020"}
		t.Logf("running Witness 1: %v", args)

		run(ctx, args)
		t.Log("witness 1 done")
	}()

	time.Sleep(waitUp)
	t.Log("witness 1 up")

	// Test Witness -> Witness

	wait.Add(1)
	go func() {
		defer wait.Done()

		args := []string{os.Args[0], "witness", "serve", "--witness-port", "9004", "--organizer-address", "localhost:9011", "--client-port", "8021", "--other-witness", "localhost:9003"}
		t.Logf("running Witness 2: %v", args)

		run(ctx, args)
		t.Log("witness 2 done")
	}()

	time.Sleep(waitUp)
	t.Log("witness 2 up")

	cancel()
	wait.Wait()
}
