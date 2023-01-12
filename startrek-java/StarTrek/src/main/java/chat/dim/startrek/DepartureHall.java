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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import chat.dim.port.Arrival;
import chat.dim.port.Departure;
import chat.dim.port.Ship;

/**
 *  Memory cache for Departures
 *  ~~~~~~~~~~~~~~~~~~~~~~~~~~~
 */
public class DepartureHall {

    // tasks for sending out
    private final List<Integer> priorities = new ArrayList<>();
    private final Map<Integer, List<Departure>> departureFleets = new HashMap<>();

    private final Map<Object, Departure> departureMap = new WeakHashMap<>();  // ID -> ship
    private final Map<Object, Long> departureFinished = new HashMap<>();      // ID -> timestamp

    /**
     *  Append outgoing ship to a fleet with priority
     *
     * @param outgo - departure task
     * @return false on duplicated
     */
    public boolean appendDeparture(Departure outgo) {
        int priority = outgo.getPriority();
        // 1. choose an array with priority
        List<Departure> fleet = departureFleets.get(priority);
        if (fleet == null) {
            // 1.1. create new array for this priority
            fleet = new ArrayList<>();
            departureFleets.put(priority, fleet);
            // 1.2. insert the priority in a sorted list
            insertPriority(priority);
        } else {
            // 1.3. check duplicated task
            if (fleet.contains(outgo)) {
                return false;
            }
        }
        // 2. append to the tail
        fleet.add(outgo);
        // 3. build mapping if SN exists
        Object sn = outgo.getSN();
        if (sn != null && outgo.isImportant()) {
            // disposable ship needs no response, so
            // we don't build index for it.
            departureMap.put(sn, outgo);
        }
        return true;
    }
    private void insertPriority(int priority) {
        int total = priorities.size();
        int index = 0, value;
        // seeking position for new priority
        for (; index < total; ++index) {
            value = priorities.get(index);
            if (value == priority) {
                // duplicated
                return;
            } else if (value > priority) {
                // got it
                break;
            }
            // current value is smaller than the new value,
            // keep going
        }
        // insert new value before the bigger one
        priorities.add(index, priority);
    }

    /**
     *  Check response from incoming ship
     *
     * @param response - incoming ship with SN
     * @return finished task
     */
    public Departure checkResponse(Arrival response) {
        Object sn = response.getSN();
        assert sn != null : "SN not found: " + response;
        // check whether this task has already finished
        Long time = departureFinished.get(sn);
        if (time != null && time > 0) {
            return null;
        }
        // check departure
        Departure ship = departureMap.get(sn);
        if (ship != null && ship.checkResponse(response)) {
            // all fragments sent, departure task finished
            // remove it and clear mapping when SN exists
            remove(ship, sn);
            // mark finished time
            departureFinished.put(sn, System.currentTimeMillis());
            return ship;
        }
        return null;
    }
    private void remove(Departure ship, Object sn) {
        int priority = ship.getPriority();
        List<Departure> fleet = departureFleets.get(priority);
        if (fleet != null) {
            fleet.remove(ship);
            // remove array when empty
            if (fleet.size() == 0) {
                departureFleets.remove(priority);
            }
        }
        // remove mapping by SN
        departureMap.remove(sn);
    }

    /**
     *  Get next new/timeout task
     *
     * @param now - current time
     * @return departure task
     */
    public Departure getNextDeparture(long now) {
        // task.expired == 0
        Departure next = getNextNewDeparture(now);
        if (next == null) {
            // task.tries > 0 and timeout
            next = getNextTimeoutDeparture(now);
        }
        return next;
    }
    private Departure getNextNewDeparture(long now) {
        List<Departure> fleet;
        Iterator<Departure> dit;
        Departure ship;
        Object sn;
        List<Integer> priorityList = new ArrayList<>(priorities);
        for (int priority : priorityList) {
            // 1. get tasks with priority
            fleet = departureFleets.get(priority);
            if (fleet == null) {
                continue;
            }
            // 2. seeking new task in this priority
            dit = fleet.iterator();
            while (dit.hasNext()) {
                ship = dit.next();
                if (ship.getState(now).equals(Ship.State.NEW)) {
                    if (ship.isImportant()) {
                        // first try, update expired time for response
                        ship.touch(now);
                    } else {
                        // disposable ship needs no response,
                        // remove it immediately.
                        dit.remove(); //fleet.remove(ship);
                        // disposable ship will not be mapped.
                        // see 'appendDeparture()'
                        sn = ship.getSN();
                        if (sn != null) {
                            departureMap.remove(sn);
                        }
                    }
                    return ship;
                }
            }
        }
        return null;
    }
    private Departure getNextTimeoutDeparture(long now) {
        List<Departure> fleet;
        Iterator<Departure> dit;
        Departure ship;
        Ship.State state;
        Object sn;
        List<Integer> priorityList = new ArrayList<>(priorities);
        for (int priority : priorityList) {
            // 1. get tasks with priority
            fleet = departureFleets.get(priority);
            if (fleet == null) {
                continue;
            }
            // 2. seeking timeout task in this priority
            dit = fleet.iterator();
            while (dit.hasNext()) {
                ship = dit.next();
                state = ship.getState(now);
                if (state.equals(Ship.State.TIMEOUT)) {
                    // response timeout, needs retry now.
                    // 2.1. update expired time;
                    ship.touch(now);
                    // 2.2. move to the tail
                    if (fleet.size() > 1) {
                        dit.remove(); //fleet.remove(ship);
                        fleet.add(ship);
                    }
                    return ship;
                } else if (state.equals(Ship.State.FAILED)) {
                    // try too many times and still missing response,
                    // task failed, remove this ship.
                    dit.remove(); //fleet.remove(ship);
                    sn = ship.getSN();
                    if (sn != null) {
                        departureMap.remove(sn);
                    }
                    return ship;
                }
            }
        }
        return null;
    }

    /**
     *  Clear all expired tasks
     */
    public void purge() {
        long now = System.currentTimeMillis();
        // 1. seeking finished tasks
        Iterator<Integer> pit = priorities.iterator();
        int prior;
        List<Departure> fleet;
        Iterator<Departure> fit;
        Departure ship;
        Object sn;
        while (pit.hasNext()) {
            prior = pit.next();
            fleet = departureFleets.get(prior);
            if (fleet == null) {
                continue;
            }
            fit = fleet.iterator();
            while (fit.hasNext()) {
                ship = fit.next();
                if (ship.getState(now).equals(Ship.State.DONE)) {
                    // task done
                    fit.remove();
                    sn = ship.getSN();
                    if (sn != null) {
                        departureMap.remove(sn);
                    }
                    // mark finished time
                    departureFinished.put(sn, now);
                }
            }
            // remove array when empty
            if (fleet.size() == 0) {
                departureFleets.remove(prior);
            }
        }
        // 2. seeking neglected finished times
        Iterator<Map.Entry<Object, Long>> mit = departureFinished.entrySet().iterator();
        long ago = now - 3600 * 1000;
        Map.Entry<Object, Long> entry;
        Long when;
        while (mit.hasNext()) {
            entry = mit.next();
            when = entry.getValue();
            if (when == null || when < ago) {
                // long time ago
                mit.remove();
            }
        }
    }
}
