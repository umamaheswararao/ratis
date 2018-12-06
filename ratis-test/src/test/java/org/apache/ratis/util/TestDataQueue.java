/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.ratis.util;

import org.apache.ratis.util.function.TriConsumer;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.concurrent.TimeoutException;

public class TestDataQueue {
  static <T> TriConsumer<T, TimeDuration, TimeoutException> getTimeoutHandler(boolean expctedTimeout) {
    return (element, time, exception) -> {
      if (!expctedTimeout) {
        throw new AssertionError("Unexpected timeout to get element " + element + " in " + time, exception);
      }
    };
  }

  private void assertSizes(int expectedNumElements, int expectedNumBytes) {
    Assert.assertEquals(expectedNumElements, q.getNumElements());
    Assert.assertEquals(expectedNumBytes, q.getNumBytes());
  }

  final SizeInBytes byteLimit = SizeInBytes.valueOf(100);
  final int elementLimit = 5;
  final DataQueue<Integer> q = new DataQueue<>(null, byteLimit, elementLimit, Integer::intValue);

  @Test(timeout = 1000)
  public void testElementLimit() {
    assertSizes(0, 0);

    int numBytes = 0;
    for (int i = 0; i < elementLimit; i++) {
      Assert.assertEquals(i, q.getNumElements());
      Assert.assertEquals(numBytes, q.getNumBytes());
      final boolean offered = q.offer(i);
      Assert.assertTrue(offered);
      numBytes += i;
      assertSizes(i+1, numBytes);
    }
    {
      final boolean offered = q.offer(0);
      Assert.assertFalse(offered);
      assertSizes(elementLimit, numBytes);
    }

    { // poll all elements
      final List<Integer> polled = q.pollList(100, (i, timeout) -> i, getTimeoutHandler(false));
      Assert.assertEquals(elementLimit, polled.size());
      for (int i = 0; i < polled.size(); i++) {
        Assert.assertEquals(i, polled.get(i).intValue());
      }
    }
    assertSizes(0, 0);
  }

  @Test(timeout = 1000)
  public void testByteLimit() {
    assertSizes(0, 0);

    try {
      q.offer(byteLimit.getSizeInt() + 1);
      Assert.fail();
    } catch (IllegalStateException ignored) {
    }

    final int halfBytes = byteLimit.getSizeInt() / 2;
    {
      final boolean offered = q.offer(halfBytes);
      Assert.assertTrue(offered);
      assertSizes(1, halfBytes);
    }

    {
      final boolean offered = q.offer(halfBytes + 1);
      Assert.assertFalse(offered);
      assertSizes(1, halfBytes);
    }

    {
      final boolean offered = q.offer(halfBytes);
      Assert.assertTrue(offered);
      assertSizes(2, byteLimit.getSizeInt());
    }

    {
      final boolean offered = q.offer(1);
      Assert.assertFalse(offered);
      assertSizes(2, byteLimit.getSizeInt());
    }

    {
      final boolean offered = q.offer(0);
      Assert.assertTrue(offered);
      assertSizes(3, byteLimit.getSizeInt());
    }

    { // poll all elements
      final List<Integer> polled = q.pollList(100, (i, timeout) -> i, getTimeoutHandler(false));
      Assert.assertEquals(3, polled.size());
      Assert.assertEquals(halfBytes, polled.get(0).intValue());
      Assert.assertEquals(halfBytes, polled.get(1).intValue());
      Assert.assertEquals(0, polled.get(2).intValue());
    }

    assertSizes(0, 0);
  }

  @Test(timeout = 1000)
  public void testTimeout() {
    assertSizes(0, 0);

    int numBytes = 0;
    for (int i = 0; i < elementLimit; i++) {
      Assert.assertEquals(i, q.getNumElements());
      Assert.assertEquals(numBytes, q.getNumBytes());
      final boolean offered = q.offer(i);
      Assert.assertTrue(offered);
      numBytes += i;
      assertSizes(i+1, numBytes);
    }

    { // poll with zero time
      final List<Integer> polled = q.pollList(0, (i, timeout) -> i, getTimeoutHandler(false));
      Assert.assertTrue(polled.isEmpty());
      assertSizes(elementLimit, numBytes);
    }

    final int halfElements = elementLimit / 2;
    { // poll with timeout
      final List<Integer> polled = q.pollList(100, (i, timeout) -> {
        if (i == halfElements) {
          // simulate timeout
          throw new TimeoutException("i=" + i);
        }
        return i;
      }, getTimeoutHandler(true));
      Assert.assertEquals(halfElements, polled.size());
      for (int i = 0; i < polled.size(); i++) {
        Assert.assertEquals(i, polled.get(i).intValue());
        numBytes -= i;
      }
      assertSizes(elementLimit - halfElements, numBytes);
    }

    { // poll the remaining elements
      final List<Integer> polled = q.pollList(100, (i, timeout) -> i, getTimeoutHandler(false));
      Assert.assertEquals(elementLimit - halfElements, polled.size());
      for (int i = 0; i < polled.size(); i++) {
        Assert.assertEquals(halfElements + i, polled.get(i).intValue());
      }
    }
    assertSizes(0, 0);
  }
}