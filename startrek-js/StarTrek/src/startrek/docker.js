;
// license: https://mit-license.org
//
//  Star Trek: Interstellar Transport
//
//                               Written in 2022 by Moky <albert.moky@gmail.com>
//
// =============================================================================
// The MIT License (MIT)
//
// Copyright (c) 2022 Albert Moky
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
// =============================================================================
//

//! require 'type/apm.js'
//! require 'net/connection.js'
//! require 'port/docker.js'
//! require 'port/arrival.js'
//! require 'port/departure.js'
//! require 'dock.js'

(function (ns, sys) {
    'use strict';

    var AddressPairObject = ns.type.AddressPairObject;
    var Docker = ns.port.Docker;
    var DockerStatus = ns.port.DockerStatus;
    var Dock = ns.Dock;

    /**
     *  Base Docker
     *
     * @param {Connection} connection
     */
    var StarDocker = function (connection) {
        var remote = connection.getRemoteAddress();
        var local = connection.getLocalAddress();
        AddressPairObject.call(this, remote, local);
        this.__conn = connection;
        this.__delegate = null;          // DockerDelegate
        this.__dock = this.createDock();
        this.__lastOutgo = null;         // Departure
        this.__lastFragments = [];       // Uint8Array[]
    };
    sys.Class(StarDocker, AddressPairObject, [Docker], null);

    StarDocker.prototype.finalize = function () {
        // make sure the relative connection is closed
        removeConnection.call(this);
        // super.finalize();
    };

    // protected: override for user-customized dock
    StarDocker.prototype.createDock = function () {
        return new Dock();
    };

    // protected
    StarDocker.prototype.getDelegate = function () {
        return this.__delegate;
    };
    // public: delegate for handling docker events
    StarDocker.prototype.setDelegate = function (delegate) {
        this.__delegate = delegate;
    };

    // protected
    StarDocker.prototype.getConnection = function () {
        return this.__conn;
    };
    var removeConnection = function () {
        // 1. clear connection reference
        var old = this.__conn;
        this.__conn = null;
        // 2. close old connection
        if (old && old.isOpen()) {
            old.close();
        }
    };

    // Override
    StarDocker.prototype.isOpen = function () {
        var conn = this.getConnection();
        return conn && conn.isOpen();
    };

    // Override
    StarDocker.prototype.isAlive = function () {
        var conn = this.getConnection();
        return conn && conn.isAlive();
    };

    // Override
    StarDocker.prototype.getStatus = function () {
        var conn = this.getConnection();
        if (conn) {
            return DockerStatus.getStatus(conn.getState());
        } else {
            return DockerStatus.ERROR;
        }
    };

    /*/
    // Override
    StarDocker.prototype.getLocalAddress = function () {
        var conn = this.getConnection();
        if (conn) {
            return conn.getLocalAddress();
        } else {
            return this.localAddress;
        }
    };
    /*/

    // Override
    StarDocker.prototype.sendShip = function (ship) {
        return this.__dock.appendDeparture(ship);
    };

    // Override
    StarDocker.prototype.processReceived = function (data) {
        // 1. get income ship from received data
        var income = this.getArrival(data);
        if (!income) {
            // waiting for more data
            return;
        }
        // 2. check income ship for response
        income = this.checkArrival(income);
        if (!income) {
            // waiting for more fragment
            return;
        }
        // 3. callback for processing income ship with completed data package
        var delegate = this.getDelegate();
        if (delegate) {
            delegate.onDockerReceived(income, this);
        }
    };

    /**
     *  Get income Ship from received data
     *
     * @param {Uint8Array} data - received data
     * @return {Arrival|Ship} income ship carrying data package/fragment
     */
    // protected
    StarDocker.prototype.getArrival = function (data) {
        ns.assert('implement me!');
        return null;
    };

    /**
     *  Check income ship for responding
     *
     * @param {Arrival|Ship} income - income ship carrying data package/fragment/response
     * @return {Arrival|Ship} income ship carrying completed data package
     */
    // protected
    StarDocker.prototype.checkArrival = function (income) {
        ns.assert('implement me!');
        return null;
    };

    /**
     *  Check and remove linked departure ship with same SN (and page index for fragment)
     *
     * @param {Arrival|Ship} income - income ship with SN
     * @return {Departure|Ship} linked outgo ship
     */
    // protected
    StarDocker.prototype.checkResponse = function (income) {
        // check response for linked departure ship (same SN)
        var linked = this.__dock.checkResponse(income);
        if (!linked) {
            // linked departure task not found, or not finished yet
            return null;
        }
        // all fragments responded, task finished
        var delegate = this.getDelegate();
        if (delegate) {
            delegate.onDockerSent(linked, this);
        }
        return linked;
    };

    /**
     * Check received ship for completed package
     *
     * @param {Arrival|Ship} income - income ship carrying data package (fragment)
     * @return {Arrival|Ship} ship carrying completed data package
     */
    // protected
    StarDocker.prototype.assembleArrival = function (income) {
        return this.__dock.assembleArrival(income);
    };

    /**
     *  Get outgo Ship from waiting queue
     *
     * @param {number} now - current time
     * @return {Departure|Ship} next new or timeout task
     */
    // protected
    StarDocker.prototype.getNextDeparture = function (now) {
        // this will be remove from the queue,
        // if needs retry, the caller should append it back
        return this.__dock.getNextDeparture(now);
    };

    // Override
    StarDocker.prototype.purge = function () {
        this.__dock.purge();
    };

    // Override
    StarDocker.prototype.close = function () {
        removeConnection.call(this);
        this.__dock = null;
    };

    //
    //  Processor
    //

    // Override
    StarDocker.prototype.process = function () {
        // 1. get connection with is ready for sending dadta
        var conn = this.getConnection();
        if (!conn || !conn.isAlive()) {
            // connection not ready now
            return false;
        }
        var delegate;
        var error;
        // 2. get data waiting to be sent out
        var outgo;     // Departure
        var fragments; // Uint8Array[]
        if (this.__lastFragments.length > 0) {
            // get remaining fragments from last outgo task
            outgo = this.__lastOutgo;
            fragments = this.__lastFragments;
            this.__lastOutgo = null;
            this.__lastFragments = [];
        } else {
            // get next outgo task
            var now = (new Date()).getTime();
            outgo = this.getNextDeparture(now);
            if (!outgo) {
                // nothing to do now, return false to let the thread have a rest
                return false;
            } else if (outgo.isFailed(now)) {
                delegate = this.getDelegate();
                if (delegate) {
                    // callback for mission failed
                    error = new Error('Request timeout');
                    delegate.onDockerFailed(error, outgo, this);
                }
                // task timeout, return true to process next one
                return true;
            } else {
                // get fragments from outgo task
                fragments = outgo.getFragments();
                if (fragments.length === 0) {
                    // all fragments of this task have bean sent already,
                    // return true to process next one
                    return true;
                }
            }
        }
        // 3. process fragments of outgo task
        var index = 0;
        var sent = 0;
        try {
            var fra;
            for (var i = 0; i < fragments.length; ++i) {
                fra = fragments[i];
                sent = conn.send(fra);
                if (sent < fra.length) {
                    // buffer overflow?
                    break;
                } else {
                    index += 1;
                    sent = 0;  // clear counter
                }
            }
            if (index < fragments.length) {
                // task failed
                error = new Error('only ' + index + '/' + fragments.length + ' fragments sent.');
            } else {
                // task done
                return true;
            }
        } catch (e) {
            // socket error, callback
            error = e;
        }
        // 4. remove sent fragments
        for (; index > 0; --index) {
            fragments.shift();
        }
        // remove partially sent data of next fragment
        if (sent > 0) {
            var last = fragments.shift();
            var part = last.subarray(sent);
            fragments.unshift(part);
        }
        // 5. store remaining data
        this.__lastOutgo = outgo;
        this.__lastFragments = fragments;
        // 6. callback for error
        delegate = this.getDelegate();
        if (delegate) {
            // delegate.onDockerFailed(error, outgo, this);
            delegate.onDockerError(error, outgo, this);
        }
        return false;
    };

    //-------- namespace --------
    ns.StarDocker = StarDocker;

    ns.registers('StarDocker');

})(StarTrek, MONKEY);
