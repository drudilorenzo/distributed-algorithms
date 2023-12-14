package cs451.parsers;

import java.io.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link LatticeParser}.
 */
public class LatticeParserImpl implements LatticeParser {

    private BufferedReader reader;
    private int p;  // number of proposals for each process
    private int vs; // maximum number of elements in a proposal
    private int ds; // maximum number of distinct elements in all proposals

    /**
     * Constructor of {@link LatticeParserImpl}.
     *
     * @param configPath: the path to the configuration file.
     */
    public LatticeParserImpl(final String configPath) {
        try {
            this.reader = new BufferedReader(new FileReader(configPath));
            String line = this.reader.readLine();

            if (line == null) {
                System.err.println("Error: empty configuration file.");
                System.exit(1);
            }

            String[] parameters = line.split(" ");
            if (parameters.length != 3) {
                System.err.println("Error: invalid configuration file.");
                System.exit(1);
            }

            this.p = Integer.parseInt(parameters[0]);
            this.vs = Integer.parseInt(parameters[1]);
            this.ds = Integer.parseInt(parameters[2]);
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            System.exit(1);
        }

    }

    @Override
    public Set<Integer> getNextProposal() {
        try {
            String line = this.reader.readLine();

            if (line == null) {
                return null;
            }

            String[] elements = line.split(" ");
            if (elements.length > this.vs) {
                System.err.println("Error: invalid configuration file.");
                System.exit(1);
            }

            Set<Integer> proposal = new HashSet<>();
            for (String element : elements) {
                proposal.add(Integer.parseInt(element));
            }

            return proposal;
        } catch (IOException e) {
            Thread.currentThread().interrupt();
            System.exit(1);
            return null;
        }
    }

    @Override
    public int getP() {
        return this.p;
    }

    @Override
    public int getVs() {
        return this.vs;
    }

    @Override
    public int getDs() {
        return this.ds;
    }

}
