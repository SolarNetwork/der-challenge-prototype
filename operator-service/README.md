# SolarNetwork DER Challenge: Operator Service

This repository contains a proof-of-concept Protobuf/gRPC based server implementation of the
`DerOperatorService` defined in [der_operator_service][der_operator_service].

## Building

Gradle is used for building. Run the `build` task via `gradlew`:

	$ ../gradlew build

This will generate a `build/libs/esi-simple-operator-service-X.jar` where `X` is a version number.

## Running

TODO

[der_operator_service]: ../api/src/main/proto/solarnetwork/esi/service/der_operator_service.proto
