# SolarNetwork DER Challenge: Facility SolarNetwork

This repository contains a proof-of-concept Protobuf/gRPC based server implementation of the
`DerFacility` defined in [der_facility_service][der_facility_service] that integrates with
SolarNetwork for operational management.

## Building

Gradle is used for building. Run the `build` task via `gradlew`:

	$ ../gradlew build

This will generate a `build/libs/esi-solarnet-facility-cli-X.jar` where `X` is a version number.


## Integration with SolarNetwork

This application relies on ESI metadata published by the [SolarNode ESI Facility Integration][esi-sn-control]
plugin. Once that has been deployed and configured, then run this application with a user-level
SolarNetwork security token by supplying these runtime properties:

| Property | Description |
|:---------|:------------|
| `esi.facility.solarnetwork.tokenId` | The user-level security token ID. |
| `esi.facility.solarnetwork.tokenSecret` | The security token's secret value. |
| `esi.facility.solarnetwork.url` | **Optional:** the SolarNetwork base URL to use. Defaults to `https://data.solarnetwork.net`. |

For example:

```sh
java -Desi.facility.solarnetwork.tokenId=SlKJD93jLDKjD \
     -Desi.facility.solarnetwork.tokenSecret=jlksjd9083hj3lkwej039 \
     -jar build/libs/esi-solarnet-facility-cli-0.1.0.jar
```

# Facility CLI via SSH

The facility provides a CLI application you can access via SSH. By default it listens on port
**2224**. To connect, use `ssh` like this:

```shell
ssh user@localhost -p 2224
Password authentication
Password: 
 :: ESI Facility SolarNetwork CLI :: (0.1.0)


Please type `help` to see available commands
SNFac>
```

The password will have been printed on the exchange's console when it started up, like this:

```
--- Generating password for ssh connection: e73d4bfb-d89f-4873-b10c-d87a08998702
```


[der_facility_service]: ../api/src/main/proto/solarnetwork/esi/service/der_facility_service.proto
[esi-sn-control]: https://github.com/SolarNetwork/solarnetwork-node/tree/develop/net.solarnetwork.node.control.esi
[sim-intro]: ../README.md#simulation-example-register-a-facility-with-an-exchange
