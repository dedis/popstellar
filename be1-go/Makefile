.PHONY: test

build: protocol
	go build -o pop ./cli

lint:
	@go install honnef.co/go/tools/cmd/staticcheck@latest
	@go mod tidy
	staticcheck ./...

test: protocol
	go test -v -race ./...

test-no-cache: protocol
	go test -v -race ./... -count=1

test-cov: protocol
	go test -v -coverpkg=./... -coverprofile=coverage.out ./... -json > report.json

vet: protocol
	go vet ./...

check: test test-cov lint vet

protocol:
	cp -r ../protocol ./validation 

clean:
	rm -rf validation/protocol

fmt:
	gofmt -s -w ./

check-fmt:
	gofmt -d -e -s ./