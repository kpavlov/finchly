build:
	mvn clean verify site
lint:
	# brew install ktlint
	ktlint --format
  # https://docs.openrewrite.org/recipes/maven/bestpractices
	mvn -U org.openrewrite.maven:rewrite-maven-plugin:run \
		-Drewrite.activeRecipes=org.openrewrite.maven.BestPractices \
		-Drewrite.exportDatatables=true
