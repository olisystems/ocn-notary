deps:
	@cd js && npm install

test:
	@printf "testing libraries\n\n"
	@echo "--- js package ---"
	@cd js && npm test
	@printf "\n--- java package ---\n\n"
	@cd jvm && ./gradlew test
	@printf "\ntests exited successfully\n"


build:
	@printf "building libraries\n\n"
	@echo "--- js package ---"
	@cd js && npm run build
	@printf "\n--- java package ---\n\n"
	@cd jvm && ./gradlew build
	@printf "\nbuilds exited successfully\n"
	
publish:
	@printf "publishing libraries\n\n"
	@echo "--- js package ---"
	@cd js && npm publish || true
	@printf "\n--- java package ---\n\n"
	@cd jvm && ./gradlew bintrayUpload || true
	@printf "\publish exited successfully\n"
