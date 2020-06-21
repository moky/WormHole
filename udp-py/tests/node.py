# -*- coding: utf-8 -*-

from typing import Optional

import udp


class Peer(udp.Peer, udp.HubListener):

    #
    #   HubListener
    #
    def received(self, data: bytes, source: tuple, destination: tuple) -> Optional[bytes]:
        task = udp.Arrival(payload=data, source=source, destination=destination)
        self.pool.add_arrival(task=task)
        return None


class Node(udp.PeerDelegate):

    def __init__(self, host: str, port: int):
        super().__init__()
        self.__local_address = (host, port)
        self.__peer = self._create_peer()
        self.__hub = self._create_hub(host=host, port=port)

    @property
    def local_address(self) -> tuple:
        return self.__local_address

    @property
    def peer(self) -> Peer:
        return self.__peer

    def _create_peer(self) -> Peer:
        peer = Peer()
        peer.delegate = self
        peer.start()
        return peer

    @property
    def hub(self) -> udp.Hub:
        return self.__hub

    @hub.setter
    def hub(self, value: udp.Hub):
        peer = self.peer
        if self.__hub is not None:
            self.__hub.remove_listener(peer)
        value.add_listener(peer)
        self.__hub = value

    def _create_hub(self, host: str, port: int) -> udp.Hub:
        hub = udp.Hub()
        hub.open(host=host, port=port)
        hub.add_listener(self.peer)
        hub.start()
        return hub

    def stop(self):
        self.__peer.stop()
        self.__hub.remove_listener(self.__peer)
        self.__hub.stop()

    def send_message(self, msg: bytes, destination: tuple) -> udp.Departure:
        return self.peer.send_message(pack=msg, destination=destination, source=self.local_address)

    def send_command(self, cmd: bytes, destination: tuple) -> udp.Departure:
        return self.peer.send_command(pack=cmd, destination=destination, source=self.local_address)

    #
    #   PeerDelegate
    #
    def send_data(self, data: bytes, destination: tuple, source: tuple) -> int:
        return self.__hub.send(data=data, destination=destination, source=source)

    def received_command(self, cmd: bytes, source: tuple, destination: tuple) -> bool:
        print('received cmd (%d bytes) from %s to %s: %s' % (len(cmd), source, destination, cmd))
        return True

    def received_message(self, msg: bytes, source: tuple, destination: tuple) -> bool:
        print('received msg (%d bytes) from %s to %s: %s' % (len(msg), source, destination, msg))
        return True
