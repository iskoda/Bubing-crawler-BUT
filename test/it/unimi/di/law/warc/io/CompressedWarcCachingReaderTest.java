package it.unimi.di.law.warc.io;

/*		 
 * Copyright (C) 2013 Paolo Boldi, Massimo Santini, and Sebastiano Vigna 
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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import it.unimi.di.law.warc.io.gzarc.GZIPArchive.FormatException;
import it.unimi.di.law.warc.records.WarcRecord;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.junit.BeforeClass;
import org.junit.Test;

public class CompressedWarcCachingReaderTest {
	
	final static int TEST_RECORDS = 200;
	final static String PATH = "/tmp/warc.gz";

	static int[] position;

	@BeforeClass
	public static void init() throws IOException, InterruptedException {
		final WarcRecord[] randomRecords = RandomReadWritesTest.prepareRndRecords();
		position = RandomReadWritesTest.writeRecords( PATH, TEST_RECORDS, randomRecords, 1 ); // 1 stands for compressed!
	}

	@Test
	public void sequentialReads() throws WarcFormatException, FormatException, IOException {
		FileInputStream input = new FileInputStream( PATH );
		CompressedWarcCachingReader cwc = new CompressedWarcCachingReader( input );
		WarcReader wr;
		int i = 0;
		while ( ( wr = cwc.cache() ) != null ) {
			assertEquals(  position[ i ], RandomReadWritesTest.getPosition( wr.read() ) );
			i++;
		}
		input.close();
	}
	
	@SuppressWarnings("unused")
	private static void consumeRecord( final WarcRecord r ) throws IllegalStateException, IOException {
		if ( r instanceof it.unimi.di.law.warc.records.HttpResponseWarcRecord ) {
			final HttpEntity entity = ((it.unimi.di.law.warc.records.HttpResponseWarcRecord)r).getEntity();
			final InputStream is = entity.getContent();
			final String throwAway = IOUtils.toString( is );
			is.close();
		} else {
			 final Header[] headers = ((it.unimi.di.law.warc.records.HttpRequestWarcRecord)r).getAllHeaders();
			 final String throwAway = Arrays.toString( headers );
		}		
	}
	
	@Test
	public void idempotentReads() throws WarcFormatException, FormatException, IOException {
		FileInputStream input = new FileInputStream( PATH );
		CompressedWarcCachingReader cwc = new CompressedWarcCachingReader( input );
		WarcReader wr;
		int i = 0;
		while ( ( wr = cwc.cache() ) != null ) {
			WarcRecord r = wr.read();
			consumeRecord( r );
			assertEquals(  position[ i ], RandomReadWritesTest.getPosition( r ) );
			r = wr.read();
			consumeRecord( r );
			i++;
		}
		input.close();
	}

	@Test
	public void randomReads() throws WarcFormatException, FormatException, IOException {
		FileInputStream input = new FileInputStream( PATH );
		CompressedWarcCachingReader cwc = new CompressedWarcCachingReader( input );
		final ArrayList<WarcReader> cache = new ArrayList<WarcReader>( position.length );
		WarcReader wr;
		while ( ( wr = cwc.cache() ) != null ) cache.add( wr );
		assertEquals( cache.size(), position.length );
		Collections.shuffle( cache );
		int[] result = new int[ position.length ];
		int i = 0;
		for ( WarcReader rwr : cache ) {
			result[ i++ ] = RandomReadWritesTest.getPosition( rwr.read() );
		}
		Arrays.sort( result );
		int[] sortedPosition = new int[ position.length ];
		System.arraycopy( position, 0, sortedPosition, 0, position.length );
		Arrays.sort( sortedPosition );
		assertArrayEquals( sortedPosition, result );
		input.close();
	}
	
}