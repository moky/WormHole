# -*- coding: utf-8 -*-
#
#   Finite State Machine
#
#                                Written in 2022 by Moky <albert.moky@gmail.com>
#
# ==============================================================================
# MIT License
#
# Copyright (c) 2022 Albert Moky
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

import threading
import time
import traceback
from weakref import WeakSet
from abc import ABC, abstractmethod
from typing import Set

from .runner import Runner, Daemon


class Ticker(ABC):

    @abstractmethod
    async def tick(self, now: float, elapsed: float):
        """
        Drive current thread forward

        :param now:     current time (seconds from Jan 1, 1970 UTC)
        :param elapsed: seconds from previous tick
        """
        raise NotImplemented


class Metronome(Runner):

    # at least wait 1/60 of a second
    MIN_INTERVAL = 1.0/60

    def __init__(self, interval: float):
        super().__init__(interval=interval)
        self.__last_time = 0
        self.__daemon = Daemon(target=self)
        self.__lock = threading.Lock()
        self.__tickers = WeakSet()

    # Override
    async def setup(self):
        await super().setup()
        self.__last_time = time.time()

    # Override
    async def process(self) -> bool:
        tickers = self.tickers
        if len(tickers) == 0:
            # nothing to do now,
            # return False to have a rest ^_^
            return False
        # 1. check time
        now = time.time()
        elapsed = now - self.__last_time
        waiting = self.interval - elapsed
        if waiting < self.MIN_INTERVAL:
            waiting = self.MIN_INTERVAL
        await self.sleep(seconds=waiting)
        now += waiting
        elapsed += waiting
        # 2. drive tickers
        for item in tickers:
            try:
                await item.tick(now=now, elapsed=elapsed)
            except Exception as error:
                await self.on_error(error=error, ticker=item)
        # 3. update last time
        self.__last_time = now
        return True

    # noinspection PyMethodMayBeStatic
    async def on_error(self, error: Exception, ticker: Ticker):
        print('[Metronome] drive ticker error: %s, %s' % (error, ticker))
        traceback.print_exc()

    @property  # private
    def tickers(self) -> Set[Ticker]:
        """ get all tickers """
        with self.__lock:
            return set(self.__tickers)

    def add_ticker(self, ticker: Ticker):
        with self.__lock:
            self.__tickers.add(ticker)

    def remove_ticker(self, ticker: Ticker):
        with self.__lock:
            self.__tickers.discard(ticker)

    def start(self):
        # await self.run()
        self.__daemon.start()

    # Override
    async def stop(self):
        await super().stop()
        # await self._idle()
        await self.sleep(seconds=self.interval * 2)
        self.__daemon.stop()

#
#   Singleton for Prime Metronome
#


class Singleton(object):

    __instances = {}

    def __init__(self, cls):
        self.__cls = cls

    def __call__(self, *args, **kwargs):
        cls = self.__cls
        instance = self.__instances.get(cls, None)
        if instance is None:
            instance = cls(*args, **kwargs)
            self.__instances[cls] = instance
        return instance

    def __getattr__(self, key):
        return getattr(self.__cls, key, None)


@Singleton
class PrimeMetronome:

    def __init__(self):
        super().__init__()
        metronome = Metronome(interval=Runner.INTERVAL_SLOW)
        metronome.start()
        self.__metronome = metronome

    def add_ticker(self, ticker: Ticker):
        self.__metronome.add_ticker(ticker=ticker)

    def remove_ticker(self, ticker: Ticker):
        self.__metronome.remove_ticker(ticker=ticker)
