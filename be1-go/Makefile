version=$(shell git describe --abbrev=0 --tags || echo '0.0.0')
versionFlag="popstellar.Version=$(version)"
versionFile=$(shell echo $(version) | tr . _)
timeFlag="popstellar.BuildTime=$(shell date +'%d/%m/%y_%H:%M')"
shortsha=$(shell git rev-parse --short HEAD)
shaFlag="popstellar.ShortSHA=$(shortsha)"

.PHONY: test

build: protocol
	go build -ldflags="-X $(versionFlag) -X $(timeFlag) -X $(shaFlag)" -o pop ./cli
	GOOS=linux GOARCH=amd64 go build -ldflags="-X $(versionFlag) -X $(timeFlag) -X $(shaFlag)" -o pop-linux-amd64-$(versionFile) ./cli
	GOOS=darwin GOARCH=amd64 go build -ldflags="-X $(versionFlag) -X $(timeFlag) -X $(shaFlag)" -o pop-darwin-amd64-$(versionFile) ./cli
	GOOS=darwin GOARCH=arm64 go build -ldflags="-X $(versionFlag) -X $(timeFlag) -X $(shaFlag)" -o pop-darwin-arm64-$(versionFile) ./cli
	GOOS=windows GOARCH=amd64 go build -ldflags="-X $(versionFlag) -X $(timeFlag) -X $(shaFlag)" -o pop-windows-amd64-$(versionFile) ./cli

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