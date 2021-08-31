/* license: https://mit-license.org
 *
 *  Star Trek: Interstellar Transport
 *
 *                                Written in 2021 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2021 Albert Moky
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 * ==============================================================================
 */
package chat.dim.net;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class ActiveConnection<P> extends BaseConnection<P> {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private int connecting;
    private boolean running;

    public ActiveConnection(Channel byteChannel, SocketAddress remote, SocketAddress local) {
        super(byteChannel, remote, local);
        connecting = 0;
        running = false;
    }

    public ActiveConnection(SocketAddress remote, SocketAddress local) {
        this(null, remote, local);
    }

    protected abstract Channel connect(SocketAddress remote, SocketAddress local) throws IOException;

    private boolean reconnect() throws IOException {
        boolean redo = false;
        Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            connecting += 1;
            if (connecting == 1 && running) {
                changeState(ConnectionState.CONNECTING);
                channel = connect(remoteAddress, localAddress);
                if (channel == null) {
                    changeState(ConnectionState.ERROR);
                } else {
                    changeState(ConnectionState.CONNECTED);
                    redo = true;
                }
            }
        } finally {
            connecting -= 1;
            writeLock.unlock();
        }
        return redo;
    }

    @Override
    protected Channel getChannel() throws IOException {
        if (channel == null) {
            reconnect();
        }
        return super.getChannel();
    }

    @Override
    public boolean isOpen() {
        return running;
    }

    @Override
    public void start() {
        running = true;
        super.start();
    }

    @Override
    public void stop() {
        running = false;
        super.stop();
    }

    @Override
    protected SocketAddress receive(ByteBuffer dst) throws IOException {
        SocketAddress remote = super.receive(dst);
        if (remote == null && channel == null && reconnect()) {
            // try again
            remote = super.receive(dst);
        }
        return remote;
    }

    @Override
    protected int send(ByteBuffer src, SocketAddress destination) throws IOException {
        int sent = super.send(src, destination);
        if (sent == -1 && channel == null && reconnect()) {
            // try again
            sent = super.send(src, destination);
        }
        return sent;
    }

    @Override
    public void exitState(ConnectionState previous, StateMachine ctx) {
        super.exitState(previous, ctx);

        ConnectionState current = ctx.getCurrentState();
        if (current != null && current.equals(ConnectionState.EXPIRED)) {
            try {
                heartbeat();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     *  Send a heartbeat package('PING') to remote address
     */
    public abstract void heartbeat() throws IOException;
}
