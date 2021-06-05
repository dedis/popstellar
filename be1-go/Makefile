.PHONY: test

define execute_command
	make copy_protocol
    $(1)
	make clean_protocol
endef

build:
	$(call execute_command, go build -o pop ./cli/)

test:
	$(call execute_command, go test -v ./...)

test_race:
	$(call execute_command, go test -race -v ./...)

copy_protocol:
	cp -r ../protocol ./validation 

clean_protocol:
	rm -rf validation/protocol
