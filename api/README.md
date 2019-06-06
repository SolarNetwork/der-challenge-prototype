# SolarNetwork DER Challenge -- API

This repository contains a Protobuf/gRPC based API definition for the concept of an energy services
interface (ESI). The [Protobuf definitions][protos] are the starting point for this project, and
the [esi.proto][esi-proto] definition is the main service-level entry point.

## Building

Gradle is used for building. Run the `build` task via `gradlew`:

	$ ../gradlew build

This will generate a `build/libs/esi-api-X.jar` where `X` is a version number.

[protos]: src/main/proto
[esi-proto]: src/main/proto/esi.proto
