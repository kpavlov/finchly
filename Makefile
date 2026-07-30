build:
	mvn clean verify site
apidocs:
	@echo "📚  Building API Docs..."
	@rm -rf target/reports/apidocs
	@mvn compile javadoc:aggregate \
		-pl finchly-core,finchly-kafka,finchly-rabbitmq -am \
		--no-transfer-progress
	@echo " ✅  Done!"
lint:
	# brew install ktlint
	ktlint --format
  # https://docs.openrewrite.org/recipes/maven/bestpractices
	mvn -U org.openrewrite.maven:rewrite-maven-plugin:run \
		-Drewrite.activeRecipes=org.openrewrite.maven.BestPractices \
		-Drewrite.exportDatatables=true
