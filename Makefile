

uname_s := $(shell uname -s)
#$(info uname_s=$(uname_s))
uname_m := $(shell uname -m)
#$(info uname_m=$(uname_m))

all: build deploy test

.PHONY: clean build deploy

build:
ifeq ($(uname_s),Darwin)
ifeq ($(uname_m),arm64)
	echo "Building on M1"
	docker build -f src/main/docker/Dockerfile.graalvm -t graalvm .
	./manageAWS.sh build graalvm
else
	echo "Building on Intel"
	./manageAWS.sh build quay.io/quarkus/ubi-quarkus-native-image:22.3.0-java17
endif
else
	./manageAWS.sh build quay.io/quarkus/ubi-quarkus-native-image:22.3.0-java17
endif

clean:
	./manageAWS.sh clean

deploy:
	./manageAWS.sh deploy

destroy:
	./manageAWS.sh destroy

test:
	./manageAWS.sh test

integration:
	./manageAWS.sh integrationTests

diff:
	./manageAWS.sh diff

# COLORS for help recipe
GREEN  := $(shell tput -Txterm setaf 2)
YELLOW := $(shell tput -Txterm setaf 3)
WHITE  := $(shell tput -Txterm setaf 7)
RESET  := $(shell tput -Txterm sgr0)

# https://gist.github.com/prwhite/8168133#gistcomment-2278355
help:
	@echo ''
	@echo 'Usage:'
	@echo '  ${YELLOW}make${RESET} ${GREEN}<target>${RESET}'
	@echo ''
	@echo 'Targets:'
	@awk '/^[a-zA-Z\-\_0-9]+:/ { \
		helpMessage = match(lastLine, /^		if (helpMessage) { \
			helpCommand = substr($$1, 0, index($$1, ":")-1); \
			helpMessage = substr(lastLine, RSTART + 3, RLENGTH); \
			printf "  ${YELLOW}%-$(TARGET_MAX_CHAR_NUM)s${RESET} ${GREEN}%s${RESET}\n", helpCommand, helpMessage; \
		} \
	} \
	{ lastLine = $$0 }' $(MAKEFILE_LIST)
