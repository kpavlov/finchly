Thank you for investing your time and effort in contributing to my project, I appreciate it a lot! 🤗

# General Guidelines

- **Open an Issue**: If you want to contribute a bug fix or a new feature that isn't listed in the [issues](https://github.com/kpavlov/awesome-kotlin-maven-template/issues) yet, please open a new issue for it. We will prioritize it shortly.
- **Follow Best Practices**: Adhere to [Google's Best Practices for Java Libraries](https://jlbp.dev/) and [Google Engineering Practices](https://google.github.io/eng-practices/).
- **Java Compatibility**: Ensure the code is compatible with Java 21.
- **Dependency Management**: Avoid adding new dependencies wherever possible (new dependencies with test scope are OK). If absolutely necessary, try to use the same libraries which are already used in the project. Make sure you run `mvn dependency:analyze` to identify unnecessary dependencies.
- **Adhere to S.O.L.I.D. Principles**: Follow [S.O.L.I.D.](https://en.wikipedia.org/wiki/SOLID) principles in your code.
- **Testing**: Write unit and/or integration tests for your code. This is critical: no tests, no review! Tests should be designed in a way to run in parallel.
- **Run All Tests**: Make sure you run all tests on all modules with `mvn clean verify`.
- **Maintain Backward Compatibility**: Avoid making breaking changes. Always keep backward compatibility in mind. For example, instead of removing fields/methods/etc, mark them `@Deprecated` and make sure they still work as before.
- **Naming Conventions**: Follow existing naming conventions.
- **No Lombok**: Avoid using Lombok in the codebase.
- **Documentation**: Add Javadoc where necessary, but the code should be self-documenting.
- **Code Style**: Follow the existing code style present in the project.
- **Discuss Large Features**: Large features should be discussed with maintainers before implementation.
- **Thread Safety**: Ensure that the code you write is thread-safe.
