package it.unimi.di.law.warc.io.gzarc;

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
import it.unimi.di.law.bubing.util.Util;
import it.unimi.di.law.warc.io.gzarc.GZIPArchive.ReadEntry;
import it.unimi.di.law.warc.io.gzarc.GZIPArchive.ReadEntry.LazyInflater;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.longs.LongBigArrayBigList;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import org.apache.commons.lang.RandomStringUtils;
import org.junit.Test;

import com.google.common.io.ByteStreams;
import com.martiansoftware.jsap.JSAP;
import com.martiansoftware.jsap.JSAPException;
import com.martiansoftware.jsap.JSAPResult;
import com.martiansoftware.jsap.Parameter;
import com.martiansoftware.jsap.SimpleJSAP;
import com.martiansoftware.jsap.Switch;
import com.martiansoftware.jsap.UnflaggedOption;

public class GZIPArchiveWriterTest {

	@Test
	public void smallEntry() throws IOException {
		FileOutputStream fos = new FileOutputStream( "/tmp/archive.gz" );
		GZIPArchiveWriter gzaw = new GZIPArchiveWriter( fos );
		GZIPArchive.WriteEntry we;
		for ( int i = 0; i < 10; i++ ) {
			we = gzaw.getEntry( "Test " + i, "Comment " + i, new Date() );
			we.deflater.write( Util.toByteArray( "Hello, world " + i + "!\n" ) );
			we.deflater.close();
			System.out.println( we );
		}
		gzaw.close();
		
		FileInputStream fis = new FileInputStream( "/tmp/archive.gz" );
		GZIPArchiveReader gzar = new GZIPArchiveReader( fis );
		GZIPArchive.ReadEntry re;
		for ( ;; ) {
			re = gzar.getEntry();
			if ( re == null ) break;
			LazyInflater lin = re.lazyInflater;
			System.out.print( Util.toString( ByteStreams.toByteArray( lin.get() ) ) );
			lin.consume();
			System.out.println( re );
		}
		fis.close();
	}

	@Test
	public void skip() throws IOException {
		FileOutputStream fos = new FileOutputStream( "/tmp/archive.gz" );
		GZIPArchiveWriter gzaw = new GZIPArchiveWriter( fos );
		GZIPArchive.WriteEntry we;
		for ( int i = 0; i < 10; i++ ) {
			we = gzaw.getEntry( "Test " + i, "Comment " + i, new Date() );
			we.deflater.write( Util.toByteArray( "Hello, world " + i + "!\n" ) );
			we.deflater.close();
		}
		gzaw.close();
		
		final LongBigArrayBigList pos = GZIPIndexer.index( new FileInputStream( "/tmp/archive.gz" ) );
				
		GZIPArchive.ReadEntry re;
		FastBufferedInputStream fis = new FastBufferedInputStream( new FileInputStream( "/tmp/archive.gz" ) );
		GZIPArchiveReader gzar = new GZIPArchiveReader( fis );
		for ( int i = (int)pos.size64() - 1; i >= 0; i-- ) {
			gzar.position( pos.getLong( i ) );
			re = gzar.getEntry();
			if ( re == null ) break;
			System.out.println( re );
		}
		fis.close();
		
	}

	@Test
	public void randomEntry() throws IOException {
		
		byte[] expectedMagic = Util.toByteArray( "MAGIC" );
		byte[] actualMagic = new byte[ expectedMagic.length ];
		
		FileOutputStream fos = new FileOutputStream( "/tmp/archive.gz" );
		GZIPArchiveWriter gzaw = new GZIPArchiveWriter( fos );
		GZIPArchive.WriteEntry we;
		for ( int i = 0; i < 10; i++ ) {
			we = gzaw.getEntry( "Test " + i, "Comment " + i, new Date() );
			we.deflater.write( expectedMagic );
			we.deflater.write( Util.toByteArray( RandomStringUtils.randomAscii( 1024 * ( 1 + i ) ) ) );
			we.deflater.close();
			System.out.println( we );
		}
		gzaw.close();		
		
		FileInputStream fis = new FileInputStream( "/tmp/archive.gz" );
		GZIPArchiveReader gzar = new GZIPArchiveReader( fis );
		InputStream in;
		
		GZIPArchive.ReadEntry re;
		for ( int i = 0; i < 11; i++ ) {
			re = gzar.getEntry();
			if ( re == null ) break;
			LazyInflater lin = re.lazyInflater;
			in = lin.get();
			in.read( actualMagic );
			assertArrayEquals( expectedMagic, actualMagic );
			for ( int j = 0; j < ( i + 1 ) * 512; j++ ) in.read();
			lin.consume();
			System.out.println( re );
		}
		fis.close();

		fis = new FileInputStream( "/tmp/archive.gz" );
		gzar = new GZIPArchiveReader( fis );
		for ( int i = 0; i < 11; i++ ) {
			re = gzar.getEntry();
			if ( re == null ) break;
			LazyInflater lin = re.lazyInflater;
			in = lin.get();
			in.read( actualMagic );
			System.out.println( Util.toString( actualMagic ) );
			while ( in.read() != -1 );
			lin.consume();
			System.out.println( re );
		}
		fis.close();

		fis = new FileInputStream( "/tmp/archive.gz" );
		gzar = new GZIPArchiveReader( fis );
		for ( int i = 0; i < 11; i++ ) {
			re = gzar.getEntry();
			if ( re == null ) break;
			LazyInflater lin = re.lazyInflater;
			in = lin.get();
			in.read( actualMagic );
			assertArrayEquals( expectedMagic, actualMagic );
			System.out.println( Util.toString( actualMagic ) );
			in.skip( Long.MAX_VALUE );
			lin.consume();
			System.out.println( re );
		}
		fis.close();

	}

	public static void main( String[] args ) throws IOException, JSAPException {
		
		SimpleJSAP jsap = new SimpleJSAP( GZIPArchiveReader.class.getName(), "Writes some random records on disk.",
				new Parameter[] {
					new Switch( "fully", 'f', "fully", "Whether to read fully the record (and do a minimal cosnsistency check)."),
					new UnflaggedOption( "path", JSAP.STRING_PARSER, JSAP.NO_DEFAULT, JSAP.REQUIRED, JSAP.NOT_GREEDY, "The path to read from." ),
			} );

			JSAPResult jsapResult = jsap.parse( args );
			if ( jsap.messagePrinted() ) System.exit( 1 );

		final boolean fully = jsapResult.getBoolean( "fully" );
		GZIPArchiveReader gzar = new GZIPArchiveReader( new FileInputStream( jsapResult.getString( "path" ) ) );
		for ( ;; ) {
			ReadEntry e = gzar.getEntry();
			if ( e == null ) break;
			InputStream inflater = e.lazyInflater.get();
			if ( fully ) ByteStreams.toByteArray( inflater );
			e.lazyInflater.consume();
			System.out.println( e );
		}
	}
	
}
