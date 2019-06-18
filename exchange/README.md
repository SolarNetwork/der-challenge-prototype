# SolarNetwork DER Challenge: Operator Service

This repository contains a proof-of-concept Protobuf/gRPC based server implementation of the
`DerFacilityExchange` defined in [der_facility_exchange][der_facility_exchange].

## Building

Gradle is used for building. Run the `build` task via `gradlew`:

	$ ../gradlew build

This will generate a `build/libs/esi-simple-facility-exchange-X.jar` where `X` is a version number.

## Running

TODO

[der_facility_exchange]: ../api/src/main/proto/solarnetwork/esi/service/der_facility_exchange.proto
