package chat.dim.stargate;

import java.io.IOError;
import java.net.SocketAddress;
import java.util.List;

import chat.dim.net.Connection;
import chat.dim.net.ConnectionState;
import chat.dim.net.Hub;
import chat.dim.port.Docker;
import chat.dim.startrek.PlainDocker;
import chat.dim.utils.Log;

public class TCPGate<H extends Hub> extends AutoGate<H> {

    public TCPGate(Docker.Delegate delegate, boolean isDaemon) {
        super(delegate, isDaemon);
    }

    public boolean sendMessage(byte[] payload, SocketAddress remote, SocketAddress local) {
        Docker worker = getDocker(remote, local, null);
        if (worker == null || !worker.isOpen()) {
            return false;
        }
        return worker.sendData(payload);
    }

    //
    //  Docker
    //

    @Override
    protected Docker createDocker(Connection conn, List<byte[]> data) {
        // TODO: check data format before creating docker
        PlainDocker docker = new PlainDocker(conn);
        docker.setDelegate(getDelegate());
        return docker;
    }

    //
    //  Connection Delegate
    //

    @Override
    public void onConnectionStateChanged(ConnectionState previous, ConnectionState current, Connection connection) {
        super.onConnectionStateChanged(previous, current, connection);
        Log.info("connection state changed: " + previous + " -> " + current + ", " + connection);
    }

    @Override
    public void onConnectionFailed(IOError error, byte[] data, Connection connection) {
        super.onConnectionFailed(error, data, connection);
        Log.error("connection failed: " + error + ", " + connection);
    }

    @Override
    public void onConnectionError(IOError error, Connection connection) {
        super.onConnectionError(error, connection);
        Log.error("connection error: " + error + ", " + connection);
    }

}
