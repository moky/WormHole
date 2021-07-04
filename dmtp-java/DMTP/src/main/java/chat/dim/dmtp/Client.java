/* license: https://mit-license.org
 *
 *  DMTP: Direct Message Transfer Protocol
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
package chat.dim.dmtp;

import java.net.SocketAddress;

import chat.dim.dmtp.protocol.Command;
import chat.dim.dmtp.protocol.LocationValue;

public abstract class Client extends Node {

    protected Client(SocketAddress local) {
        super(local);
    }

    //
    //  Process
    //

    protected boolean processSign(LocationValue location, SocketAddress destination) {
        LocationDelegate delegate = getDelegate();
        assert delegate != null : "contact delegate not set yet";
        LocationValue mine = delegate.signLocation(location);
        if (mine == null) {
            throw new NullPointerException("failed to sign the location: " + location);
        }
        // update the signed location
        if (delegate.storeLocation(mine)) {
            return sayHello(destination);
        } else {
            throw new AssertionError("failed to update location: " + mine);
        }
    }

    protected boolean processFrom(LocationValue location) {
        // when someone is calling you,
        // respond anything (say "HI") to build the connection
        LocationDelegate delegate = getDelegate();
        assert delegate != null : "contact delegate not set yet";
        if (!delegate.storeLocation(location)) {
            //throw new RuntimeException("failed to update location: " + location);
            return false;
        }
        SocketAddress address = location.getSourceAddress();
        if (address != null) {
            connect(address);
            sayHello(address);
        }
        address = location.getMappedAddress();
        if (address != null) {
            connect(address);
            sayHello(address);
        }
        return true;
    }

    @Override
    public boolean processCommand(Command cmd, SocketAddress source) {
        if (cmd.tag.equals(Command.SIGN))
        {
            assert cmd.value instanceof LocationValue : "sign command error: " + cmd.value;
            return processSign((LocationValue) cmd.value, source);
        }
        else if (cmd.tag.equals(Command.FROM))
        {
            assert cmd.value instanceof LocationValue : "call from error: " + cmd.value;
            return processFrom((LocationValue) cmd.value);
        }
        else
        {
            return super.processCommand(cmd, source);
        }
    }
}
