package com.mycompany.vertxnettyip6udptester;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.datagram.DatagramPacket;
import io.vertx.core.datagram.DatagramSocket;
import io.vertx.core.datagram.DatagramSocketOptions;

import io.vertx.core.json.JsonObject;
import java.time.LocalDateTime;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tester class to show a possible issue with Netty/Vertx and sending IP6
 * UDP traffic on MacOS.
 * 
 * It appears that the scope-id is being stripped off during the send 
 * at least that's what the Netty debug output looks like
 * 
 * This code was thrown together and is not optimal but it works on
 * Linux and Windows
 * 
 * I have code that works using standard Java NIO classes which makes me
 * think it might be a Netty issue
 * 
 * 
 * This code simulates a device discovery mechanism. The Device class will
 * listen to the MULTICAST_GROUP for a discovery message and then will
 * read the port it needs to send a UDP response back to.
 * 
 * @author tmulle
 */
public class Tester {
    
    private static final String INTERFACE;
    private static final String MULTICAST_GROUP = "FF02::1";
    private static final int LISTEN_PORT;
    private static final String LISTEN_ALL_INTERFACES = "::";
    
    // Specify these to suit your environment
    static {
        INTERFACE = System.getProperty("net.interface", "en0");
        LISTEN_PORT = Integer.parseInt(System.getProperty("net.listen_port", "35056"));
    }
   
    /**
     * Main
     * 
     * @param args 
     */
    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx();

        // Deploy the receive then the sender
        vertx.deployVerticle(new Device());
        vertx.deployVerticle(new Sender());
        
    }
    
    /**
     * This class will send repeatedly a simulated discovery request
     * and the Device class will respond back on the port this class
     * put in the request message.
     */
    static class Sender extends AbstractVerticle {
        private final static Logger LOG = LoggerFactory.getLogger(Sender.class);
        
        @Override
        public void start(Promise<Void> startPromise) throws Exception {

            // Create the options 
            DatagramSocketOptions options = new DatagramSocketOptions()
                    .setReuseAddress(true)
                    .setIpV6(true)
                    .setReusePort(true)
                    .setMulticastNetworkInterface(INTERFACE);

            // Create the socket to which we will listen on Multicast messages
            DatagramSocket socket6 = vertx.createDatagramSocket(options);

            // Create the socket to which we will listen for Discovery replies
            DatagramSocket uniSocket = vertx.createDatagramSocket(options);

            // Listen for unicast message
            setupUnicastListener(startPromise, uniSocket);

            // Data to send - This mimics a discovery request
            // and puts the port to respond back to
            JsonObject payload = new JsonObject();
            payload.put("action", "REQUEST");
            payload.put("responsePort", uniSocket.localAddress().port());
            
            // Add the data
            Buffer buffer = Buffer.buffer(payload.encode());

            // Send every 5 seconds - Just for fun
            LOG.info("Starting to send Discovery Requests every 5 seconds");
            vertx.setPeriodic(5000, id -> {
                int count = 0;
                LOG.info("Sending request #{}", ++count);
                socket6.send(buffer, LISTEN_PORT, MULTICAST_GROUP)
                        .onSuccess(v -> LOG.info("Request sent successfully"))
                        .onFailure(Throwable::printStackTrace);
            });
            
        }
        
        /**
         * Listens on a ephemeral port which will be used to receive the
         * discovery responses
         * 
         * @param startPromise
         * @param socket 
         */
        private void setupUnicastListener(Promise<Void> startPromise, DatagramSocket socket) {
            socket.listen(0, LISTEN_ALL_INTERFACES)
                    .onSuccess(result -> LOG.info("Listening for Unicast message on IPV6 @ {}", result.localAddress()))
                    .onFailure(startPromise::fail);
            
            // Handle the discovery reponses
            socket.handler(this::processDiscoveryResponse);
        }

        /**
         * Used to process device discovery messages responses
         */
        private void processDiscoveryResponse(DatagramPacket packet) {
            LOG.info("Got a discovery response message from {} with data - {}", packet.sender(), packet.data());
        }
    }

    /**
     * This class simulates a device on the network that will respond to a
     * discovery request message using the port contained in the request message
     */
    static class Device extends AbstractVerticle {
        
        private final static Logger LOG = LoggerFactory.getLogger(Device.class);
        
        private DatagramSocket socket6;
        private String id;
        
        public Device() {
            id = UUID.randomUUID().toString();
        }
        
        @Override
        public void start(Promise<Void> startPromise) throws Exception {

            // Create the options 
            DatagramSocketOptions options = new DatagramSocketOptions()
                    .setReuseAddress(true)
                    .setIpV6(true)
                    .setLogActivity(true)
                    .setReusePort(true);

            // Create the socket
            socket6 = vertx.createDatagramSocket(options);

            //Listen for discovery messages
            setupDiscoveryListener(startPromise, socket6);
            
        }
        
        /**
         * Listen on the Multicast Group
         * 
         * @param startPromise
         * @param socket 
         */
        private void setupDiscoveryListener(Promise<Void> startPromise, DatagramSocket socket) {

            // Listen on all interfaces and broadcast group
            socket.listen(LISTEN_PORT, LISTEN_ALL_INTERFACES)
                    .onSuccess(result -> {
                        result.listenMulticastGroup(MULTICAST_GROUP, INTERFACE, null)
                                .onSuccess(v -> LOG.info("Listening for Multicast Messages on IPv6 Group [{}] on local address ({}): {}",MULTICAST_GROUP,INTERFACE,result.localAddress()))
                                .onFailure(startPromise::fail);
                    })
                    .onFailure(startPromise::fail);

            // Print out the message
            socket.handler(this::respondToDiscovery);
        }

        /**
         * Used to respond to discovery requests
         *
         * @param packet
         */
        private void respondToDiscovery(DatagramPacket packet) {
            LOG.info("Received Discovery Packet from {} with data {}",packet.sender(), packet.data());
            
            JsonObject response = new JsonObject();
            response.put("id", id);
            response.put("date", LocalDateTime.now().toString());
            
            Buffer buffer = response.toBuffer();

            // Read the port we are supposed to reply back to
            // this port is the port that the 
            JsonObject toJsonObject = packet.data().toJsonObject();
            int parseInt = toJsonObject.getInteger("responsePort");
            
            
            LOG.info("Sending response back to {}:{} with data - {}",packet.sender().host(), parseInt, buffer);
            
            // Send back the response using the port number supplied in the request
            // It's here where it fails using IPv6 on MacOS but works
            // on Linux
            // I get a "No Route to Host"
            socket6.send(buffer, parseInt, packet.sender().host())
                    .onSuccess(result -> LOG.info("Send ok"))
                    .onFailure(Throwable::printStackTrace);
        }
        
    }
    
}
