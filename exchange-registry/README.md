# SolarNetwork DER Challenge: Operator Registry

This repository contains a proof-of-concept Protobuf/gRPC based server implementation of the
`DerOperatorRegistryService` defined in [der_facility_exchange_registry_service][der_facility_exchange_registry].

## Building

Gradle is used for building. Run the `build` task via `gradlew`:

	$ ../gradlew build

This will generate a `build/libs/esi-simple-exchange-registry-X.jar` where `X` is a version number.

## Running

TODO

[der_facility_exchange_registry]: ../api/src/main/proto/solarnetwork/esi/service/der_facility_exchange_registry.proto
