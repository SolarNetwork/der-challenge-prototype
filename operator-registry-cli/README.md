# SolarNetwork DER Challenge: Operator Registry CLI

This repository contains a proof-of-concept interactive command line application using a 
Protobuf/gRPC based client implementation of the `DerOperatorRegistryService` defined in 
[der_operator_registry_service][der_operator_registry].

![CLI App Demo](docs/opreg-cli.gif)

## Building

Gradle is used for building. Run the `build` task via `gradlew`:

	$ ../gradlew build

This will generate a `build/libs/esi-cli-operator-registry-X.jar` where `X` is a version number.

## Running

Run via `java -jar esi-cli-operator-registry-X.jar`. The following command line arguments are
supported:

| Argument | Default | Description |
|:---------|:--------|:------------|
| `--help` | | Show command-line argument help. |
| `--no-ssl` | | Do not use SSL. |
| `--uri=uri` | localhost:9090 | The gRPC compliant URI for the ESI Operator Registry to use. |

For example:

```sh
java -jar build/libs/esi-cli-operator-registry-0.1.0.jar --no-ssl --uri=esi.example.com:9090
```

Once started, an `OpReg>` prompt will be shown. The following commands are supported:

| Command | Description |
|:--------|:------------|
| `help`  | Show command help. |
| `list`  | List system operators in the registry. |
| `quit`  | Exit the program. |

For example:

```
OpReg> list
Result 1
  Name       Monopoly Utility
  ID         monopoly-utility
  URI        dns:///monopoly.example.com
Result 2
  Name       Earth Utility
  ID         earth-utility
  URI        dns:///earth.localhost
Result 3
  Name       My ESI
  ID         my-esi
  URI        dns:///localhost:9090
```

[der_operator_registry]: ../api/src/main/proto/solarnetwork/esi/service/der_operator_registry_service.proto
