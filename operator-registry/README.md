# SolarNetwork DER Challenge: Operator Registry

This repository contains a proof-of-concept Protobuf/gRPC based server implementation of the
`DerOperatorRegistryService` defined in [der_operator_registry_service][der_operator_registry].

## Building

Gradle is used for building. Run the `build` task via `gradlew`:

	$ ../gradlew build

This will generate a `build/libs/esi-api-X.jar` where `X` is a version number.

## Running

TODO

[der_operator_registry]: ../api/src/main/proto/solarnetwork/esi/service/der_operator_registry_service.proto
