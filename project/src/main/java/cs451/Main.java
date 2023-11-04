package cs451;

import cs451.links.PerfectLink;
import cs451.message.Message;
import cs451.message.PayloadMessageImpl;

import java.io.File;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

/**
 * To execute:
 * ./run.sh --id 1 --hosts ../example/hosts --output ../example/output.txt ../example/configs/perfect-links.config
 */

public class Main {

    // To be able to use them they should be static.
    private static long timeInit;         // time of the first send
    private static BufferedWriter writer; // buffered writer to write to the output file
    private static PerfectLink pLink;     // perfect link abstraction

    private static void printDeliver(final Message message) {
        final int senderId = message.getSenderId();
        final int seqNum = message.getId();
        try {
            writer.write("d " + senderId + " " + seqNum + "\n");
        } catch (IOException e) {
            System.out.println("Delivering: Error writing to output file.\n");
            System.exit(1);
        }
    }

    private static void handleSignal() {
        System.out.println("Stop: " + (System.currentTimeMillis() - Main.timeInit) + " ms\n");

        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        //close all connections
        Main.pLink.close();

        // write/flush output file
        System.out.println("Writing output.");

        try {
            Main.writer.flush();
            Main.writer.close();
        } catch (IOException e) {
            System.out.println("Error closing output file");
        }
    }

    private static void initSignalHandlers() {
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                Main.handleSignal();
            }
        });
    }

    @SuppressWarnings({"InfiniteLoopStatement", "BusyWait"})
    public static void main(String[] args) throws InterruptedException {
        Parser parser = new Parser(args);
        parser.parse();

        Main.initSignalHandlers();

        long pid = ProcessHandle.current().pid();
        System.out.println("My PID: " + pid + "\n");
        System.out.println("From a new terminal type `kill -SIGINT " + pid + "` or `kill -SIGTERM " + pid + "` to stop processing packets\n");

        System.out.println("My ID: " + parser.myId() + "\n");
        System.out.println("List of resolved hosts is:");
        System.out.println("==========================");
        for (Host host: parser.hosts()) {
            System.out.println(host.getId());
            System.out.println("Human-readable IP: " + host.getIp());
            System.out.println("Human-readable Port: " + host.getPort());
            System.out.println();
        }
        System.out.println();

        System.out.println("Path to output:");
        System.out.println("===============");
        System.out.println(parser.output() + "\n");

        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.config() + "\n");

        System.out.println("Doing some initialization\n");

        // Read config file
        int numMessages = 0;
        int receiverId = 0;
        try (Scanner scanner = new Scanner(new File(parser.config()))) {
            scanner.useDelimiter(" |\\n");
            numMessages = scanner.nextInt();
            System.out.println("Number of messages to broadcast: " + numMessages);
            receiverId = scanner.nextInt();
            System.out.println("Receiver Id: " + receiverId + "\n");
        } catch (IOException e) {
            System.out.println("Error reading config file.");
            System.exit(1);
        }

        // Get port of the process
        int myPort = -1;
        for (Host host: parser.hosts()) {
            if (host.getId() == parser.myId()) {
                myPort = host.getPort();
                break;
            }
        }

        // If the process is not in the hosts file, exit
        if (myPort == -1) {
            System.out.println("Could not find my port.");
            System.exit(1);
        }

        try {
            var fWriter = new FileWriter(parser.output());
            writer = new BufferedWriter(fWriter);
        } catch (IOException e) {
            System.out.println("Error opening output file.");
            System.exit(1);
        }

        System.out.print("PerfectLink is ready to send messages.\n");
        System.out.println("Broadcasting and delivering messages...\n");

        // Set up PerfectLink
        Main.pLink = new PerfectLink(parser.myId(), myPort, parser.hosts(), Main::printDeliver);

        // Start the timer
        Main.timeInit = System.currentTimeMillis();
        
        // If the process is not the receiver, broadcast messages
        if (parser.myId() != receiverId) {
            // Broadcast messages
            for (int i = 1; i <= numMessages; i++) {
                try {
                    Main.writer.write("b " + i + "\n");
                } catch (IOException e) {
                    System.out.println("Broadcasting: Error writing to output file.\n");
                    System.exit(1);
                }

                // Create payload (int to byte array)
                var payload = new byte[4];
                payload[0] = (byte)((i >> 24) & 0xff);
                payload[1] = (byte)((i >> 16) & 0xff);
                payload[2] = (byte)((i >> 8) & 0xff);
                payload[3] = (byte)(i & 0xff);

                Main.pLink.send(
                        new PayloadMessageImpl(
                                payload,
                                i,
                                parser.myId(),
                                receiverId)
                );
            }
            System.out.println("Broadcast done, waiting for the delivery.\n");
        }

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
