# Contract Proxy Example Project

This folder contains two custom systems, the [Contract Initiator](contract-initiator) and the [Contract Reactor](contract-reactor).
These two systems, conceptually owned and controlled by two different parties, help illustrate how an Arrowhead Contract Proxy system could be used to assist two parties in offering and entering into agreements that are legally binding, given that the used certificates, private keys and signed documents are handled according to whatever criteria are set by any legal institutions of relevance.
When running the demo, as instructed below, the [Contract Initiator](contract-initiator) sends a signed offer to enter into a contract to buy a certain number of units, as outlined by the contract template [simple-purchase.txt](configuration/contract-templates/simple-purchase.txt) to the [Contract Reactor](contract-reactor), which evaluates the offer and then sends a signed acceptance back to the initiator. 
Please refer to their source code for more details about how to use the Contract Proxy system.

## Building and Running

This example project is most conveniently executed by running the [`run.sh`](run.sh) script available in this folder using a unix-compatible shell, such as those provided natively by Linux or macOS, or via MinGW or Cygwin on Windows.
For the shell script to work, Docker, Docker Compose and Maven must be installed.
The script uses Docker to launch a network consisting of

1) a Service Registry,
2) an Orchestrator system,
3) an Authorization system,
4) an Event Handler system,
5) the Contract Initiator system,
6) a Contract Proxy system for the Initiator system,
7) the Contract Reactor system,
8) a Contract Proxy system for the Reactor system,
9) a MySQL database container, and
10) a configuration system, which sets up systems 5 to 9 with the help of systems 1 to 3.

The topology created is as follows:

```
                    +--------------+
                    | Configurator |
                    +-------+------+
                            |
          +-----------------+-----------------+
          |                 |                 |
          V                 V                 V
+------------------+ +---------------+ +--------------+ +---------------+ +-------+
| Service Registry | | Authorization | | Orchestrator | | Event Handler | | MySQL |
+------------------+ +---------------+ +--------------+ +---------------+ +-------+
   A   A     A   A                       A   A   A   A    A   A   A   A
   |   |     |   |                       |   |   |   |    |   |   |   |
   |   +--+--C---C-----------------------C---+---C---C----C---+   |   |
   |      |  |   |                       |       |   |    |       |   |
   |      |  +---C-----------------------C-------+---C----C-----+-+   |
   |      |      |                       |           |    |     |     |
   +------C------C-------+---------------+-----------C----+     |     |
          |      |       |                           |          |     |
          |      +-------C------------------------+--+----------C-----+
          |              |                        |             |
+---------+----------+   |                        |   +---------+--------+
| Contract Initiator |   |                        |   | Contract Reactor |
+--------------------+   |                        |   +------------------+
          A              |                        |             A
          |     +--------+-------+       +--------+-------+     |
          +-----+ Contract Proxy |<----->| Contract Proxy +-----+
                +----------------+       +----------------+
```
Arrows denote service consumption, with the system pointed at has at least one service consumed by the system from which the arrow extends.
While no arrows connect the SR, AU, OR, EH systems and the MySQL instance, there are connections between these systems that have been left implicit.

In this particular scenario, the contract initiator and reactor are both members of the same local cloud.
In a more realistic use case, these systems would be located in different local clouds connected by Gatekeeper and Gateway systems that use a relay to coordinate.
To reduce the complexity and RAM utilization of this demo, however, only a single local cloud is used.

Running the demo takes a few minutes and produces a few hundred lines of log output.
Something akin to the following being present towards the end of the log indicates that the demo was successful:

```
contract-initiator.cp | INFO ========== DEMO STARTING ==========
contract-initiator.cp | INFO == DEMO == Sending offer via "contract-proxy-initiator" and "contract-proxy-reactor" to "reactor" ...
contract-initiator.cp | INFO == DEMO == Offer contains the following contract: TrustedContract{templateName='simple-purchase.txt', arguments={Currency=EUR, Price=1290, PaymentDate=2020-08-02T09:12:38.285498Z, Seller=Reactor System, Quantity=200, Buyer=Initiator System, ArticleNumber=XYZ-123}}
contract-reactor.cp   | INFO == DEMO == Accepted TrustedContractOffer{offerorName='Initiator System', receiverName='Reactor System', validAfter=2020-06-03T09:12:38.298371Z, validUntil=2020-06-03T11:12:38.298371Z, contracts=[TrustedContract{templateName='simple-purchase.txt', arguments={Price=1290, PaymentDate=2020-08-02T09:12:38.285498Z, ArticleNumber=XYZ-123, Currency=EUR, Quantity=200, Buyer=Initiator System, Seller=Reactor System}}], offeredAt=2020-06-03T09:12:38.298371Z}
contract-initiator.cp | INFO == DEMO == Offer accepted by counter-party TrustedContractNegotiation{id=2067101290543595625, offer=TrustedContractOffer{offerorName='Initiator System', receiverName='Reactor System', validAfter=2020-06-03T09:12:38.298371Z, validUntil=2020-06-03T11:12:38.298371Z, contracts=[TrustedContract{templateName='simple-purchase.txt', arguments={Price=1290, PaymentDate=2020-08-02T09:12:38.285498Z, ArticleNumber=XYZ-123, Currency=EUR, Quantity=200, Buyer=Initiator System, Seller=Reactor System}}], offeredAt=2020-06-03T09:12:38.298371Z}, status=ACCEPTED}
```

The demo cluster will not shut down by itself after the demo is over, which means that you need to shut it down manually.