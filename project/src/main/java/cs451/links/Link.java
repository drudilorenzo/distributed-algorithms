package cs451.links;

import cs451.Host;
import cs451.packet.Packet;

public interface Link {

    void send(Packet packet, Host host);

}
