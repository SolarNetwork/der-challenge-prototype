# SolarNetwork DER Challenge Prototypes

This repository contains prototypes for [SEPA DER Challenge][der-challenge] components. This work
has funding support from the [Pacific Northwest National Laboratory][pnnl] and 
[Ecogy Energy][ecogy].

This top-level directory serves as an umbrella project for various sub-projects:

 * [api](api/) - API definitions
 * [common](common/) - Common/shared implementation resources for other projects
 * [common-cli](common-cli/) - Common/shared implementation resources for other CLI projects
 * [exchange](exchange/) - Facility Exchange server proof-of-concept
 * [exchange-registry](exchange-registry/) - Facility Exchange Registry server proof-of-concept
 * [exchange-registry-cli](exchange-registry-cli/) - Facility Exchange Registry client proof-of-concept

## Building

Gradle is used for building. Run the `build` task via `gradlew`:

	$ ./gradlew build

Each sub-project will produce its own build artifact(s).

# Simulation example: register a facility with an exchange

This example shows how to register a simulated facility with a simulated exchange, using the command
line applications included in this repository. You will will be needing to open 4 independent
terminal windows to complete the simulation. Each project will use built-in default values that
are designed to make them work seamlessly together for this demonstration.

![ESI Facility registration simulation](docs/esi-sim-facility-registration.gif)

You'll need a **Java 8 runtime** (or newer) and a **SSH** client to run this simulation. Make sure
all projects are built before continuing. This is as simple as:

```shell
./gradlew build
```

**Note** in the following examples the _version number_ of the JAR file arguments might differ
from what is shown. Please adjust the examples to use whatever version is available.

**Note** that all the following steps assume your terminal session's starting working directory
is _this_ directory.

## Start the Exchange Registry

The first step is to start up an Exchange Registry. This is a gRPC-based service that provides
a telephone book-like service for locating exchanges a facility might integrate with. Start up
the registry like this:

```shell
cd exchange-registry
java -jar build/libs/esi-simple-exchange-registry-0.1.0.jar
```

After a moment, you should see something like the following printed to the screen:

```
 :: ESI Simple Facility Exchange Registry :: (0.1.0)

Loaded 1 DerFacilityExchangeInfo registry entries:

Name                 | UID                  | URI                                     
-------------------- | -------------------- | ----------------------------------------
Monopoly Utility     | monopoly-utility     | dns:///localhost:9091             

gRPC Server started, listening on address: 0.0.0.0, port: 9090
```

Leave this service running, and **continue in a new terminal** for the next step.

## Start the Exchange

An exchange is a gRPC-based service that a facility registers with to participate in grid-based
events. The `exchange` project contains a simulated exchange service. Start up this service now:

```shell
cd exchange
java -jar build/libs/esi-simple-facility-exchange-0.1.0-app.war
```

After a moment, you should see something like the following printed to the screen:

```
 :: ESI Facility Exchange :: (0.1.0)

gRPC Server started, listening on address: 0.0.0.0, port: 9091
```

Leave this service running, and **continue in a new terminal** for the next step.

## Start the Facility

A facility is a gRPC-based service that registers with an exchange to participate in grid-based
events, and manages energy resources to achieve the goals of those events. The `facility-sim-cli`
project contains a simulated facility service. Start up this service now:

```shell
cd facility-sim-cli
java -jar build/libs/esi-simple-facility-simulator-cli-0.1.0.jar
```

After a moment, you should see something like the following printed to the screen:

```
 :: ESI Facility Simulator CLI :: (0.1.0)

--- Generating password for ssh connection: 2707bb1b-d89f-4873-b10c-d87a08998702
Ssh server started [127.0.0.1:2223]

gRPC Server started, listening on address: 0.0.0.0, port: 9092
```

**Note** that SSH connection password. You'll need that value in the next section. Leave this
service running, and **continue in a new terminal** for the next step.

## Register the Facility with the Exchange

The simulated facility is managed via a command line interface accessed via `ssh`. Connect to the
service now. When prompted for a password, enter the one shown to you when you started the facility
service (in the previous section). You should see output like the following:

```shell
ssh user@localhost -p 2223

Password authentication
Password: 
 :: ESI Facility Simulator CLI :: (0.1.0)

Welcome! You need to register this facility with an exchange.
Use the exchange-choose command to get started.

Please type `help` to see available commands
Fac> 
```

Now you're connected to the facility. The `Fac>` prompt shows you that the facility is ready to
process your commands. You'll also notice it suggested you use the **exchange-choose** command
to get started, which is exactly what you should do now. Type in `exchange-choose` followed by
<kbd>Enter</kbd>.

```shell
Fac> exchange-choose
Facility Exchange 1
  Name       Monopoly Utility
  ID         monopoly-utility
  URI        dns:///localhost:9091

Enter the number of the exchange you would you like to use:
```

This command queries the **Exchange Registry** for the exchanges available for the facility to
register with. There is only one to choose from: **Monopoly Utility**. Select that by typing
<kbd>1</kbd><kbd>Enter</kbd>. You'll be asked to confirm your choice, and then the registration
process will start:

```
Enter the number of the exchange you would you like to use:
1

You chose Monopoly Utility @ dns:///localhost:9091, is that correct?
y

Great! You need to register with Monopoly Utility now.

Please fill in the following form to register with Monopoly Utility.
```

What's happening now is that the facility has connected to the Monopoly Utility exchange and 
requested the **facility registration form** that must be filled in and submitted in order to
register with the exchange. What's important to realize here is that the form questions are
provided by the exchange itself. The facility has no prior knowledge of what data must be provided
to the exchange to register with it.

Follow the prompts and fill in the requested data. Each question will be presented with an 
_example value_ that you can copy exactly to ensure the registration goes smoothly. Remember, all
the form data (labels, captions, examples) come from the exchange, and are fictional for this
simulation:

```
To register with Simple Operator Service, you must provide the Utility
Interconnection Customer Identifier, Customer Number, and account holder's
surname, as shown on your most recent account statement. If unsure about
anything, please call 555-123-1234 for assistance.

1) UICI
Your Utility Interconnection Customer Identifier.
e.g. 123-1234-1234
? 
123-1234-1234

2) Customer #
Your Customer Number.
e.g. ABC123456789
? 
ABC123456789

3) Surname
The account holder's surname.
e.g. Smith-Doe
? 
Smith-Doe
```

At this point, the facility will confirm that you are happy with your answers and wish to submit 
the registration information to the exchange. Go ahead and submit it now:

```
Here are your answers, please review them now:

1) UICI: 123-1234-1234
2) Customer #: ABC123456789
3) Surname: Smith-Doe


Would you like to change any answers?
n

Would you like to submit the registration?
y
```

At this point, the facility will submit the registration information to the Monopoly Utility
exchange. The exchange is expected to perform whatever sort of immediate business validation it
needs to on the submitted data, and return an error if something is amiss. Otherwise, the 
exchange accepts the registration and the facility must then wait for the exchange to **complete**
the registration process by calling a gRPC method on the facility. This completion step informs
the facility if the registration was approved or not.

In this simulation, the exchange will almost immediately approve the registration request, and then
automatically invokes the completion method on the facility. A real exchange might need to perform
other steps, that could take hours or days even to complete the request.

The facility will print out a message to the terminal when it receives the completion call from the
exchange. It will look like this:

```
-------------------------------------------------------------------------------
The registration with exchange monopoly-utility @ dns:///localhost:9091 has
succeeded.
-------------------------------------------------------------------------------
```

## Parting thoughts

That completes the facility registration simulation example. If you want to perform the simulation
over again, you must shut down both the exchange and the facility (type <kbd>Ctl-C</kbd> on their
respective terminals) and then start them up again. All persisted state is lost when the 
applications terminate.

[der-challenge]: http://www.plugandplayder.org/
[pnnl]: https://www.pnnl.gov/
[ecogy]: https://ecogyenergy.com/
