
import java.io.IOException;
import java.net.SocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;

import chat.dim.mtp.DataType;
import chat.dim.mtp.Package;
import chat.dim.mtp.PackageDocker;
import chat.dim.net.Connection;
import chat.dim.net.Hub;
import chat.dim.port.Docker;
import chat.dim.startrek.StarGate;
import chat.dim.type.Data;

public class UDPGate<H extends Hub> extends StarGate implements Runnable {

    private boolean running = false;
    H hub = null;

    public UDPGate(Delegate delegate) {
        super(delegate);
    }

    public void start() {
        new Thread(this).start();
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        running = true;
        while (running) {
            if (!process()) {
                idle(8);
            }
        }
    }

    @Override
    public boolean process() {
        boolean activated = hub.process();
        boolean busy = super.process();
        return activated || busy;
    }

    @Override
    protected Connection getConnection(SocketAddress remote, SocketAddress local) {
        return hub.getConnection(remote, local);
    }

    @Override
    protected Connection connect(SocketAddress remote, SocketAddress local) throws IOException {
        return hub.connect(remote, local);
    }

    @Override
    protected Docker createDocker(SocketAddress remote, SocketAddress local, List<byte[]> data) {
        // TODO: check data format before creating docker
        return new PackageDocker(remote, local, data, this);
    }

    void sendCommand(byte[] body, SocketAddress source, SocketAddress destination) {
        Package pack = Package.create(DataType.COMMAND, new Data(body));
        Object worker = getDocker(destination, source, null);
        ((PackageDocker) worker).sendPackage(pack);
    }

    void sendMessage(byte[] body, SocketAddress source, SocketAddress destination) {
        Package pack = Package.create(DataType.MESSAGE, new Data(body));
        Object worker = getDocker(destination, source, null);
        ((PackageDocker) worker).sendPackage(pack);
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

    static void idle(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}