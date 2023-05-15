# Conflict-free replicated data types -- counters

This repository contains implementation of simplest CRDTs -- growing counter and positive-negative
counter. Those are delta implementations, meaning that a replica, while being state-based, transfer
only its modified part to the other replicas.

For testing purposes, simple thread-safe wrapper is implemented that simulates network interaction
between replicas. Tests include base functional tests and tests for concurrency.

## Usage

To run tests, run the following command.

```bash
./gradlew test
```