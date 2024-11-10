# Finchly

[![Maven Central](https://img.shields.io/maven-central/v/me.kpavlov.finchly/root)](https://repo1.maven.org/maven2/me/kpavlov/finchly/)
[![Kotlin CI with Maven](https://github.com/kpavlov/finchly/actions/workflows/maven.yml/badge.svg?branch=main)](https://github.com/kpavlov/finchly/actions/workflows/maven.yml)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/3aa0b5847e70494d9795ff98aa14b386)](https://app.codacy.com/gh/kpavlov/finchly/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_grade)
[![Codacy Covarage Badge](https://app.codacy.com/project/badge/Coverage/3aa0b5847e70494d9795ff98aa14b386)](https://app.codacy.com/gh/kpavlov/finchly/dashboard?utm_source=gh&utm_medium=referral&utm_content=&utm_campaign=Badge_coverage)

_Elegant utilities for streamlining integration testing in Kotlin_

<img src="docs/finchly-logo.webp" alt="Logo" height="200" width="200">

## Overview

Finchly is a modern Kotlin library that simplifies integration testing by providing a collection of utilities and helpers. It focuses on making your tests more readable, maintainable, and efficient.


## Features

- 🚀 Simple and intuitive API
- 👍 Kotlin-first design
- 🎪[TestEnvironment](docs/TestEnvironment.md) provides an easy way to manage environment variables between system and `.env` files with fallback values for test environment configurations.
- 🎭[BaseWiremock](docs/Wiremock.md) provides a Kotlin wrapper for WireMock server with built-in configuration, lifecycle management, and verification for simplified HTTP service mocking in tests.

## How to build

Building project locally:
```shell
mvn clean verify
```
or using Make
```shell
make build
```

## Contributing
We welcome contributions! Please see the [Contributing Guidelines](CONTRIBUTING.md) for details.

