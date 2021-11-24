.PHONY: test

build: protocol
	go build -o pop ./cli/main

lint:
	@go get -v honnef.co/go/tools/cmd/staticcheck
	@go mod tidy
	staticcheck ./...

check-cov: protocol lint vet
	go test -v ./...
	go test -v -race -coverpkg=./... -coverprofile=coverage.out ./... -json > report.json

check: protocol lint vet
	go test -v ./...
	go test -v -race ./...

vet: protocol
	go vet ./...

protocol:
	cp -r ../protocol ./validation 

clean:
	rm -rf validation/protocol

fmt:
	gofmt -s -w ./