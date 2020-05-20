# Contract Proxy Example Project

This folder contains two custom systems, the [Contract Initiator](contract-initiator) and the [Contract Reactor](contract-reactor).
These two systems, conceptually owned and controlled by two different parties, help illustrate how an Arrowhead Contract Proxy system could be used to assist two parties in offering and entering into agreements that are legally binding, given that the used certificates, private keys and signed documents are handled according to whatever criteria are set by the relevant legal institutions.

## Building and Running

This example project is most conveniently executed by running the [`run.sh`](run.sh) script available in this folder using a unix-compatible shell, such as those provided natively by Linux or macOS, or via MinGW or Cygwin on Windows.
The shell script uses Docker to launch a network consisting of

1) a Service Registry,
2) an Orchestrator system,
3) an Authorization system,
4) a Contract Proxy system,
5) the Contract Initiator system,
6) the Contract Reactor system and
7) a MySQL database container.

After all systems have started up and registered using the Service Registry, the Contract Initiator sends a couple of contract offers to the Contract Reactor, which will accept only some of them.
Only the output produced by the initiator and reactor systems will be written to console.

Apart from _Docker_, the script also relies on _Maven_ for building all the .jar files required to run the project, as well as on _cURL_ for setting up authorization and orchestration rules once the cloud has started.