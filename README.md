# VertxNettyUDP6Tester
Test of UDP sending on MacOS to demonstrate possible issue on MacOS

Tester class to show a possible issue with Netty/Vertx and sending IP6
UDP traffic on MacOS.
  
This code was thrown together and is not optimal but it works on
Linux and Windows.

I have code that works on MacOS using standard Java NIO classes which makes me think it might be a Netty issue.
    
This code simulates a device discovery mechanism. The Device class will
listen to the MULTICAST_GROUP for a discovery message and then will
read the port it needs to send a UDP response back to.

## Code Setup

The code has some initial defaults that I setup such as:

* INTERFACE - Which interface to use ie. eth0, en0, etc. (`en0`)
* LISTEN_PORT - Which port to listen on (`35056`)
* MULTICAST_GROUP - The IP6 Multicast Group (`FF02::1`)
* LISTEN_ALL_INTERFACES - Listen on all interfaces (`::`)

You can override two options via the following env variables:
* net.interface - defaults to `en0`
* net.listen_port - defaults to `35056`

You'll most likely want to override the `net.interface` to match your system.

## Networking setup
My system is a 2017 15" Macbook Pro running *Big Sur (11.5)* and I primarily use Wireless network at home. 

I also have 2 VirtualBox VMs (*Fedora 33* and *Windows 10*) that I tested on and both were setup to use *Bridged Mode* so they get their own IP addresses.

All three systems have JDK11 running on them:

```
openjdk version "11.0.11" 2021-04-20
OpenJDK Runtime Environment AdoptOpenJDK-11.0.11+9 (build 11.0.11+9)
OpenJDK 64-Bit Server VM AdoptOpenJDK-11.0.11+9 (build 11.0.11+9, mixed mode)
```

 
### Run the code
1. `mvn clean package`
2. `mvn -Dnet.interface=<Your main Interface> exec:java`

## Output
When you run the code you should see the following results

##### Linux
```
10:45:33.994 INFO  c.m.vertxnettyip6udptester.Tester - *** Starting ***
10:45:33.996 INFO  c.m.vertxnettyip6udptester.Tester - Network Interface = enp0s3
10:45:33.997 INFO  c.m.vertxnettyip6udptester.Tester - Network Port = 35056
10:45:34.431 INFO  c.m.v.Tester$Sender - Listening for Unicast message on IPV6 @ 0:0:0:0:0:0:0:0:37830
10:45:34.439 INFO  c.m.v.Tester$Device - Listening for Multicast Messages on IPv6 Group [FF02::1] on local address (enp0s3): 0:0:0:0:0:0:0:0:35056
10:45:34.477 INFO  c.m.v.Tester$Sender - Starting to send Discovery Requests every 5 seconds
10:45:39.484 INFO  c.m.v.Tester$Sender - Sending request...
10:45:39.510 INFO  c.m.v.Tester$Sender - Request sent successfully
10:45:39.551 INFO  c.m.v.Tester$Device - Received Discovery Packet from fe80:0:0:0:5678:74b1:f6ec:c5fb%2:59876 with data {"action":"REQUEST","responsePort":37830}
10:45:39.587 INFO  c.m.v.Tester$Device - Sending response back to fe80:0:0:0:5678:74b1:f6ec:c5fb%2:37830 with data - {"id":"7302a85b-5746-405d-a195-5a44e540157b","date":"2021-08-06T10:45:39.566295"}
10:45:39.589 INFO  c.m.v.Tester$Sender - Got a discovery response message from fe80:0:0:0:5678:74b1:f6ec:c5fb%2:35056 with data - {"id":"7302a85b-5746-405d-a195-5a44e540157b","date":"2021-08-06T10:45:39.566295"}
10:45:39.590 INFO  c.m.v.Tester$Device - Response Sent ok
10:45:39.590 INFO  c.m.v.Tester$Device - Received Discovery Packet from fe80:0:0:0:5678:74b1:f6ec:c5fb%2:59876 with data {"action":"REQUEST","responsePort":37830}
10:45:39.592 INFO  c.m.v.Tester$Device - Sending response back to fe80:0:0:0:5678:74b1:f6ec:c5fb%2:37830 with data - {"id":"7302a85b-5746-405d-a195-5a44e540157b","date":"2021-08-06T10:45:39.591888"}
10:45:39.593 INFO  c.m.v.Tester$Sender - Got a discovery response message from fe80:0:0:0:5678:74b1:f6ec:c5fb%2:35056 with data - {"id":"7302a85b-5746-405d-a195-5a44e540157b","date":"2021-08-06T10:45:39.591888"}
10:45:39.594 INFO  c.m.v.Tester$Device - Response Sent ok
```
##### Windows

```
10:37:41.792 INFO  c.m.vertxnettyip6udptester.Tester - *** Starting ***
10:37:41.808 INFO  c.m.vertxnettyip6udptester.Tester - Network Interface = eth5
10:37:41.808 INFO  c.m.vertxnettyip6udptester.Tester - Network Port = 35056
10:37:42.433 INFO  c.m.v.Tester$Sender - Listening for Unicast message on IPV6 @ 0:0:0:0:0:0:0:0:62354
10:37:42.480 INFO  c.m.v.Tester$Sender - Starting to send Discovery Requests every 5 seconds
10:37:42.480 INFO  c.m.v.Tester$Device - Listening for Multicast Messages on IPv6 Group [FF02::1] on local address (eth5): 0:0:0:0:0:0:0:0:35056
10:37:47.496 INFO  c.m.v.Tester$Sender - Sending request...
10:37:47.527 INFO  c.m.v.Tester$Sender - Request sent successfully
10:37:47.527 INFO  c.m.v.Tester$Device - Received Discovery Packet from fe80:0:0:0:5ca5:ac05:8242:b3e9%15:62355 with data {"action":"REQUEST","responsePort":62354}
10:37:47.589 INFO  c.m.v.Tester$Device - Sending response back to fe80:0:0:0:5ca5:ac05:8242:b3e9%15:62354 with data - {"id":"838ecac4-4c23-401f-92b9-5bd30b73881a","date":"2021-08-06T10:37:47.558356100"}
10:37:47.589 INFO  c.m.v.Tester$Sender - Got a discovery response message from fe80:0:0:0:5ca5:ac05:8242:b3e9%15:35056 with data - {"id":"838ecac4-4c23-401f-92b9-5bd30b73881a","date":"2021-08-06T10:37:47.558356100"}
10:37:47.589 INFO  c.m.v.Tester$Device - Send ok
10:37:47.605 INFO  c.m.v.Tester$Device - Received Discovery Packet from fe80:0:0:0:5ca5:ac05:8242:b3e9%15:62355 with data {"action":"REQUEST","responsePort":62354}
10:37:47.605 INFO  c.m.v.Tester$Device - Sending response back to fe80:0:0:0:5ca5:ac05:8242:b3e9%15:62354 with data - {"id":"838ecac4-4c23-401f-92b9-5bd30b73881a","date":"2021-08-06T10:37:47.605148200"}
10:37:47.605 INFO  c.m.v.Tester$Sender - Got a discovery response message from fe80:0:0:0:5ca5:ac05:8242:b3e9%15:35056 with data - {"id":"838ecac4-4c23-401f-92b9-5bd30b73881a","date":"2021-08-06T10:37:47.605148200"}
10:37:47.605 INFO  c.m.v.Tester$Device - Response Sent ok
```
##### MacOS

