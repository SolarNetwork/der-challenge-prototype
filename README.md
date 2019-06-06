# SolarNetwork DER Challenge Prototypes

This repository contains prototypes for [SEPA DER Challenge][der-challenge] components. This work
has funding support from the [Pacific Northwest National Laboratory][pnnl] and [Ecogy
Energy][ecogy].

This top-level directory serves as an umbrella project for various sub-projects:

 * [api](api/) - API definitions

## Building

Gradle is used for building. Run the `build` task via `gradlew`:

	$ ./gradlew build

Each sub-project will produce its own build artifact(s).

[der-challenge]: http://www.plugandplayder.org/
[pnnl]: https://www.pnnl.gov/
[ecogy]: https://ecogyenergy.com/
