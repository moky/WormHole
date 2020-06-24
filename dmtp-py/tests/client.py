#! /usr/bin/env python3
# -*- coding: utf-8 -*-

import random

import sys
import os
import time
from typing import Optional
from weakref import WeakValueDictionary

import stun
import dmtp

curPath = os.path.abspath(os.path.dirname(__file__))
rootPath = os.path.split(curPath)[0]
sys.path.append(rootPath)


SERVER_GZ1 = '134.175.87.98'
SERVER_GZ2 = '203.195.224.155'
SERVER_GZ3 = '129.204.94.164'

# SERVER_HOST = '127.0.0.1'
SERVER_HOST = SERVER_GZ1
SERVER_PORT = 9395

CLIENT_HOST = stun.get_local_ip()
CLIENT_PORT = random.choice(range(9900, 9999))


class Client(dmtp.Client):

    def __init__(self, port: int, host: str='127.0.0.1'):
        super().__init__(host=host, port=port)
        self.server_address = None
        self.identifier = 'moky-%d' % CLIENT_PORT
        self.nat = 'Port Restricted Cone NAT'
        # online contacts
        self.__contacts: dict = {}          # ID -> Contact
        self.__map = WeakValueDictionary()  # (IP, port) -> Contact

    #
    #   Contacts (with locations)
    #

    def get_contact(self, identifier: str=None, address: tuple=None) -> Optional[dmtp.Contact]:
        """ get contact by ID or address """
        if identifier is None:
            assert len(address) == 2, 'address error: %s' % str(address)
            return self.__map.get(address)
        else:
            return self.__contacts.get(identifier)

    def set_location(self, location: dmtp.LocationValue) -> bool:
        """ Login """
        if self.__analyze_location(location=location) < 0:
            print('location error: %s' % location)
            return False
        identifier = location.identifier
        contact = self.get_contact(identifier=identifier)
        if contact is None:
            contact = self._create_contact(identifier=identifier)
        if contact.update_location(location=location):
            self.__contacts[identifier] = contact
            return True

    def remove_location(self, location: dmtp.LocationValue) -> bool:
        """ Logout """
        identifier = location.identifier
        contact = self.get_contact(identifier=identifier)
        if contact is None:
            return False
        assert isinstance(contact, dmtp.Contact), 'contact error: %s' % contact
        if contact.remove_location(location=location):
            if not contact.is_online:
                # all sessions removed/expired
                self.__contacts.pop(identifier, None)
            if location.source_address is not None:
                address = location.source_address
                address = (address.ip, address.port)
                self.__map.pop(address, None)
            if location.mapped_address is not None:
                address = location.mapped_address
                address = (address.ip, address.port)
                self.__map.pop(address, None)
            return True

    def update_address(self, address: tuple, contact: dmtp.Contact) -> bool:
        if contact.update_address(address=address):
            self.__map[address] = contact
            return True

    # noinspection PyMethodMayBeStatic
    def __analyze_location(self, location: dmtp.LocationValue) -> int:
        if location is None:
            # location should not empty
            return -1
        if location.identifier is None:
            # user ID should not empty
            return -2
        if location.mapped_address is None:
            # mapped address should not empty
            return -3
        if location.signature is None:
            # location not signed
            return -4
        # data = "source_address" + "mapped_address" + "relayed_address" + "time"
        data = location.mapped_address.data
        if location.source_address is not None:
            data = location.source_address.data + data
        if location.relayed_address is not None:
            data = data + location.relayed_address.data
        timestamp = dmtp.TimestampValue(value=location.timestamp)
        data += timestamp.data
        signature = location.signature
        # TODO: verify data and signature with public key
        assert data is not None and signature is not None
        return 0

    def __sign_location(self, location: dmtp.LocationValue) -> Optional[dmtp.LocationValue]:
        if location is None or location.identifier is None or location.mapped_address is None:
            # location error
            return None
        # data = "source_address" + "mapped_address" + "relayed_address" + "time"
        mapped_address = location.mapped_address
        data = mapped_address.data
        # source address
        source_ip = self.local_address[0]
        source_port = self.local_address[1]
        source_address = dmtp.SourceAddressValue(ip=source_ip, port=source_port)
        data = source_address.data + data
        # relayed address
        if location.relayed_address is None:
            relayed_address = None
        else:
            relayed_address = location.relayed_address
            data = data + relayed_address.data
        # time
        timestamp = int(time.time())
        data += dmtp.TimestampValue(value=timestamp).data
        # TODO: sign it with private key
        signature = b'sign(' + data + b')'
        return dmtp.LocationValue.new(identifier=location.identifier,
                                      source_address=source_address,
                                      mapped_address=mapped_address,
                                      relayed_address=relayed_address,
                                      timestamp=timestamp, signature=signature, nat=self.nat)

    def say_hi(self, destination: tuple) -> bool:
        sender = self.get_contact(identifier=self.identifier)
        if sender is None:
            location = None
        else:
            location = sender.get_location(address=self.local_address)
        if location is None:
            cmd = dmtp.HelloCommand.new(identifier=self.identifier)
        else:
            cmd = dmtp.HelloCommand.new(location=location)
        print('sending cmd: %s' % cmd)
        self.send_command(cmd=cmd, destination=destination)
        return True

    def sign_in(self, location: dmtp.LocationValue, destination: tuple) -> bool:
        print('server ask signing: %s' % location)
        location = self.__sign_location(location=location)
        if location is None:
            return False
        cmd = dmtp.HelloCommand.new(location=location)
        print('sending cmd: %s' % cmd)
        self.send_command(cmd=cmd, destination=destination)
        self.set_location(location=location)
        return True

    def call(self, identifier: str) -> bool:
        cmd = dmtp.CallCommand.new(identifier=identifier)
        print('sending cmd: %s' % cmd)
        self.send_command(cmd=cmd, destination=self.server_address)
        return True

    def process_message(self, msg: dmtp.Message, source: tuple) -> bool:
        print('received msg from %s:\n\t%s' % (source, msg))
        content = msg.content
        if content is not None:
            print('msg content: "%s"' % content.decode('utf-8'))
        return True


def send_text(receiver: str, msg: str):
    contact = g_client.get_contact(identifier=receiver)
    if contact is None:
        addresses = None
    else:
        addresses = contact.addresses
    if addresses is None or len(addresses) == 0:
        print('user (%s) not login ... %s' % (receiver, contact))
        # ask the server to help building a connection
        g_client.call(identifier=receiver)
        return False
    content = msg.encode('utf-8')
    msg = dmtp.Message.new(info={
        'sender': g_client.identifier,
        'receiver': receiver,
        'time': int(time.time()),
        'data': content,
    })
    for address in addresses:
        print('sending msg to %s:\n\t%s' % (address, msg))
        g_client.send_message(msg=msg, destination=address)


if __name__ == '__main__':
    # create client
    print('UDP client %s -> %s starting ...' % ((CLIENT_HOST, CLIENT_PORT), (SERVER_HOST, SERVER_PORT)))
    g_client = Client(host=CLIENT_HOST, port=CLIENT_PORT)
    g_client.server_address = (SERVER_HOST, SERVER_PORT)
    g_client.start()

    friend = 'moky'

    if len(sys.argv) == 3:
        g_client.identifier = sys.argv[1]
        friend = sys.argv[2]

    # login
    g_client.connect(remote_address=g_client.server_address)

    # test send
    text = '你好 %s！' % friend
    while True:
        time.sleep(5)
        print('----')
        send_text(receiver=friend, msg=text)
