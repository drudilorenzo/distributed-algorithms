package cs451;

import cs451.broadcast.FIFOBroadcast;

import java.io.File;
import java.util.Scanner;
import java.io.FileWriter;
import java.io.IOException;

/*
 * Student:
 * - name:   Lorenzo Drudi
 * - email:  lorenzo.drudi@epfl.ch
 * - SCIPER: 367980
 */

/*
 * To execute:
 *
 * Manual:
 * ./run.sh --id 1 --hosts ../example/hosts --output ../example/output/1.output ../example/configs/fifo-broadcast.confi
 *
 * Automatic:
 * ./stress.py fifo -r ../project/run.sh -l ../example/output -p 3 -m 5
 * */

public class Main {

    // To be able to use them they should be static.
    private static FIFOBroadcast fifoBroadcast; // fifo broadcast abstraction
    private static FileWriter writer;           // file writer to write to the output file

    private static void printDeliver(final int seqNum, final int senderId) {
        try {
            writer.write("d " + senderId + " " + seqNum + "\n");
        } catch (IOException e) {
            System.out.println("Delivering: Error writing to output file.\n");
            System.exit(1);
        }
    }

    private static void handleSignal() {

        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        //close all connections
        //Main.fifoBroadcast.close();

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
        Host host;
        int myPort = -1;
        for (int i = 0; i < parser.hosts().size(); i++) {
            host = parser.hosts().get(i);
            if (host.getId() == parser.myId()) {
                myPort = host.getPort();
                break;
            }
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
        try (Scanner scanner = new Scanner(new File(parser.config()))) {
            scanner.useDelimiter(" |\\n");
            numMessages = scanner.nextInt();
            System.out.println("Number of messages to broadcast: " + numMessages);
        } catch (IOException e) {
            System.out.println("Error reading config file.");
            System.exit(1);
        }

        // If the process is not in the hosts file, exit
        if (myPort == -1) {
            System.out.println("Could not find my port.");
            System.exit(1);
        }

        try {
            writer = new FileWriter(parser.output());
        } catch (IOException e) {
            System.out.println("Error opening output file.");
            System.exit(1);
        }

        // Initialize the fifo broadcast abstraction
        System.out.println("Initializing fifo broadcast abstraction.\n");
        var t1 = System.currentTimeMillis();
        Main.fifoBroadcast = new FIFOBroadcast(parser.myId(), myPort, parser.hosts(), Main::printDeliver);
        var t2 = System.currentTimeMillis();
        System.out.println("FIFO broadcast abstraction initialized in " + (t2 - t1) + " ms.\n");
        System.out.print("Ready to (fifo) broadcast messages.\n");

        System.out.println("Broadcasting and delivering messages...\n");

        for (int  msgId= 1; msgId <= numMessages; msgId++) {
            try {
                Main.writer.write("b " + msgId + "\n");
            } catch (IOException e) {
                System.out.println("Broadcasting: Error writing to output file.\n");
                System.exit(1);
            }

            // Create payload.
            // Empty since the seq num is in the message id.
            var payload = new byte[0];

            Main.fifoBroadcast.broadcast(
                    msgId,
                    payload
            );
        }
        System.out.println("Broadcast done, waiting for the delivery.\n");

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
