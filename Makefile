build:
	mvn clean verify site
lint:
	# brew install ktlint
	ktlint --format
