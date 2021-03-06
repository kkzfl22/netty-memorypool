/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.mycat.test;

import io.mycat.core.buffer.ByteBuf;
import io.mycat.core.buffer.PooledByteBufAllocator;

import java.nio.ByteOrder;

import static org.junit.Assert.assertEquals;

/**
 * Tests little-endian heap channel buffers
 */
public class PooledLittleEndianHeapByteBufTest extends AbstractByteBufTest {

    private ByteBuf buffer;

    @Override
    protected ByteBuf newBuffer(int length) {
        buffer = PooledByteBufAllocator.DEFAULT.heapBuffer(length).order(ByteOrder.LITTLE_ENDIAN);
        assertEquals(0, buffer.writerIndex());
        return buffer;
    }

    @Override
    protected ByteBuf[] components() {
        return new ByteBuf[] { buffer };
    }
}
