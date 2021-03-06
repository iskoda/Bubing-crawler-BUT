package it.unimi.di.law.bubing.util;

/*		 
 * Copyright (C) 2004-2013 Paolo Boldi, Massimo Santini, and Sebastiano Vigna 
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 3 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 *  for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, see <http://www.gnu.org/licenses/>.
 *
 */

//RELEASE-STATUS: DIST

import static org.junit.Assert.assertEquals;
import it.unimi.dsi.fastutil.bytes.ByteArrays;
import it.unimi.dsi.fastutil.io.BinIO;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import it.unimi.dsi.fastutil.objects.Object2IntOpenCustomHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.util.XorShift1024StarRandom;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Test;

public class ConcurrentCountingMapTest {
	@Test
	public void test() {
		ConcurrentCountingMap map = new ConcurrentCountingMap( 4 );
		assertEquals( 0, map.addTo( new byte[ 1 ], 1 ) );
		assertEquals( 1, map.addTo( new byte[ 1 ], 1 ) );
		assertEquals( 2, map.get( new byte[] { 1, 0, 1 }, 1, 1 ) );
		assertEquals( 0, map.addTo( new byte[ 0 ], 3 ) );
		assertEquals( 3, map.put( new byte[ 0 ], 10 ) );
	}

	@Test
	public void testLarge() throws IOException, ClassNotFoundException {
		ConcurrentCountingMap map = new ConcurrentCountingMap( 4 );
		XorShift1024StarRandom random = new XorShift1024StarRandom( 0 );
		Object2IntOpenCustomHashMap<byte[]> hashMap = new Object2IntOpenCustomHashMap<byte[]>( ByteArrays.HASH_STRATEGY );
		for( int i = 0; i < 1000000; i++ ) {
			final int length = random.nextInt( 100 );
			final int offset = random.nextInt( 3 );
			final int padding = random.nextInt( 3 );
			byte[] key = new byte[ offset + length + padding ];
			for( int p = key.length; p-- != 0; ) key[ p ] = (byte)random.nextInt( 4 );
			final byte[] exactKey = Arrays.copyOfRange( key, offset, offset + length );
			switch( random.nextInt( 3 ) ) {
			case 0:
				final int delta = random.nextInt( 3 ) + 1;
				assertEquals( hashMap.addTo( exactKey, delta ), map.addTo( key, offset, length, delta ) );
				break;
			case 1:
				final int value = random.nextInt( 3 ) + 1;
				assertEquals( hashMap.put( exactKey, value ), map.put( key, offset, length, value ) );
				break;
			case 2:
				assertEquals( hashMap.getInt( exactKey ), map.get( key, offset, length ) );
			}
		}

		for( ObjectIterator<Entry<byte[]>> iterator = hashMap.object2IntEntrySet().fastIterator(); iterator.hasNext(); ) {
			final Entry<byte[]> next = iterator.next();
			assertEquals( Arrays.toString( next.getKey() ), next.getIntValue(), map.get( next.getKey() ) );
		}

		File temp = File.createTempFile( ConcurrentCountingMap.class.getSimpleName() + "-", "-temp" );
		temp.deleteOnExit();
		BinIO.storeObject( map, temp );
		map = (ConcurrentCountingMap)BinIO.loadObject( temp );
		for( ObjectIterator<Entry<byte[]>> iterator = hashMap.object2IntEntrySet().fastIterator(); iterator.hasNext(); ) {
			final Entry<byte[]> next = iterator.next();
			assertEquals( Arrays.toString( next.getKey() ), next.getIntValue(), map.get( next.getKey() ) );
		}
		
		temp.delete();
	}

}
