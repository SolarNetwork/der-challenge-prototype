# SolarNetwork DER Challenge: API

This repository contains a Protobuf/gRPC based API definition for the concept of an energy services
interface (ESI). The [service Protobuf definitions][proto-service] are the main entry points to the
API. The [domain Protobuf definitions][proto-domain] model all the domain objects used in the API.

If you want to explore this API, an easy to navigate HTML document is available [here][html-api].

## Building

Gradle is used for building. Run the `build` task via `gradlew`:

	$ ../gradlew build

This will generate a `build/libs/esi-api-X.jar` where `X` is a version number.

[proto-domain]: src/main/proto/solarnetwork/esi/domain
[proto-service]: src/main/proto/solarnetwork/esi/service
[html-api]: https://data.solarnetwork.net/dev/der-challenge/doc/api/
