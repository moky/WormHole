#! /usr/bin/env python3
# -*- coding: utf-8 -*-

import sys
import os
from typing import Optional

curPath = os.path.abspath(os.path.dirname(__file__))
rootPath = os.path.split(curPath)[0]
sys.path.append(rootPath)

from udp import Channel, Connection
from udp import Gate, GateDelegate, GateStatus
from udp import Hub, ServerHub
from udp import Arrival, PackageArrival, Departure, PackageDeparture

from tests.stargate import UDPGate


class UDPServerHub(ServerHub):

    # Override
    def _get_channel(self, remote: Optional[tuple], local: Optional[tuple]) -> Optional[Channel]:
        channel = super()._get_channel(remote=remote, local=local)
        if channel is None:
            channel = super()._get_channel(remote=None, local=local)
        return channel

    # Override
    def _get_connection(self, remote: tuple, local: Optional[tuple]) -> Optional[Connection]:
        return super()._get_connection(remote=remote, local=None)

    # Override
    def _set_connection(self, remote: tuple, local: Optional[tuple], connection: Connection):
        super()._set_connection(remote=remote, local=None, connection=connection)

    # Override
    def _remove_connection(self, remote: tuple, local: Optional[tuple], connection: Optional[Connection]):
        super()._remove_connection(remote=remote, local=None, connection=connection)


class Server(GateDelegate):

    def __init__(self, host: str, port: int):
        super().__init__()
        self.__local_address = (host, port)
        gate = UDPGate(delegate=self, daemonic=False)
        gate.hub = UDPServerHub(delegate=gate)
        self.__gate = gate

    @property
    def local_address(self) -> tuple:
        return self.__local_address

    @property
    def gate(self) -> UDPGate:
        return self.__gate

    @property
    def hub(self) -> ServerHub:
        return self.gate.hub

    def start(self):
        self.hub.bind(address=self.local_address)
        self.gate.start()

    def send(self, data: bytes, destination: tuple):
        self.gate.send_command(body=data, source=self.local_address, destination=destination)

    #
    #   Gate Delegate
    #

    # Override
    def gate_status_changed(self, previous: GateStatus, current: GateStatus,
                            remote: tuple, local: Optional[tuple], gate: Gate):
        UDPGate.info('!!! connection (%s, %s) state changed: %s -> %s' % (remote, local, previous, current))

    # Override
    def gate_received(self, ship: Arrival,
                      source: tuple, destination: Optional[tuple], connection: Connection):
        assert isinstance(ship, PackageArrival), 'arrival ship error: %s' % ship
        pack = ship.package
        data = pack.body.get_bytes()
        try:
            text = data.decode('utf-8')
        except UnicodeDecodeError as error:
            UDPGate.error(msg='failed to decode data: %s, %s' % (error, data))
            text = str(data)
        UDPGate.info('<<< received (%d bytes) from %s: %s' % (len(data), source, text))
        text = '%d# %d byte(s) received' % (self.counter, len(data))
        self.counter += 1
        UDPGate.info('>>> responding: %s' % text)
        data = text.encode('utf-8')
        self.send(data=data, destination=source)

    counter = 0

    # Override
    def gate_sent(self, ship: Departure,
                  source: Optional[tuple], destination: tuple, connection: Connection):
        assert isinstance(ship, PackageDeparture), 'departure ship error: %s' % ship
        pack = ship.package
        data = pack.body.get_bytes()
        size = len(data)
        UDPGate.info('message sent: %d byte(s) to %s' % (size, destination))

    # Override
    def gate_error(self, error: IOError, ship: Departure,
                   source: Optional[tuple], destination: tuple, connection: Connection):
        UDPGate.error('gate error (%s, %s): %s' % (source, destination, error))


SERVER_HOST = Hub.inet_address()
# SERVER_HOST = '0.0.0.0'
SERVER_PORT = 9394


if __name__ == '__main__':

    UDPGate.info('UDP server (%s:%d) starting ...' % (SERVER_HOST, SERVER_PORT))

    g_server = Server(host=SERVER_HOST, port=SERVER_PORT)

    g_server.start()
