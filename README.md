# VertxNettyUDP6Tester

Update
-------
Thanks to the folks at Netty this issue seems to be resolved with the latest 4.1.74.Final release. And Vert.x folks just updated to 4.1.25 which includes the Netty fix and the code now works properly as expected.

Original issue link: https://github.com/netty/netty/issues/11563

Issue Description
-----------------
Test of UDP sending on MacOS to demonstrate possible issue on MacOS

Tester class to show a possible issue with Netty/Vertx and sending IP6
UDP traffic on MacOS.
  
This code was thrown together and is not optimal but it works on
Linux and Windows.

I have code that works on MacOS using standard Java NIO classes which makes me think it might be a Vert.x/Netty issue.
    
This code simulates a device discovery mechanism. The Device class will
listen to the MULTICAST_GROUP for a discovery message and then will
read the port it needs to send a UDP response back to.

No matter if I use Wireless or Hardwired on MacOS I can't get the code to 
run successfully like it does on the other systems.

**Note**: I disabled the Netty logging in `logback.xml` you can turn it on by modifying the file and rebuilding the project. I wanted clean outputs for the bug report.

**Note #2**: I've included a pure Java `java.net.*` version of the application as well to demonstrate that using standard Java networking works properly.


It has the same parameters as the `Tester` class and you can run it like: 
```
java -Dnet.interface=eth0 -cp VertxNettyIP6UdpTester-1.0-SNAPSHOT-fat.jar com.mycompany.vertxnettyip6udptester.RegularJavaUDPTester 
```

## Code Setup

The code has some initial defaults that I setup, defaults are in `()'s`:

* OP_MODE - Which service are we running (Device, Sender, or Both) `(Both)`
* MODE - Which mode are we in `IPv4` or `IPv6` (`IPv6`)
* INTERFACE - Which interface to use ie. eth0, en0, etc. (`en0`)
* LISTEN_PORT - Which port to listen on (`35056`)
* MULTICAST_GROUP - The IPv4 or IPv6 Multicast Group (`FF02::1`)
* LISTEN_ALL_INTERFACES - Listen address for all interfaces (`::`)

You can override the options via the following env variables:
* net.ip_mode - must be "IPv4" or "IPv6"..defaults to `IPv6`
* net.interface - defaults to `en0`
* net.listen_port - defaults to `35056`
* net.service_mode - must be "Sender" or "Device" or "Both"..defaults to `Both` 

You'll most likely want to override the `net.interface` to match your system.

You can also use the `service_mode` to only run either the sender or device side of the code.

This is useful if you want to run either the sender or device on different machines to test network.

Running the Sender on my Mac and the Device on a RaspberryPi made no difference. The Mac still error when
trying to send the UDP packets.

## Networking setup
My system is a 2017 15" Macbook Pro running *Big Sur (11.5)* and I primarily use Wireless networking at home. 

I also have 2 VirtualBox VMs (*Fedora 33* and *Windows 10*) that I tested on and both were setup to use *Bridged Mode* so they get their own IP addresses.

I also tested on a hardwired Linux machine at work and that works fine.

All three systems have `JDK11` running on them:

```
openjdk version "11.0.11" 2021-04-20
OpenJDK Runtime Environment AdoptOpenJDK-11.0.11+9 (build 11.0.11+9)
OpenJDK 64-Bit Server VM AdoptOpenJDK-11.0.11+9 (build 11.0.11+9, mixed mode)
```

- MacOS Network Settings
  * Wireless
```
en0: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500
	options=400<CHANNEL_IO>
	ether 8c:85:90:18:0c:87
	inet6 fe80::16:a125:3493:8186%en0 prefixlen 64 secured scopeid 0x5
	inet 192.168.1.151 netmask 0xffffff00 broadcast 192.168.1.255
	nd6 options=201<PERFORMNUD,DAD>
	media: autoselect
	status: active
```
  * Wired
  ```
  en7: flags=8863<UP,BROADCAST,SMART,RUNNING,SIMPLEX,MULTICAST> mtu 1500
	options=6467<RXCSUM,TXCSUM,VLAN_MTU,TSO4,TSO6,CHANNEL_IO,PARTIAL_CSUM,ZEROINVERT_CSUM>
	ether 00:05:1b:ca:43:ed
	inet6 fe80::1c7f:eaa6:662f:b8d9%en7 prefixlen 64 secured scopeid 0xa
	inet 192.168.1.175 netmask 0xffffff00 broadcast 192.168.1.255
	nd6 options=201<PERFORMNUD,DAD>
	media: autoselect (1000baseT <full-duplex>)
	status: active 
  ```

- Linux VM Network Settings
```
enp0s3: flags=4163<UP,BROADCAST,RUNNING,MULTICAST>  mtu 1500
        inet 192.168.1.236  netmask 255.255.255.0  broadcast 192.168.1.255
        inet6 fe80::5678:74b1:f6ec:c5fb  prefixlen 64  scopeid 0x20<link>
        ether 08:00:27:b2:68:ea  txqueuelen 1000  (Ethernet)
        RX packets 18828  bytes 6030052 (5.7 MiB)
        RX errors 0  dropped 2  overruns 0  frame 0
        TX packets 4438  bytes 425983 (415.9 KiB)
        TX errors 0  dropped 0 overruns 0  carrier 0  collisions 0
```

- Windows VM Network Settings
```
Ethernet adapter Ethernet:

   Connection-specific DNS Suffix  . : fios-router.home
   Link-local IPv6 Address . . . . . : fe80::5ca5:ac05:8242:b3e9%15
   IPv4 Address. . . . . . . . . . . : 192.168.1.223
   Subnet Mask . . . . . . . . . . . : 255.255.255.0
   Default Gateway . . . . . . . . . : 172.20.0.1
                                       192.168.1.1
```
 
### Run the code
1. `mvn clean package`
2. `mvn -Dnet.interface=<Your main Interface> exec:java`

## Output
When you run the code you should see the following results

##### Linux - Not sure why I see duplicate receive messages on `Device`
```
111:56:22.095 INFO  c.m.vertxnettyip6udptester.Tester - *** Starting Variables ***
11:56:22.107 INFO  c.m.vertxnettyip6udptester.Tester - Network Mode = IPv6
11:56:22.108 INFO  c.m.vertxnettyip6udptester.Tester - Network Interface = enp0s3
11:56:22.108 INFO  c.m.vertxnettyip6udptester.Tester - Network Listen All Interface = ::
11:56:22.108 INFO  c.m.vertxnettyip6udptester.Tester - Network Multicast Group = FF02::1
11:56:22.108 INFO  c.m.vertxnettyip6udptester.Tester - Network Port = 35056
11:56:22.108 INFO  c.m.vertxnettyip6udptester.Tester - **************************
11:56:22.776 INFO  c.m.v.Tester$Sender - Listening for Unicast message on IPv6 @ 0:0:0:0:0:0:0:0:50774
11:56:22.792 INFO  c.m.v.Tester$Device - Listening for Multicast Messages on IPv6 Group [FF02::1] on local address (enp0s3): 0:0:0:0:0:0:0:0:35056
11:56:22.825 INFO  c.m.v.Tester$Sender - Starting to send Discovery Requests every 5 seconds
11:56:27.840 INFO  c.m.v.Tester$Sender - Sending request...
11:56:27.891 INFO  c.m.v.Tester$Sender - Request sent successfully
11:56:27.898 INFO  c.m.v.Tester$Device - Received Discovery Packet from fe80:0:0:0:5678:74b1:f6ec:c5fb%2:60763 with data {"action":"REQUEST","responsePort":50774}
11:56:27.958 INFO  c.m.v.Tester$Device - Sending response back to fe80:0:0:0:5678:74b1:f6ec:c5fb%2:50774 with data - {"id":"0c9e1fce-f416-4e27-a587-de83e8043493","date":"2021-08-06T11:56:27.931603"}
11:56:27.960 INFO  c.m.v.Tester$Sender - Got a discovery response message from fe80:0:0:0:5678:74b1:f6ec:c5fb%2:35056 with data - {"id":"0c9e1fce-f416-4e27-a587-de83e8043493","date":"2021-08-06T11:56:27.931603"}
11:56:27.960 INFO  c.m.v.Tester$Device - Response Sent ok
11:56:27.969 INFO  c.m.v.Tester$Device - Received Discovery Packet from fe80:0:0:0:5678:74b1:f6ec:c5fb%2:60763 with data {"action":"REQUEST","responsePort":50774}
11:56:27.970 INFO  c.m.v.Tester$Device - Sending response back to fe80:0:0:0:5678:74b1:f6ec:c5fb%2:50774 with data - {"id":"0c9e1fce-f416-4e27-a587-de83e8043493","date":"2021-08-06T11:56:27.969225"}
11:56:27.973 INFO  c.m.v.Tester$Sender - Got a discovery response message from fe80:0:0:0:5678:74b1:f6ec:c5fb%2:35056 with data - {"id":"0c9e1fce-f416-4e27-a587-de83e8043493","date":"2021-08-06T11:56:27.969225"}
11:56:27.974 INFO  c.m.v.Tester$Device - Response Sent ok

```
##### Windows - Not sure why I see duplicate receive messages on `Device`

```
11:48:32.089 INFO  c.m.vertxnettyip6udptester.Tester - *** Starting ***
11:48:32.089 INFO  c.m.vertxnettyip6udptester.Tester - Network Mode = IPv6
11:48:32.105 INFO  c.m.vertxnettyip6udptester.Tester - Network Interface = eth5
11:48:32.105 INFO  c.m.vertxnettyip6udptester.Tester - Network Listen All Interface = ::
11:48:32.105 INFO  c.m.vertxnettyip6udptester.Tester - Network Multicast Group = FF02::1
11:48:32.105 INFO  c.m.vertxnettyip6udptester.Tester - Network Port = 35056
11:48:32.745 INFO  c.m.v.Tester$Sender - Listening for Unicast message on IPv6 @ 0:0:0:0:0:0:0:0:55440
11:48:32.792 INFO  c.m.v.Tester$Sender - Starting to send Discovery Requests every 5 seconds
11:48:32.823 INFO  c.m.v.Tester$Device - Listening for Multicast Messages on IPv6 Group [FF02::1] on local address (eth5): 0:0:0:0:0:0:0:0:35056
11:48:37.809 INFO  c.m.v.Tester$Sender - Sending request...
11:48:37.824 INFO  c.m.v.Tester$Sender - Request sent successfully
11:48:37.824 INFO  c.m.v.Tester$Device - Received Discovery Packet from fe80:0:0:0:5ca5:ac05:8242:b3e9%15:55441 with data {"action":"REQUEST","responsePort":55440}
11:48:37.870 INFO  c.m.v.Tester$Device - Sending response back to fe80:0:0:0:5ca5:ac05:8242:b3e9%15:55440 with data - {"id":"a57441db-3f06-4fc1-9cfa-90334a008102","date":"2021-08-06T11:48:37.855100300"}
11:48:37.870 INFO  c.m.v.Tester$Sender - Got a discovery response message from fe80:0:0:0:5ca5:ac05:8242:b3e9%15:35056 with data - {"id":"a57441db-3f06-4fc1-9cfa-90334a008102","date":"2021-08-06T11:48:37.855100300"}
11:48:37.870 INFO  c.m.v.Tester$Device - Response Sent ok
11:48:37.870 INFO  c.m.v.Tester$Device - Received Discovery Packet from fe80:0:0:0:5ca5:ac05:8242:b3e9%15:55441 with data {"action":"REQUEST","responsePort":55440}
11:48:37.886 INFO  c.m.v.Tester$Device - Sending response back to fe80:0:0:0:5ca5:ac05:8242:b3e9%15:55440 with data - {"id":"a57441db-3f06-4fc1-9cfa-90334a008102","date":"2021-08-06T11:48:37.886362900"}
11:48:37.886 INFO  c.m.v.Tester$Sender - Got a discovery response message from fe80:0:0:0:5ca5:ac05:8242:b3e9%15:35056 with data - {"id":"a57441db-3f06-4fc1-9cfa-90334a008102","date":"2021-08-06T11:48:37.886362900"}
11:48:37.886 INFO  c.m.v.Tester$Device - Response Sent ok
```
##### MacOS (IPv6 Mode)
```
12:28:05.209 INFO  c.m.vertxnettyip6udptester.Tester - *** Starting Variables ***                                                                                                                                                                                                                                         
12:28:05.211 INFO  c.m.vertxnettyip6udptester.Tester - Network Mode = IPv6                                                                                                                                                                                                                                                
12:28:05.213 INFO  c.m.vertxnettyip6udptester.Tester - Network Interface = en0                                                                                                                                                                                                                                            
12:28:05.213 INFO  c.m.vertxnettyip6udptester.Tester - Netork Listen All Interface = ::                                                                                                                                                                                                                                   
12:28:05.213 INFO  c.m.vertxnettyip6udptester.Tester - Network Multicast Group = FF02::1                                                                                                                                                                                                                                  
12:28:05.213 INFO  c.m.vertxnettyip6udptester.Tester - Network Port = 35056                                                                                                                                                                                                                                               
12:28:05.213 INFO  c.m.vertxnettyip6udptester.Tester - **************************                                                                                                                                                                                                                                         
12:28:05.646 INFO  c.m.v.Tester$Sender - Listening for Unicast message on IPv6 @ 0:0:0:0:0:0:0:0:61812                                                                                                                                                                                                                    
12:28:05.649 INFO  c.m.v.Tester$Device - Listening for Multicast Messages on IPv6 Group [FF02::1] on local address (en0): 0:0:0:0:0:0:0:0:35056                                                                                                                                                                           
12:28:05.698 INFO  c.m.v.Tester$Sender - Starting to send Discovery Requests every 5 seconds                                                                                                                                                                                                                              
12:28:10.702 INFO  c.m.v.Tester$Sender - Sending request...                                                                                                                                                                                                                                                               
java.net.NoRouteToHostException: No route to host                                                                                                                                                                                                                                                                         
        at java.base/sun.nio.ch.DatagramChannelImpl.send0(Native Method)                                                                                                                                                                                                                                                  
        at java.base/sun.nio.ch.DatagramChannelImpl.sendFromNativeBuffer(DatagramChannelImpl.java:584)                                                                                                                                                                                                                    
        at java.base/sun.nio.ch.DatagramChannelImpl.send(DatagramChannelImpl.java:546)                                                                                                                                                                                                                                    
        at java.base/sun.nio.ch.DatagramChannelImpl.send(DatagramChannelImpl.java:529)                                                                                                                                                                                                                                    
        at io.netty.channel.socket.nio.NioDatagramChannel.doWriteMessage(NioDatagramChannel.java:296)                                                                                                                                                                                                                     
        at io.netty.channel.nio.AbstractNioMessageChannel.doWrite(AbstractNioMessageChannel.java:143)                                                                                                                                                                                                                     
        at io.netty.channel.AbstractChannel$AbstractUnsafe.flush0(AbstractChannel.java:953)                                                                                                                                                                                                                               
        at io.netty.channel.nio.AbstractNioChannel$AbstractNioUnsafe.flush0(AbstractNioChannel.java:354)                                                                                                                                                                                                                  
        at io.netty.channel.AbstractChannel$AbstractUnsafe.flush(AbstractChannel.java:917)                                                                                                                                                                                                                                
        at io.netty.channel.DefaultChannelPipeline$HeadContext.flush(DefaultChannelPipeline.java:1372)                                                                                                                                                                                                                    
        at io.netty.channel.AbstractChannelHandlerContext.invokeFlush0(AbstractChannelHandlerContext.java:750)                                                                                                                                                                                                            
        at io.netty.channel.AbstractChannelHandlerContext.invokeWriteAndFlush(AbstractChannelHandlerContext.java:765)
        at io.netty.channel.AbstractChannelHandlerContext.write(AbstractChannelHandlerContext.java:790)
        at io.netty.channel.AbstractChannelHandlerContext.writeAndFlush(AbstractChannelHandlerContext.java:758)
        at io.netty.channel.AbstractChannelHandlerContext.writeAndFlush(AbstractChannelHandlerContext.java:808)
        at io.netty.channel.DefaultChannelPipeline.writeAndFlush(DefaultChannelPipeline.java:1025)
        at io.netty.channel.AbstractChannel.writeAndFlush(AbstractChannel.java:306)
        at io.vertx.core.datagram.impl.DatagramSocketImpl.lambda$send$3(DatagramSocketImpl.java:348)
        at io.netty.util.concurrent.DefaultPromise.notifyListener0(DefaultPromise.java:578)
        at io.netty.util.concurrent.DefaultPromise.notifyListenersNow(DefaultPromise.java:552)
        at io.netty.util.concurrent.DefaultPromise.notifyListeners(DefaultPromise.java:491)
        at io.netty.util.concurrent.DefaultPromise.addListener(DefaultPromise.java:184)
        at io.netty.util.concurrent.DefaultPromise.addListener(DefaultPromise.java:35)
        at io.vertx.core.datagram.impl.DatagramSocketImpl.send(DatagramSocketImpl.java:346)
        at com.mycompany.vertxnettyip6udptester.Tester$Sender.lambda$start$1(Tester.java:149)
        at io.vertx.core.impl.VertxImpl$InternalTimerHandler.handle(VertxImpl.java:946)
        at io.vertx.core.impl.VertxImpl$InternalTimerHandler.handle(VertxImpl.java:917)
        at io.vertx.core.impl.EventLoopContext.emit(EventLoopContext.java:49)
        at io.vertx.core.impl.ContextImpl.emit(ContextImpl.java:275)
        at io.vertx.core.impl.EventLoopContext.emit(EventLoopContext.java:22)
        at io.vertx.core.impl.AbstractContext.emit(AbstractContext.java:49)
        at io.vertx.core.impl.EventLoopContext.emit(EventLoopContext.java:22)
        at io.vertx.core.impl.VertxImpl$InternalTimerHandler.run(VertxImpl.java:940)
        at io.netty.util.concurrent.PromiseTask.runTask(PromiseTask.java:98)
        at io.netty.util.concurrent.ScheduledFutureTask.run(ScheduledFutureTask.java:176)
        at io.netty.util.concurrent.AbstractEventExecutor.safeExecute(AbstractEventExecutor.java:164)
        at io.netty.util.concurrent.SingleThreadEventExecutor.runAllTasks(SingleThreadEventExecutor.java:472)
        at io.netty.channel.nio.NioEventLoop.run(NioEventLoop.java:500)
        at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:989)
        at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74)
        at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30)
        at java.base/java.lang.Thread.run(Thread.java:829)

```
##### MacOS (IPv4 Mode)
```
13:09:58.470 INFO  c.m.vertxnettyip6udptester.Tester - *** Starting Variables ***                                                                                                                                                                                                                                         
13:09:58.472 INFO  c.m.vertxnettyip6udptester.Tester - Network Mode = IPv4                                                                                                                                                                                                                                                
13:09:58.474 INFO  c.m.vertxnettyip6udptester.Tester - Network Interface = en0                                                                                                                                                                                                                                            
13:09:58.474 INFO  c.m.vertxnettyip6udptester.Tester - Netork Listen All Interface = 0.0.0.0                                                                                                                                                                                                                              
13:09:58.474 INFO  c.m.vertxnettyip6udptester.Tester - Network Multicast Group = 224.0.0.224                                                                                                                                                                                                                              
13:09:58.474 INFO  c.m.vertxnettyip6udptester.Tester - Network Port = 35056                                                                                                                                                                                                                                               
13:09:58.474 INFO  c.m.vertxnettyip6udptester.Tester - **************************                                                                                                                                                                                                                                         
13:09:58.980 INFO  c.m.v.Tester$Sender - Listening for Unicast message on IPv4 @ 0.0.0.0:61683                                                                                                                                                                                                                            
13:09:58.983 INFO  c.m.v.Tester$Device - Listening for Multicast Messages on IPv4 Group [224.0.0.224] on local address (en0): 0.0.0.0:35056                                                                                                                                                                               
13:09:59.040 INFO  c.m.v.Tester$Sender - Starting to send Discovery Requests every 5 seconds                                                                                                                                                                                                                              
13:10:04.047 INFO  c.m.v.Tester$Sender - Sending request...                                                                                                                                                                                                                                                               
13:10:04.083 INFO  c.m.v.Tester$Sender - Request sent successfully                                                                                                                                                                                                                                                        
13:10:04.108 INFO  c.m.v.Tester$Device - Received Discovery Packet from 192.168.1.151:61500 with data {"action":"REQUEST","responsePort":61683}                                                                                                                                                                           
13:10:04.141 INFO  c.m.v.Tester$Device - Sending response back to 192.168.1.151:61683 with data - {"id":"0486ecd0-08e5-492b-8dc3-b6e9d55e2a30","date":"2021-08-06T13:10:04.122957"}                                                                                                                                       
13:10:04.142 INFO  c.m.v.Tester$Sender - Got a discovery response message from 192.168.1.151:35056 with data - {"id":"0486ecd0-08e5-492b-8dc3-b6e9d55e2a30","date":"2021-08-06T13:10:04.122957"}                                                                                                                          
13:10:04.142 INFO  c.m.v.Tester$Device - Response Sent ok         
```
