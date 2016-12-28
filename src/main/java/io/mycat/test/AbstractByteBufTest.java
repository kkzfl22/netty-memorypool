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
import io.mycat.core.buffer.ByteBufProcessor;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ScatteringByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static io.mycat.utils.EmptyArrays.EMPTY_BYTES;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

/**
 * An abstract test class for channel buffers
 */
public abstract class AbstractByteBufTest {

    private static final int CAPACITY = 4096; // Must be even
    private static final int BLOCK_SIZE = 128;

    private long seed;
    private Random random;
    private ByteBuf buffer;

    protected abstract ByteBuf newBuffer(int capacity);
    protected abstract ByteBuf[] components();

    protected boolean discardReadBytesDoesNotMoveWritableBytes() {
        return true;
    }

    @Before
    public void init() {
        buffer = newBuffer(CAPACITY);
        seed = System.currentTimeMillis();
        random = new Random(seed);
    }

    @After
    public void dispose() {
        if (buffer != null) {
            assertThat(buffer.release(), is(true));
            assertThat(buffer.refCnt(), is(0));

            try {
                buffer.release();
            } catch (Exception e) {
                // Ignore.
            }
            buffer = null;
        }
    }

    @Test
    public void initialState() {
        assertEquals(CAPACITY, buffer.capacity());
        assertEquals(0, buffer.readerIndex());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void readerIndexBoundaryCheck1() {
        try {
            buffer.writerIndex(0);
        } catch (IndexOutOfBoundsException e) {
            fail();
        }
        buffer.readerIndex(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void readerIndexBoundaryCheck2() {
        try {
            buffer.writerIndex(buffer.capacity());
        } catch (IndexOutOfBoundsException e) {
            fail();
        }
        buffer.readerIndex(buffer.capacity() + 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void readerIndexBoundaryCheck3() {
        try {
            buffer.writerIndex(CAPACITY / 2);
        } catch (IndexOutOfBoundsException e) {
            fail();
        }
        buffer.readerIndex(CAPACITY * 3 / 2);
    }

    @Test
    public void readerIndexBoundaryCheck4() {
        buffer.writerIndex(0);
        buffer.readerIndex(0);
        buffer.writerIndex(buffer.capacity());
        buffer.readerIndex(buffer.capacity());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void writerIndexBoundaryCheck1() {
        buffer.writerIndex(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void writerIndexBoundaryCheck2() {
        try {
            buffer.writerIndex(CAPACITY);
            buffer.readerIndex(CAPACITY);
        } catch (IndexOutOfBoundsException e) {
            fail();
        }
        buffer.writerIndex(buffer.capacity() + 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void writerIndexBoundaryCheck3() {
        try {
            buffer.writerIndex(CAPACITY);
            buffer.readerIndex(CAPACITY / 2);
        } catch (IndexOutOfBoundsException e) {
            fail();
        }
        buffer.writerIndex(CAPACITY / 4);
    }

    @Test
    public void writerIndexBoundaryCheck4() {
        buffer.writerIndex(0);
        buffer.readerIndex(0);
        buffer.writerIndex(CAPACITY);

        buffer.writeBytes(ByteBuffer.wrap(EMPTY_BYTES));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getBooleanBoundaryCheck1() {
        buffer.getBoolean(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getBooleanBoundaryCheck2() {
        buffer.getBoolean(buffer.capacity());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getByteBoundaryCheck1() {
        buffer.getByte(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getByteBoundaryCheck2() {
        buffer.getByte(buffer.capacity());
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getShortBoundaryCheck1() {
        buffer.getShort(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getShortBoundaryCheck2() {
        buffer.getShort(buffer.capacity() - 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getMediumBoundaryCheck1() {
        buffer.getMedium(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getMediumBoundaryCheck2() {
        buffer.getMedium(buffer.capacity() - 2);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getIntBoundaryCheck1() {
        buffer.getInt(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getIntBoundaryCheck2() {
        buffer.getInt(buffer.capacity() - 3);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getLongBoundaryCheck1() {
        buffer.getLong(-1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getLongBoundaryCheck2() {
        buffer.getLong(buffer.capacity() - 7);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getByteArrayBoundaryCheck1() {
        buffer.getBytes(-1, EMPTY_BYTES);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getByteArrayBoundaryCheck2() {
        buffer.getBytes(-1, EMPTY_BYTES, 0, 0);
    }

    @Test
    public void getByteArrayBoundaryCheck3() {
        byte[] dst = new byte[4];
        buffer.setInt(0, 0x01020304);
        try {
            buffer.getBytes(0, dst, -1, 4);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // Success
        }

        // No partial copy is expected.
        assertEquals(0, dst[0]);
        assertEquals(0, dst[1]);
        assertEquals(0, dst[2]);
        assertEquals(0, dst[3]);
    }

    @Test
    public void getByteArrayBoundaryCheck4() {
        byte[] dst = new byte[4];
        buffer.setInt(0, 0x01020304);
        try {
            buffer.getBytes(0, dst, 1, 4);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // Success
        }

        // No partial copy is expected.
        assertEquals(0, dst[0]);
        assertEquals(0, dst[1]);
        assertEquals(0, dst[2]);
        assertEquals(0, dst[3]);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getByteBufferBoundaryCheck() {
        buffer.getBytes(-1, ByteBuffer.allocate(0));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void copyBoundaryCheck1() {
        buffer.copy(-1, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void copyBoundaryCheck2() {
        buffer.copy(0, buffer.capacity() + 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void copyBoundaryCheck3() {
        buffer.copy(buffer.capacity() + 1, 0);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void copyBoundaryCheck4() {
        buffer.copy(buffer.capacity(), 1);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void setIndexBoundaryCheck1() {
        buffer.setIndex(-1, CAPACITY);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void setIndexBoundaryCheck2() {
        buffer.setIndex(CAPACITY / 2, CAPACITY / 4);
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void setIndexBoundaryCheck3() {
        buffer.setIndex(0, CAPACITY + 1);
    }

    @Test
    public void getByteBufferState() {
        ByteBuffer dst = ByteBuffer.allocate(4);
        dst.position(1);
        dst.limit(3);

        buffer.setByte(0, (byte) 1);
        buffer.setByte(1, (byte) 2);
        buffer.setByte(2, (byte) 3);
        buffer.setByte(3, (byte) 4);
        buffer.getBytes(1, dst);

        assertEquals(3, dst.position());
        assertEquals(3, dst.limit());

        dst.clear();
        assertEquals(0, dst.get(0));
        assertEquals(2, dst.get(1));
        assertEquals(3, dst.get(2));
        assertEquals(0, dst.get(3));
    }

    @Test(expected = IndexOutOfBoundsException.class)
    public void getDirectByteBufferBoundaryCheck() {
        buffer.getBytes(-1, ByteBuffer.allocateDirect(0));
    }

    @Test
    public void getDirectByteBufferState() {
        ByteBuffer dst = ByteBuffer.allocateDirect(4);
        dst.position(1);
        dst.limit(3);

        buffer.setByte(0, (byte) 1);
        buffer.setByte(1, (byte) 2);
        buffer.setByte(2, (byte) 3);
        buffer.setByte(3, (byte) 4);
        buffer.getBytes(1, dst);

        assertEquals(3, dst.position());
        assertEquals(3, dst.limit());

        dst.clear();
        assertEquals(0, dst.get(0));
        assertEquals(2, dst.get(1));
        assertEquals(3, dst.get(2));
        assertEquals(0, dst.get(3));
    }

    @Test
    public void testRandomByteAccess() {
        for (int i = 0; i < buffer.capacity(); i ++) {
            byte value = (byte) random.nextInt();
            buffer.setByte(i, value);
        }

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity(); i ++) {
            byte value = (byte) random.nextInt();
            assertEquals(value, buffer.getByte(i));
        }
    }

    @Test
    public void testRandomUnsignedByteAccess() {
        for (int i = 0; i < buffer.capacity(); i ++) {
            byte value = (byte) random.nextInt();
            buffer.setByte(i, value);
        }

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity(); i ++) {
            int value = random.nextInt() & 0xFF;
            assertEquals(value, buffer.getUnsignedByte(i));
        }
    }

    @Test
    public void testRandomShortAccess() {
        for (int i = 0; i < buffer.capacity() - 1; i += 2) {
            short value = (short) random.nextInt();
            buffer.setShort(i, value);
        }

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity() - 1; i += 2) {
            short value = (short) random.nextInt();
            assertEquals(value, buffer.getShort(i));
        }
    }

    @Test
    public void testRandomUnsignedShortAccess() {
        for (int i = 0; i < buffer.capacity() - 1; i += 2) {
            short value = (short) random.nextInt();
            buffer.setShort(i, value);
        }

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity() - 1; i += 2) {
            int value = random.nextInt() & 0xFFFF;
            assertEquals(value, buffer.getUnsignedShort(i));
        }
    }

    @Test
    public void testRandomMediumAccess() {
        for (int i = 0; i < buffer.capacity() - 2; i += 3) {
            int value = random.nextInt();
            buffer.setMedium(i, value);
        }

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity() - 2; i += 3) {
            int value = random.nextInt() << 8 >> 8;
            assertEquals(value, buffer.getMedium(i));
        }
    }

    @Test
    public void testRandomUnsignedMediumAccess() {
        for (int i = 0; i < buffer.capacity() - 2; i += 3) {
            int value = random.nextInt();
            buffer.setMedium(i, value);
        }

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity() - 2; i += 3) {
            int value = random.nextInt() & 0x00FFFFFF;
            assertEquals(value, buffer.getUnsignedMedium(i));
        }
    }

    @Test
    public void testRandomIntAccess() {
        for (int i = 0; i < buffer.capacity() - 3; i += 4) {
            int value = random.nextInt();
            buffer.setInt(i, value);
        }

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity() - 3; i += 4) {
            int value = random.nextInt();
            assertEquals(value, buffer.getInt(i));
        }
    }

    @Test
    public void testRandomUnsignedIntAccess() {
        for (int i = 0; i < buffer.capacity() - 3; i += 4) {
            int value = random.nextInt();
            buffer.setInt(i, value);
        }

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity() - 3; i += 4) {
            long value = random.nextInt() & 0xFFFFFFFFL;
            assertEquals(value, buffer.getUnsignedInt(i));
        }
    }

    @Test
    public void testRandomLongAccess() {
        for (int i = 0; i < buffer.capacity() - 7; i += 8) {
            long value = random.nextLong();
            buffer.setLong(i, value);
        }

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity() - 7; i += 8) {
            long value = random.nextLong();
            assertEquals(value, buffer.getLong(i));
        }
    }

    @Test
    public void testSetZero() {
        buffer.clear();
        while (buffer.isWritable()) {
            buffer.writeByte((byte) 0xFF);
        }

        for (int i = 0; i < buffer.capacity();) {
            int length = Math.min(buffer.capacity() - i, random.nextInt(32));
            buffer.setZero(i, length);
            i += length;
        }

        for (int i = 0; i < buffer.capacity(); i ++) {
            assertEquals(0, buffer.getByte(i));
        }
    }

    @Test
    public void testSequentialByteAccess() {
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity(); i ++) {
            byte value = (byte) random.nextInt();
            assertEquals(i, buffer.writerIndex());
            assertTrue(buffer.isWritable());
            buffer.writeByte(value);
        }

        assertEquals(0, buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.isWritable());

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity(); i ++) {
            byte value = (byte) random.nextInt();
            assertEquals(i, buffer.readerIndex());
            assertTrue(buffer.isReadable());
            assertEquals(value, buffer.readByte());
        }

        assertEquals(buffer.capacity(), buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.isReadable());
        assertFalse(buffer.isWritable());
    }

    @Test
    public void testSequentialUnsignedByteAccess() {
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity(); i ++) {
            byte value = (byte) random.nextInt();
            assertEquals(i, buffer.writerIndex());
            assertTrue(buffer.isWritable());
            buffer.writeByte(value);
        }

        assertEquals(0, buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.isWritable());

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity(); i ++) {
            int value = random.nextInt() & 0xFF;
            assertEquals(i, buffer.readerIndex());
            assertTrue(buffer.isReadable());
            assertEquals(value, buffer.readUnsignedByte());
        }

        assertEquals(buffer.capacity(), buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.isReadable());
        assertFalse(buffer.isWritable());
    }

    @Test
    public void testSequentialShortAccess() {
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity(); i += 2) {
            short value = (short) random.nextInt();
            assertEquals(i, buffer.writerIndex());
            assertTrue(buffer.isWritable());
            buffer.writeShort(value);
        }

        assertEquals(0, buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.isWritable());

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity(); i += 2) {
            short value = (short) random.nextInt();
            assertEquals(i, buffer.readerIndex());
            assertTrue(buffer.isReadable());
            assertEquals(value, buffer.readShort());
        }

        assertEquals(buffer.capacity(), buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.isReadable());
        assertFalse(buffer.isWritable());
    }

    @Test
    public void testSequentialUnsignedShortAccess() {
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity(); i += 2) {
            short value = (short) random.nextInt();
            assertEquals(i, buffer.writerIndex());
            assertTrue(buffer.isWritable());
            buffer.writeShort(value);
        }

        assertEquals(0, buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.isWritable());

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity(); i += 2) {
            int value = random.nextInt() & 0xFFFF;
            assertEquals(i, buffer.readerIndex());
            assertTrue(buffer.isReadable());
            assertEquals(value, buffer.readUnsignedShort());
        }

        assertEquals(buffer.capacity(), buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.isReadable());
        assertFalse(buffer.isWritable());
    }

    @Test
    public void testSequentialMediumAccess() {
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity() / 3 * 3; i += 3) {
            int value = random.nextInt();
            assertEquals(i, buffer.writerIndex());
            assertTrue(buffer.isWritable());
            buffer.writeMedium(value);
        }

        assertEquals(0, buffer.readerIndex());
        assertEquals(buffer.capacity() / 3 * 3, buffer.writerIndex());
        assertEquals(buffer.capacity() % 3, buffer.writableBytes());

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity() / 3 * 3; i += 3) {
            int value = random.nextInt() << 8 >> 8;
            assertEquals(i, buffer.readerIndex());
            assertTrue(buffer.isReadable());
            assertEquals(value, buffer.readMedium());
        }

        assertEquals(buffer.capacity() / 3 * 3, buffer.readerIndex());
        assertEquals(buffer.capacity() / 3 * 3, buffer.writerIndex());
        assertEquals(0, buffer.readableBytes());
        assertEquals(buffer.capacity() % 3, buffer.writableBytes());
    }

    @Test
    public void testSequentialUnsignedMediumAccess() {
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity() / 3 * 3; i += 3) {
            int value = random.nextInt() & 0x00FFFFFF;
            assertEquals(i, buffer.writerIndex());
            assertTrue(buffer.isWritable());
            buffer.writeMedium(value);
        }

        assertEquals(0, buffer.readerIndex());
        assertEquals(buffer.capacity() / 3 * 3, buffer.writerIndex());
        assertEquals(buffer.capacity() % 3, buffer.writableBytes());

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity() / 3 * 3; i += 3) {
            int value = random.nextInt() & 0x00FFFFFF;
            assertEquals(i, buffer.readerIndex());
            assertTrue(buffer.isReadable());
            assertEquals(value, buffer.readUnsignedMedium());
        }

        assertEquals(buffer.capacity() / 3 * 3, buffer.readerIndex());
        assertEquals(buffer.capacity() / 3 * 3, buffer.writerIndex());
        assertEquals(0, buffer.readableBytes());
        assertEquals(buffer.capacity() % 3, buffer.writableBytes());
    }

    @Test
    public void testSequentialIntAccess() {
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity(); i += 4) {
            int value = random.nextInt();
            assertEquals(i, buffer.writerIndex());
            assertTrue(buffer.isWritable());
            buffer.writeInt(value);
        }

        assertEquals(0, buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.isWritable());

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity(); i += 4) {
            int value = random.nextInt();
            assertEquals(i, buffer.readerIndex());
            assertTrue(buffer.isReadable());
            assertEquals(value, buffer.readInt());
        }

        assertEquals(buffer.capacity(), buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.isReadable());
        assertFalse(buffer.isWritable());
    }

    @Test
    public void testSequentialUnsignedIntAccess() {
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity(); i += 4) {
            int value = random.nextInt();
            assertEquals(i, buffer.writerIndex());
            assertTrue(buffer.isWritable());
            buffer.writeInt(value);
        }

        assertEquals(0, buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.isWritable());

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity(); i += 4) {
            long value = random.nextInt() & 0xFFFFFFFFL;
            assertEquals(i, buffer.readerIndex());
            assertTrue(buffer.isReadable());
            assertEquals(value, buffer.readUnsignedInt());
        }

        assertEquals(buffer.capacity(), buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.isReadable());
        assertFalse(buffer.isWritable());
    }

    @Test
    public void testSequentialLongAccess() {
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity(); i += 8) {
            long value = random.nextLong();
            assertEquals(i, buffer.writerIndex());
            assertTrue(buffer.isWritable());
            buffer.writeLong(value);
        }

        assertEquals(0, buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.isWritable());

        random.setSeed(seed);
        for (int i = 0; i < buffer.capacity(); i += 8) {
            long value = random.nextLong();
            assertEquals(i, buffer.readerIndex());
            assertTrue(buffer.isReadable());
            assertEquals(value, buffer.readLong());
        }

        assertEquals(buffer.capacity(), buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());
        assertFalse(buffer.isReadable());
        assertFalse(buffer.isWritable());
    }

    @Test
    public void testByteArrayTransfer() {
        byte[] value = new byte[BLOCK_SIZE * 2];
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(value);
            buffer.setBytes(i, value, random.nextInt(BLOCK_SIZE), BLOCK_SIZE);
        }

        random.setSeed(seed);
        byte[] expectedValue = new byte[BLOCK_SIZE * 2];
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValue);
            int valueOffset = random.nextInt(BLOCK_SIZE);
            buffer.getBytes(i, value, valueOffset, BLOCK_SIZE);
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j ++) {
                assertEquals(expectedValue[j], value[j]);
            }
        }
    }


    @Test
    public void testSequentialByteArrayTransfer1() {
        byte[] value = new byte[BLOCK_SIZE];
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(value);
            assertEquals(0, buffer.readerIndex());
            assertEquals(i, buffer.writerIndex());
            buffer.writeBytes(value);
        }

        random.setSeed(seed);
        byte[] expectedValue = new byte[BLOCK_SIZE];
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValue);
            assertEquals(i, buffer.readerIndex());
            assertEquals(CAPACITY, buffer.writerIndex());
            buffer.readBytes(value);
            for (int j = 0; j < BLOCK_SIZE; j ++) {
                assertEquals(expectedValue[j], value[j]);
            }
        }
    }

    @Test
    public void testSequentialByteArrayTransfer2() {
        byte[] value = new byte[BLOCK_SIZE * 2];
        buffer.writerIndex(0);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(value);
            assertEquals(0, buffer.readerIndex());
            assertEquals(i, buffer.writerIndex());
            int readerIndex = random.nextInt(BLOCK_SIZE);
            buffer.writeBytes(value, readerIndex, BLOCK_SIZE);
        }

        random.setSeed(seed);
        byte[] expectedValue = new byte[BLOCK_SIZE * 2];
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValue);
            int valueOffset = random.nextInt(BLOCK_SIZE);
            assertEquals(i, buffer.readerIndex());
            assertEquals(CAPACITY, buffer.writerIndex());
            buffer.readBytes(value, valueOffset, BLOCK_SIZE);
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j ++) {
                assertEquals(expectedValue[j], value[j]);
            }
        }
    }

    @Test
    public void testSequentialByteBufferTransfer() {
        buffer.writerIndex(0);
        ByteBuffer value = ByteBuffer.allocate(BLOCK_SIZE * 2);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(value.array());
            value.clear().position(random.nextInt(BLOCK_SIZE));
            value.limit(value.position() + BLOCK_SIZE);
            buffer.writeBytes(value);
        }

        random.setSeed(seed);
        ByteBuffer expectedValue = ByteBuffer.allocate(BLOCK_SIZE * 2);
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            random.nextBytes(expectedValue.array());
            int valueOffset = random.nextInt(BLOCK_SIZE);
            value.clear().position(valueOffset).limit(valueOffset + BLOCK_SIZE);
            buffer.readBytes(value);
            assertEquals(valueOffset + BLOCK_SIZE, value.position());
            for (int j = valueOffset; j < valueOffset + BLOCK_SIZE; j ++) {
                assertEquals(expectedValue.get(j), value.get(j));
            }
        }
    }





    @Test
    public void testWriteZero() {
        try {
            buffer.writeZero(-1);
            fail();
        } catch (IllegalArgumentException e) {
            // Expected
        }

        buffer.clear();
        while (buffer.isWritable()) {
            buffer.writeByte((byte) 0xFF);
        }

        buffer.clear();
        for (int i = 0; i < buffer.capacity();) {
            int length = Math.min(buffer.capacity() - i, random.nextInt(32));
            buffer.writeZero(length);
            i += length;
        }

        assertEquals(0, buffer.readerIndex());
        assertEquals(buffer.capacity(), buffer.writerIndex());

        for (int i = 0; i < buffer.capacity(); i ++) {
            assertEquals(0, buffer.getByte(i));
        }
    }


    @Test
    public void testStreamTransfer1() throws Exception {
        byte[] expected = new byte[buffer.capacity()];
        random.nextBytes(expected);

        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            ByteArrayInputStream in = new ByteArrayInputStream(expected, i, BLOCK_SIZE);
            assertEquals(BLOCK_SIZE, buffer.setBytes(i, in, BLOCK_SIZE));
            assertEquals(-1, buffer.setBytes(i, in, 0));
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            buffer.getBytes(i, out, BLOCK_SIZE);
        }

        assertTrue(Arrays.equals(expected, out.toByteArray()));
    }

    @Test
    public void testStreamTransfer2() throws Exception {
        byte[] expected = new byte[buffer.capacity()];
        random.nextBytes(expected);
        buffer.clear();

        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            ByteArrayInputStream in = new ByteArrayInputStream(expected, i, BLOCK_SIZE);
            assertEquals(i, buffer.writerIndex());
            buffer.writeBytes(in, BLOCK_SIZE);
            assertEquals(i + BLOCK_SIZE, buffer.writerIndex());
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            assertEquals(i, buffer.readerIndex());
            buffer.readBytes(out, BLOCK_SIZE);
            assertEquals(i + BLOCK_SIZE, buffer.readerIndex());
        }

        assertTrue(Arrays.equals(expected, out.toByteArray()));
    }


    @Test
    public void testDuplicate() {
        for (int i = 0; i < buffer.capacity(); i ++) {
            byte value = (byte) random.nextInt();
            buffer.setByte(i, value);
        }

        final int readerIndex = CAPACITY / 3;
        final int writerIndex = CAPACITY * 2 / 3;
        buffer.setIndex(readerIndex, writerIndex);

        // Make sure all properties are copied.
        ByteBuf duplicate = buffer.duplicate();
        assertEquals(buffer.readerIndex(), duplicate.readerIndex());
        assertEquals(buffer.writerIndex(), duplicate.writerIndex());
        assertEquals(buffer.capacity(), duplicate.capacity());
        assertSame(buffer.order(), duplicate.order());
        for (int i = 0; i < duplicate.capacity(); i ++) {
            assertEquals(buffer.getByte(i), duplicate.getByte(i));
        }

        // Make sure the buffer content is shared.
        buffer.setByte(readerIndex, (byte) (buffer.getByte(readerIndex) + 1));
        assertEquals(buffer.getByte(readerIndex), duplicate.getByte(readerIndex));
        duplicate.setByte(1, (byte) (duplicate.getByte(1) + 1));
        assertEquals(buffer.getByte(1), duplicate.getByte(1));
    }

    @Test
    public void testSliceEndianness() throws Exception {
        assertEquals(buffer.order(), buffer.slice(0, buffer.capacity()).order());
        assertEquals(buffer.order(), buffer.slice(0, buffer.capacity() - 1).order());
        assertEquals(buffer.order(), buffer.slice(1, buffer.capacity() - 1).order());
        assertEquals(buffer.order(), buffer.slice(1, buffer.capacity() - 2).order());
    }

    @Test
    public void testSliceIndex() throws Exception {
        assertEquals(0, buffer.slice(0, buffer.capacity()).readerIndex());
        assertEquals(0, buffer.slice(0, buffer.capacity() - 1).readerIndex());
        assertEquals(0, buffer.slice(1, buffer.capacity() - 1).readerIndex());
        assertEquals(0, buffer.slice(1, buffer.capacity() - 2).readerIndex());

        assertEquals(buffer.capacity(), buffer.slice(0, buffer.capacity()).writerIndex());
        assertEquals(buffer.capacity() - 1, buffer.slice(0, buffer.capacity() - 1).writerIndex());
        assertEquals(buffer.capacity() - 1, buffer.slice(1, buffer.capacity() - 1).writerIndex());
        assertEquals(buffer.capacity() - 2, buffer.slice(1, buffer.capacity() - 2).writerIndex());
    }


    @Test
    public void testIndexOf() {
        buffer.clear();
        buffer.writeByte((byte) 1);
        buffer.writeByte((byte) 2);
        buffer.writeByte((byte) 3);
        buffer.writeByte((byte) 2);
        buffer.writeByte((byte) 1);

        assertEquals(-1, buffer.indexOf(1, 4, (byte) 1));
        assertEquals(-1, buffer.indexOf(4, 1, (byte) 1));
        assertEquals(1, buffer.indexOf(1, 4, (byte) 2));
        assertEquals(3, buffer.indexOf(4, 1, (byte) 2));
    }

    @Test
    public void testNioBuffer1() {
        Assume.assumeTrue(buffer.nioBufferCount() == 1);

        byte[] value = new byte[buffer.capacity()];
        random.nextBytes(value);
        buffer.clear();
        buffer.writeBytes(value);

        assertRemainingEquals(ByteBuffer.wrap(value), buffer.nioBuffer());
    }

    @Test
    public void testToByteBuffer2() {
        Assume.assumeTrue(buffer.nioBufferCount() == 1);

        byte[] value = new byte[buffer.capacity()];
        random.nextBytes(value);
        buffer.clear();
        buffer.writeBytes(value);

        for (int i = 0; i < buffer.capacity() - BLOCK_SIZE + 1; i += BLOCK_SIZE) {
            assertRemainingEquals(ByteBuffer.wrap(value, i, BLOCK_SIZE), buffer.nioBuffer(i, BLOCK_SIZE));
        }
    }

    private static void assertRemainingEquals(ByteBuffer expected, ByteBuffer actual) {
        int remaining = expected.remaining();
        int remaining2 = actual.remaining();

        assertEquals(remaining, remaining2);
        byte[] array1 = new byte[remaining];
        byte[] array2 = new byte[remaining2];
        expected.get(array1);
        actual.get(array2);
        assertArrayEquals(array1, array2);
    }

    @Test
    public void testToByteBuffer3() {
        Assume.assumeTrue(buffer.nioBufferCount() == 1);

        assertEquals(buffer.order(), buffer.nioBuffer().order());
    }

    @Test
    public void testSkipBytes1() {
        buffer.setIndex(CAPACITY / 4, CAPACITY / 2);

        buffer.skipBytes(CAPACITY / 4);
        assertEquals(CAPACITY / 4 * 2, buffer.readerIndex());

        try {
            buffer.skipBytes(CAPACITY / 4 + 1);
            fail();
        } catch (IndexOutOfBoundsException e) {
            // Expected
        }

        // Should remain unchanged.
        assertEquals(CAPACITY / 4 * 2, buffer.readerIndex());
    }
    // Test case for https://github.com/netty/netty/issues/325
    @Test
    public void testDiscardAllReadBytes() {
        buffer.writerIndex(buffer.capacity());
        buffer.readerIndex(buffer.writerIndex());
        buffer.discardReadBytes();
    }

    @Test
    public void testForEachByte() {
        buffer.clear();
        for (int i = 0; i < CAPACITY; i ++) {
            buffer.writeByte(i + 1);
        }

        final AtomicInteger lastIndex = new AtomicInteger();
        buffer.setIndex(CAPACITY / 4, CAPACITY * 3 / 4);
        assertThat(buffer.forEachByte(new ByteBufProcessor() {
            int i = CAPACITY / 4;

            @Override
            public boolean process(byte value) throws Exception {
                assertThat(value, is((byte) (i + 1)));
                lastIndex.set(i);
                i ++;
                return true;
            }
        }), is(-1));

        assertThat(lastIndex.get(), is(CAPACITY * 3 / 4 - 1));
    }

    @Test
    public void testForEachByteAbort() {
        buffer.clear();
        for (int i = 0; i < CAPACITY; i ++) {
            buffer.writeByte(i + 1);
        }

        final int stop = CAPACITY / 2;
        assertThat(buffer.forEachByte(CAPACITY / 3, CAPACITY / 3, new ByteBufProcessor() {
            int i = CAPACITY / 3;

            @Override
            public boolean process(byte value) throws Exception {
                assertThat(value, is((byte) (i + 1)));
                if (i == stop) {
                    return false;
                }

                i++;
                return true;
            }
        }), is(stop));
    }

    @Test
    public void testForEachByteDesc() {
        buffer.clear();
        for (int i = 0; i < CAPACITY; i ++) {
            buffer.writeByte(i + 1);
        }

        final AtomicInteger lastIndex = new AtomicInteger();
        assertThat(buffer.forEachByteDesc(CAPACITY / 4, CAPACITY * 2 / 4, new ByteBufProcessor() {
            int i = CAPACITY * 3 / 4 - 1;

            @Override
            public boolean process(byte value) throws Exception {
                assertThat(value, is((byte) (i + 1)));
                lastIndex.set(i);
                i --;
                return true;
            }
        }), is(-1));

        assertThat(lastIndex.get(), is(CAPACITY / 4));
    }

    @Test
    public void testSliceRelease() {
        ByteBuf buf = newBuffer(8);
        assertEquals(1, buf.refCnt());
        assertTrue(buf.slice().release());
        assertEquals(0, buf.refCnt());
    }

    @Test
    public void testDuplicateRelease() {
        ByteBuf buf = newBuffer(8);
        assertEquals(1, buf.refCnt());
        assertTrue(buf.duplicate().release());
        assertEquals(0, buf.refCnt());
    }

    // Test-case trying to reproduce:
    // https://github.com/netty/netty/issues/2843
    @Test
    public void testRefCnt() throws Exception {
        testRefCnt0(false);
    }

    // Test-case trying to reproduce:
    // https://github.com/netty/netty/issues/2843
    @Test
    public void testRefCnt2() throws Exception {
        testRefCnt0(true);
    }


    private void testRefCnt0(final boolean parameter) throws Exception {
        for (int i = 0; i < 10; i++) {
            final CountDownLatch latch = new CountDownLatch(1);
            final CountDownLatch innerLatch = new CountDownLatch(1);

            final ByteBuf buffer = newBuffer(4);
            assertEquals(1, buffer.refCnt());
            final AtomicInteger cnt = new AtomicInteger(Integer.MAX_VALUE);
            Thread t1 = new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean released;
                    if (parameter) {
                        released = buffer.release(buffer.refCnt());
                    } else {
                        released = buffer.release();
                    }
                    assertTrue(released);
                    Thread t2 = new Thread(new Runnable() {
                        @Override
                        public void run() {
                            cnt.set(buffer.refCnt());
                            latch.countDown();
                        }
                    });
                    t2.start();
                    try {
                        // Keep Thread alive a bit so the ThreadLocal caches are not freed
                        innerLatch.await();
                    } catch (InterruptedException ignore) {
                        // ignore
                    }
                }
            });
            t1.start();

            latch.await();
            assertEquals(0, cnt.get());
            innerLatch.countDown();
        }
    }

    static final class TestGatheringByteChannel implements GatheringByteChannel {
        private final ByteArrayOutputStream out = new ByteArrayOutputStream();
        private final WritableByteChannel channel = Channels.newChannel(out);
        private final int limit;
        TestGatheringByteChannel(int limit) {
            this.limit = limit;
        }

        TestGatheringByteChannel() {
            this(Integer.MAX_VALUE);
        }

        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) throws IOException {
            long written = 0;
            for (; offset < length; offset++) {
                written += write(srcs[offset]);
                if (written >= limit) {
                    break;
                }
            }
            return written;
        }

        @Override
        public long write(ByteBuffer[] srcs) throws IOException {
            return write(srcs, 0, srcs.length);
        }

        @Override
        public int write(ByteBuffer src) throws IOException {
            int oldLimit = src.limit();
            if (limit < src.remaining()) {
                src.limit(src.position() + limit);
            }
            int w = channel.write(src);
            src.limit(oldLimit);
            return w;
        }

        @Override
        public boolean isOpen() {
            return channel.isOpen();
        }

        @Override
        public void close() throws IOException {
            channel.close();
        }

        public byte[] writtenBytes() {
            return out.toByteArray();
        }
    }

    private static final class DevNullGatheringByteChannel implements GatheringByteChannel {
        @Override
        public long write(ByteBuffer[] srcs, int offset, int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long write(ByteBuffer[] srcs) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int write(ByteBuffer src) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TestScatteringByteChannel implements ScatteringByteChannel {
        @Override
        public long read(ByteBuffer[] dsts, int offset, int length) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long read(ByteBuffer[] dsts) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int read(ByteBuffer dst) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isOpen() {
            return false;
        }

        @Override
        public void close() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class TestByteBufProcessor implements ByteBufProcessor {
        @Override
        public boolean process(byte value) throws Exception {
            return true;
        }
    }
}
