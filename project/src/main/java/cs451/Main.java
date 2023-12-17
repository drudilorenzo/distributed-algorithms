package cs451;

import cs451.parsers.LatticeParserImpl;
import cs451.lattice.manager.LatticeManager;
import cs451.lattice.manager.LatticeManagerImpl;

import java.util.Set;
import java.io.FileWriter;
import java.io.IOException;
import java.util.stream.Collectors;

/**
 * Lorenzo Drudi
 * lorenzo.drudi@epfl.ch
 * 367980
 */

/**
 * To execute:
 * ./run.sh --id 1 --hosts ../example/hosts --output ../example/output/1.output ../example/configs/lattice-agreement-1.config
 */

public class Main {

    // To be able to use them they should be static.
    private static LatticeManager latticeManager; // lattice manager
    private static FileWriter writer;             // file writer to write to the output file

    private static void printDeliver(final Set<Integer> decision) {
        try {
            var s = decision.stream().map(Object::toString).collect(Collectors.joining(" "));
            writer.write(s + "\n");
        } catch (IOException e) {
            System.out.println("Delivering: Error writing to output file.\n");
            System.exit(1);
        }
    }

    private static void handleSignal() {
        //immediately stop network packet processing
        System.out.println("Immediately stopping network packet processing.");

        //close all connections
        Main.latticeManager.close();

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

        // If the process is not in the hosts file, exit
        if (myPort == -1) {
            System.out.println("Could not find my port.");
            System.exit(1);
        }
        System.out.println();

        System.out.println("Path to output:");
        System.out.println("===============");
        System.out.println(parser.output() + "\n");

        System.out.println("Path to config:");
        System.out.println("===============");
        System.out.println(parser.config() + "\n");

        System.out.println("Doing some initialization\n");

        try {
            writer = new FileWriter(parser.output());
        } catch (IOException e) {
            System.out.println("Error opening output file.");
            System.exit(1);
        }

        Main.latticeManager = new LatticeManagerImpl(
                parser.hosts(),
                parser.myId(),
                myPort,
                Main::printDeliver
        );

        // Read config file
        var latticeParser = new LatticeParserImpl(parser.config());

        System.out.println("Doing " + latticeParser.getP() + " proposals\n");

        Set<Integer> proposal;
        while ((proposal = latticeParser.getNextProposal()) != null) {
            latticeManager.propose(proposal);
        }

        // After a process finishes broadcasting,
        // it waits forever for the delivery of messages.
        while (true) {
            // Sleep for 1 hour
            Thread.sleep(60 * 60 * 1000);
        }
    }
}
