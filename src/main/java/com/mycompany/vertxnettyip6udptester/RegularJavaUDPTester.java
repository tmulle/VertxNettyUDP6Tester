package com.mycompany.vertxnettyip6udptester;

import io.vertx.core.json.JsonObject;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Tester that demonstrates UDP sending using the standard Java Multicast
 * sockets
 * 
 * This class is showing that MacOS UDP ipv6 sockets do work and there is 
 * some sort of issue in Netty or Vert.x when handling UDP ipv6 on MacOS
 *
 * @author tmulle
 */
public class RegularJavaUDPTester {

    private static final Logger LOG = LoggerFactory.getLogger(RegularJavaUDPTester.class);

    private static String INTERFACE;
    private static String MULTICAST_GROUP;
    private static String LISTEN_ALL_INTERFACE;
    private static int LISTEN_PORT;
    private static IP_MODE MODE;
    private static Operation_Mode OP_MODE;

    private static final ExecutorService executorService = Executors.newCachedThreadPool();
    private static final ScheduledExecutorService scheduledExecutorService = Executors.newSingleThreadScheduledExecutor();

    // Which mode
    private static enum IP_MODE {
        IPv4, IPv6
    };

    private static enum Operation_Mode {
        Sender, Device, Both
    };

    /**
     * Main
     *
     * @param args
     */
    public static void main(String[] args) {

        // Setup the variables
        MODE = IP_MODE.valueOf(System.getProperty("net.ip_mode", "IPv6"));
        INTERFACE = System.getProperty("net.interface", "en0");
        LISTEN_PORT = Integer.parseInt(System.getProperty("net.listen_port", "35056"));

        // Which mode?
        switch (MODE) {
            case IPv4: {
                MULTICAST_GROUP = "224.0.0.224";
                LISTEN_ALL_INTERFACE = "0.0.0.0";
                break;
            }
            case IPv6: {
                MULTICAST_GROUP = "FF02::1";
                LISTEN_ALL_INTERFACE = "::";
                break;
            }
        }

        // Which service are we running? Default ALL
        OP_MODE = Operation_Mode.valueOf(System.getProperty("service_mode", "Both"));

        LOG.info("*** Starting Variables ***");
        LOG.info("Operation Mode = {}", OP_MODE);
        LOG.info("Network Mode = {}", MODE);
        LOG.info("Network Interface = {}", INTERFACE);
        LOG.info("Network Listen All Interface = {}", LISTEN_ALL_INTERFACE);
        LOG.info("Network Multicast Group = {}", MULTICAST_GROUP);
        LOG.info("Network Port = {}", LISTEN_PORT);
        LOG.info("**************************");

        switch (OP_MODE) {
            case Sender: {
                executorService.submit(new Sender());
                break;
            }
            case Device: {
                executorService.submit(new Device());
                break;
            }
            default: {
                executorService.submit(new Sender());
                executorService.submit(new Device());
            }
        }
    }

    
     /**
     * This class will send repeatedly a simulated discovery request and the
     * Device class will respond back on the port this class put in the request
     * message.
     */
    static class Sender implements Runnable {

        private final static Logger LOG = LoggerFactory.getLogger(Sender.class);

        DatagramSocket uniSocket;
        InetAddress mcAddress;

        @Override
        public void run() {

            try {

                // Create the socket and multicast group
                uniSocket = new DatagramSocket(0);
                mcAddress = InetAddress.getByName(MULTICAST_GROUP);

                // Set up the unicast listener thread to listen on all interfaces
                // and an ephemeral port
                setupUnicastListener();

                // Set up the scheduled multicast message sender to send
                // every 5 seconds
                setupSenderLoop();

            } catch (Exception ex) {
                LOG.error("Error", ex);
            }
        }

        /**
         * Creates a sender loop which will send out a broadcast every 5 seconds
         *
         * @throws Exception
         */
        private void setupSenderLoop() throws Exception {

            // Create the sender socket
            DatagramSocket sender = new DatagramSocket();
            
            // Create the runnable
            Runnable senderRunnable = () -> {
                JsonObject request = new JsonObject();
                request.put("action", "REQUEST");
                request.put("responsePort", uniSocket.getLocalPort());

                byte[] buf = request.toBuffer().getBytes();
                DatagramPacket packet = new DatagramPacket(buf, buf.length, mcAddress, LISTEN_PORT);
                try {
                    LOG.info("Sending request...");
                    sender.send(packet);
                    LOG.info("Request sent successfully");
                } catch (Exception e) {
                    LOG.error("Error in sending", e);
                }
            };

            // Send every 5 seconds - Just for fun
            LOG.info("Starting to send Discovery Requests every 5 seconds");
            scheduledExecutorService.scheduleAtFixedRate(senderRunnable, 5, 5, TimeUnit.SECONDS);
        }

        /**
         * Creates and submits the unicast listener process
         *
         * @throws Exception
         */
        private void setupUnicastListener() throws Exception {

            Runnable uniListener = () -> {
                try {
                    LOG.info("Listening for Unicast message on {} @ {}", MODE, uniSocket.getLocalSocketAddress());
                    while (!Thread.interrupted()) {

                        // Will hold the data we receive
                        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
                        uniSocket.receive(packet);

                        // Print out what we got
                        String response = new String(packet.getData()).trim();
                        LOG.info("Got a discovery response message from {} with data - {}", packet.getAddress(), response);
                    }
                } catch (Exception e) {
                    LOG.error("Error in Unicast listener", e);
                }
            };

            // Start the listener
            executorService.submit(uniListener);
        }

    }

    /**
     * This class simulates a device on the network that will respond to a
     * discovery request message using the port contained in the request message
     */
    static class Device implements Runnable {

        private final static Logger LOG = LoggerFactory.getLogger(Tester.Device.class);

        private InetAddress mcIPAddress;
        private MulticastSocket mcSocket;
        private String id = UUID.randomUUID().toString();

        @Override
        public void run() {

            try {

                // Join the multicast group
                mcIPAddress = InetAddress.getByName(MULTICAST_GROUP);
                mcSocket = new MulticastSocket(LISTEN_PORT);
                mcSocket.joinGroup(mcIPAddress);

                // Will hold the data we receive
                DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);

                LOG.info("Multicast Receiver running at {}", mcSocket.getLocalSocketAddress());

                // Multicast listener job
                try {
                    while (!Thread.interrupted()) {

                        LOG.info("Waiting for a  multicast message...");
                        mcSocket.receive(packet);

                        // Resond to packet
                        respondToDiscovery(packet);

                    }
                } catch (IOException iOException) {
                    LOG.error("Error reading multicast", iOException);
                }

            } catch (IOException iOException) {
                LOG.error("Something went wrong", iOException);
            }
        }

        /**
         * Used to respond to discovery requests
         *
         * @param packet
         */
        private void respondToDiscovery(DatagramPacket packet) throws IOException {

            // Get the data
            String incomingData = new String(packet.getData()).trim();

            LOG.info("Received Discovery Packet from {} with data {}", packet.getAddress(), incomingData);

            // Build response
            JsonObject response = new JsonObject();
            response.put("id", id);
            response.put("date", LocalDateTime.now().toString());

            // Read out the request port variable
            JsonObject incoming = new JsonObject(incomingData);
            int port = incoming.getInteger("responsePort");

            // Build the new destination address with the new port
            SocketAddress dest = new InetSocketAddress(packet.getAddress(), port);

            // Get the payload data
            byte[] data = response.toString().getBytes();

            // Build the packet
            DatagramPacket responsePacket = new DatagramPacket(data, data.length, dest);

            // Send the data
            LOG.info("Sending response data to {} - {}", dest, new String(data).trim());
            mcSocket.send(responsePacket);
        }
    }
}
