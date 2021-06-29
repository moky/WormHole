/* license: https://mit-license.org
 *
 *  TCP: Transmission Control Protocol
 *
 *                                Written in 2020 by Moky <albert.moky@gmail.com>
 *
 * ==============================================================================
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 Albert Moky
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

import java.net.SocketAddress;

public interface Connection {

    //
    //  Flags
    //
    boolean isOpen();  // not closed
    boolean isBound();
    boolean isConnected();

    SocketAddress getRemoteAddress();

    /**
     *  Send data
     *
     * @param data - outgo package
     * @return count of bytes sent, -1 on error
     */
    int send(byte[] data);

    /**
     *  Receive data
     *
     * @return income package
     */
    byte[] receive();

    /**
     *  Close the connection
     */
    void close();

    /**
     *  Get status
     *
     * @return connection status
     */
    ConnectionState getState();

    interface Delegate {

        /**
         *  Call when connection status changed
         *
         * @param connection - current connection
         * @param oldStatus - status before
         * @param newStatus - status after
         */
        void onConnectionStateChanged(Connection connection, ConnectionState oldStatus, ConnectionState newStatus);
    }
}