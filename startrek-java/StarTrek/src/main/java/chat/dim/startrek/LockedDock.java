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
package chat.dim.startrek;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import chat.dim.port.Arrival;
import chat.dim.port.Departure;

public class LockedDock extends Dock {

    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    @Override
    public Arrival assembleArrival(final Arrival income) {
        final Arrival completed;
        final Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            completed = super.assembleArrival(income);
        } finally {
            writeLock.unlock();
        }
        return completed;
    }

    @Override
    public boolean appendDeparture(final Departure ship) {
        final boolean added;
        final Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            added = super.appendDeparture(ship);
        } finally {
            writeLock.unlock();
        }
        return added;
    }

    @Override
    public Departure checkResponse(final Arrival response) {
        final Departure finished;
        final Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            finished = super.checkResponse(response);
        } finally {
            writeLock.unlock();
        }
        return finished;
    }

    @Override
    public Departure getNextDeparture() {
        final Departure next;
        final Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            next = super.getNextDeparture();
        } finally {
            writeLock.unlock();
        }
        return next;
    }

    @Override
    public void purge() {
        final Lock writeLock = lock.writeLock();
        writeLock.lock();
        try {
            super.purge();
        } finally {
            writeLock.unlock();
        }
    }
}
