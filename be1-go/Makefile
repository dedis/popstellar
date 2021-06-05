.PHONY: test

build:
	make copy_protocol
	go build -o pop ./cli/
	make clean_protocol

test:
	make copy_protocol
	go test -v ./...
	make clean_protocol

copy_protocol:
	cp -r ../protocol ./validation 

clean_protocol:
	rm -rf validation/protocol
