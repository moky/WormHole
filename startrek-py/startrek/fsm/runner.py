# -*- coding: utf-8 -*-
#
#   Finite State Machine
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

import asyncio
import weakref
from abc import ABC, abstractmethod
from typing import Optional


class Processor(ABC):

    @abstractmethod
    async def process(self) -> bool:
        """
        Do the job

        :return: False on nothing to do
        """
        raise NotImplemented


class Handler(ABC):

    @abstractmethod
    async def setup(self):
        """ Prepare for Handling """
        raise NotImplemented
    
    @abstractmethod
    async def handle(self):
        """ Handling run loop """
        raise NotImplemented

    @abstractmethod
    async def finish(self):
        """ Cleanup after handled """
        raise NotImplemented


class Runnable(ABC):

    @abstractmethod
    async def run(self):
        """ Run in a thread """
        raise NotImplemented


# noinspection PyAbstractClass
class Runner(Runnable, Handler, Processor, ABC):
    """
        Runner
        ~~~~~~

        @abstract method:
            - process()
    """

    # Frames Per Second
    # ~~~~~~~~~~~~~~~~~
    # (1) The human eye can process 10-12 still images per second,
    #     and the dynamic compensation function can also deceive us.
    # (2) At a frame rate of 12fps or lower, we can quickly distinguish between
    #     a pile of still images and not animations.
    # (3) Once the playback rate (frames per second) of the images reaches 16-24 fps,
    #     our brain will assume that these images are a continuously moving scene
    #     and will appear like the effect of a movie.
    # (4) At 24fps, there is a feeling of 'motion blur',
    #     while at 60fps, the image is the smoothest and cleanest.
    INTERVAL_SLOW = 1.0/10
    INTERVAL_NORMAL = 1.0/25
    INTERVAL_FAST = 1.0/60

    def __init__(self, interval: float):
        super().__init__()
        assert interval > 0, 'interval error: %s' % interval
        self.__interval = interval
        self.__running = False

    @property
    def interval(self) -> float:
        return self.__interval

    @property
    def running(self) -> bool:
        return self.__running

    async def stop(self):
        self.__running = False

    # Override
    async def run(self):
        await self.setup()
        try:
            await self.handle()
        finally:
            await self.finish()

    # Override
    async def setup(self):
        self.__running = True

    # Override
    async def finish(self):
        self.__running = False

    # Override
    async def handle(self):
        while self.running:
            if await self.process():
                # runner is busy, return True to go on.
                pass
            else:
                # if nothing to do now, return False here
                # to let the thread have a rest.
                await self._idle()

    # protected
    async def _idle(self):
        await self.sleep(seconds=self.interval)
        # time.sleep(self.interval)

    @classmethod
    async def sleep(cls, seconds: float):
        await asyncio.sleep(seconds)

    @classmethod
    def async_run(cls, coro):
        # asyncio.run(main)
        asyncio.create_task(coro)


class Daemon:

    def __init__(self, target: Runnable):
        super().__init__()
        self.__target = weakref.ref(target)

    @property  # private
    def target(self) -> Optional[Runnable]:
        ref = self.__target
        if ref is not None:
            return ref()

    def start(self):
        target = self.target
        if target is not None:
            Runner.async_run(coro=target.run())
