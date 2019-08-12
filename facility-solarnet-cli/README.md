# SolarNetwork DER Challenge: Facility SolarNetwork

This repository contains a proof-of-concept Protobuf/gRPC based server implementation of the
`DerFacility` defined in [der_facility_service][der_facility_service] that integrates with
SolarNetwork for operational management.

## Building

Gradle is used for building. Run the `build` task via `gradlew`:

	$ ../gradlew build

This will generate a `build/libs/esi-solarnet-facility-cli-X.jar` where `X` is a version number.



[der_facility_service]: ../api/src/main/proto/solarnetwork/esi/service/der_facility_service.proto
[sim-intro]: ../README.md#simulation-example-register-a-facility-with-an-exchange
