# -*- coding: utf-8 -*-
#
#   Star Trek: Interstellar Transport
#
#                                Written in 2021 by Moky <albert.moky@gmail.com>
#
# ==============================================================================
# MIT License
#
# Copyright (c) 2021 Albert Moky
#
# Permission is hereby granted, free of charge, to any person obtaining a copy
# of this software and associated documentation files (the "Software"), to deal
# in the Software without restriction, including without limitation the rights
# to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
# copies of the Software, and to permit persons to whom the Software is
# furnished to do so, subject to the following conditions:
#
# The above copyright notice and this permission notice shall be included in all
# copies or substantial portions of the Software.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
# AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
# LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
# OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
# SOFTWARE.
# ==============================================================================

from .net import Hub
from .net import Channel, ChannelStatus
from .net import Connection, ConnectionState, ConnectionDelegate

from .socket import BaseChannel
from .socket import BaseHub, BaseConnection, ActiveConnection

from .port import Ship, Arrival, Departure
from .port import ShipStatus, DeparturePriority
from .port import Porter, PorterStatus, PorterDelegate
from .port import Gate

from .arrival import ArrivalShip, ArrivalHall
from .departure import DepartureShip, DepartureHall
from .dock import Dock, LockedDock
from .stardocker import StarPorter
from .stargate import StarGate

name = "StarTrek"

__author__ = 'Albert Moky'

__all__ = [

    'Hub',
    'Channel', 'ChannelStatus',
    'Connection', 'ConnectionState', 'ConnectionDelegate',

    'BaseChannel',
    'BaseHub', 'BaseConnection', 'ActiveConnection',

    'Ship', 'Arrival', 'Departure',
    'ShipStatus', 'DeparturePriority',
    'Porter', 'PorterStatus', 'PorterDelegate',
    'Gate',

    'ArrivalShip', 'ArrivalHall', 'DepartureShip', 'DepartureHall',
    'Dock', 'LockedDock',
    'StarPorter', 'StarGate',
]
