
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import chat.dim.mtp.DataType;
import chat.dim.mtp.Package;
import chat.dim.mtp.PackageDocker;
import chat.dim.net.Connection;
import chat.dim.net.Hub;
import chat.dim.port.Departure;
import chat.dim.port.Docker;
import chat.dim.skywalker.Runner;
import chat.dim.startrek.StarGate;
import chat.dim.type.Data;

public class UDPGate<H extends Hub> extends StarGate implements Runnable {

    private boolean running = false;
    private H hub = null;

    public UDPGate(Delegate delegate) {
        super(delegate);
    }

    public H getHub() {
        return hub;
    }
    public void setHub(H h) {
        hub = h;
    }

    public void start() {
        new Thread(this).start();
    }

    public void stop() {
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    @Override
    public void run() {
        running = true;
        while (isRunning()) {
            if (!process()) {
                idle();
            }
        }
    }

    protected void idle() {
        Runner.idle(8);
    }

    @Override
    public boolean process() {
        boolean incoming = hub.process();
        boolean outgoing = super.process();
        return incoming || outgoing;
    }

    @Override
    public Connection getConnection(SocketAddress remote, SocketAddress local) {
        return hub.getConnection(remote, local);
    }

    @Override
    protected Docker createDocker(SocketAddress remote, SocketAddress local, List<byte[]> data) {
        // TODO: check data format before creating docker
        Connection conn = getConnection(remote, local);
        if (conn == null) {
            info("connection not found: " + remote);
        }
        return new PackageDocker(remote, local, this);
    }

    @Override
    protected List<byte[]> cacheAdvanceParty(byte[] data, SocketAddress source, SocketAddress destination, Connection connection) {
        // TODO: cache the advance party before decide which docker to use
        List<byte[]> array = new ArrayList<>();
        if (data != null) {
            array.add(data);
        }
        return array;
    }

    @Override
    protected void clearAdvanceParty(SocketAddress source, SocketAddress destination, Connection connection) {
        // TODO: remove advance party for this connection
    }

    public void sendCommand(byte[] body, SocketAddress source, SocketAddress destination) {
        Package pack = Package.create(DataType.COMMAND, new Data(body));
        send(pack, Departure.Priority.SLOWER.value, source, destination);
    }

    public void sendMessage(byte[] body, SocketAddress source, SocketAddress destination) {
        Package pack = Package.create(DataType.MESSAGE, new Data(body));
        send(pack, source, destination);
    }

    public void send(Package pack, int priority, SocketAddress source, SocketAddress destination) {
        Docker worker = getDocker(destination, source, null);
        ((PackageDocker) worker).send(pack, priority);
    }
    public void send(Package pack, SocketAddress source, SocketAddress destination) {
        Docker worker = getDocker(destination, source, null);
        ((PackageDocker) worker).send(pack);
    }

    static void info(String msg) {
        System.out.printf("%s\n", msg);
    }
    static void info(byte[] data) {
        info(new String(data, StandardCharsets.UTF_8));
    }
    static void error(String msg) {
        System.out.printf("ERROR> %s\n", msg);
    }
}
