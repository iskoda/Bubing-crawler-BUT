package it.unimi.di.law.bubing.frontier;

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

import it.unimi.di.law.bubing.util.BURL;
import it.unimi.di.law.bubing.util.ByteArrayDiskQueues;
import it.unimi.di.law.bubing.util.ByteArrayDiskQueues.QueueData;
import it.unimi.di.law.bubing.util.Util;
import it.unimi.dsi.fastutil.bytes.ByteArrayList;
import it.unimi.dsi.fastutil.io.FastBufferedInputStream;
import it.unimi.dsi.fastutil.io.FastBufferedOutputStream;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Reference2ObjectMap;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sleepycat.je.Cursor;
import com.sleepycat.je.Database;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.PriorityQueue;

//RELEASE-STATUS: DIST

/** A <em>workbench virtualizer</em> based on a {@linkplain Database Berkeley DB} database.
 * 
 * <p>An instance of this class acts as a thin layer between the workbench and a set of disk queues, possibly one for
 * each visit state, stored in a {@link Database}. Each queue is associated with a scheme+authority (the key). 
 * Values are given by an increasing timestamp (written as a vByte-encoded integer) followed by a path+query.
 * 
 * <p>Path+queries are enqueued using the {@link #enqueueURL(VisitState, ByteArrayList)} method. They can be {@linkplain #dequeuePathQueries(VisitState, int) dequeued in batches}
 * (the method uses {@linkplain Cursor cursors}). When a queue is no longer needed, it can be {@linkplain #remove(VisitState) removed}. 
 * 
 * @author Sebastiano Vigna
 */
public class WorkbenchVirtualizer implements Closeable {
	private static final Logger LOGGER = LoggerFactory.getLogger( WorkbenchVirtualizer.class );
	
	/** The underlying set of byte-array disk queues. */
	private ByteArrayDiskQueues byteArrayDiskQueues;
	/** A reference to the {@link Frontier}. */
	private Frontier frontier;
	/** The directory containing the virtualizer files. */
	private File directory;

	/** Creates the virtualizer.
	 * 
	 * @param frontier the frontier instantiating this virtualizer.
	 */
	public WorkbenchVirtualizer( final Frontier frontier ) {
		this.frontier = frontier;
		directory = new File( frontier.rc.frontierDir, "virtualizer" );
		directory.mkdir();
		byteArrayDiskQueues = new ByteArrayDiskQueues( directory );
	}

	/** Dequeues at most the given number of path+queries into the given visit state.
	 * 
	 * <p>Note that the path+queries are directly enqueued into the visit state using 
	 * {@link VisitState#enqueuePathQuery(byte[])}.
	 * 
	 * @param visitState the visitState in which path+queries will be moved.
	 * @param maxUrls the maximum number of path+queries to move.
	 * @return the number of actually dequeued path+queries.
	 * @throws IOException 
	 */
	public int dequeuePathQueries( final VisitState visitState, final int maxUrls ) throws IOException {
		if ( maxUrls == 0 ) return 0;
		int dequeued = (int)Math.min( maxUrls, byteArrayDiskQueues.count( visitState ) );
		for( int i = dequeued; i-- != 0; ) {
                    byte[] pathQuery = byteArrayDiskQueues.dequeue( visitState );
                    final PathQueryState pathQueryState = new PathQueryState( pathQuery );
                    visitState.enqueuePathQuery( pathQueryState );
                }
		return dequeued;
	}

        	/** Dequeues at most the given number of path+queries into the given visit state.
	 * 
	 * <p>Note that the path+queries are directly enqueued into the visit state using 
	 * {@link VisitState#enqueuePathQuery(byte[])}.
	 * 
	 * @param visitState the visitState in which path+queries will be moved.
	 * @param maxUrls the maximum number of path+queries to move.
	 * @return the number of actually dequeued path+queries.
	 * @throws IOException 
	 */
	public int dequeuePathQueriesState( final VisitState visitState, final int maxUrls ) throws IOException {
		if ( maxUrls == 0 ) return 0;
		int dequeued = 0;
                
                PriorityQueue<PathQueryState> pathQueries = new PriorityQueue<>();
                
                for( int i = (int)byteArrayDiskQueues.count( visitState ); i-- != 0; ) {
                	byte[] bytes = byteArrayDiskQueues.dequeue( visitState );
                        
			try( ByteArrayInputStream b = new ByteArrayInputStream( bytes ) ){
				try( ObjectInputStream o = new ObjectInputStream( b ) ){
					final PathQueryState pathQueryState = (PathQueryState) o.readObject();
					pathQueries.add( pathQueryState );
                                }
			} catch ( Throwable e ) {
				LOGGER.warn( "Exception during virtualizer dequeue: " + e );
                        }
		}
                
		for( int i = pathQueries.size(); i-- != 0; ) {
                        final PathQueryState pathQuery = pathQueries.poll();
                        if ( pathQuery.nextFetch < System.currentTimeMillis() && dequeued < maxUrls ) {
                            visitState.enqueuePathQuery( pathQuery );
                            dequeued++;
                            LOGGER.info( "Dequeue url: {}", BURL.fromNormalizedSchemeAuthorityAndPathQuery( visitState.schemeAuthority, pathQuery.pathQuery ) );
                        } else {
                            this.enqueuePathQueryState( visitState, pathQuery );
                        }
                }
                LOGGER.info( "Dequeue {} url to visitstate {}", dequeued, Util.toString( visitState.schemeAuthority ) );
                        
		return dequeued;
	}
	/** Returns the number of path+queries associated with the given visit state.
	 * 
 	 * @param visitState the visitState whose path+queries are to be counted.
	 * @return the number of path+queries associated with the given visit state.
	 */
	public long count( VisitState visitState ) {
		return byteArrayDiskQueues.count( visitState );
	}

	/** Returns the number of visit states on disk.
	 * 
	 * @return the number of visit states on disk.
	 */
	public int onDisk() {
		return byteArrayDiskQueues.numKeys();
	}

	/** Removes all path+queries associated with the given visit state.
	 * 
 	 * @param visitState the visitState whose path+queries are to be removed.
	 * @throws IOException 
	 */
	public void remove( VisitState visitState ) throws IOException {
		byteArrayDiskQueues.remove( visitState );
	}

	/** Enqueues the given URL as a path+query associated to the scheme+authority of the given visit state.
	 *  
 	 * @param visitState the visitState to which the URL must be added.
	 * @param url a {@link BURL BUbiNG URL}.
	 * @throws IOException 
	 */
	public void enqueueURL( VisitState visitState, final ByteArrayList url ) throws IOException {
		final byte[] urlBuffer = url.elements();
		final int pathQueryStart = BURL.startOfpathAndQuery( urlBuffer );
		byteArrayDiskQueues.enqueue( visitState,  urlBuffer, pathQueryStart, url.size() - pathQueryStart );
	}


	/**
	 *  
 	 * @param visitState
	 * @param pathQuery 
	 * @throws IOException 
	 */
	public void enqueuePathQueryState( VisitState visitState, final PathQueryState pathQuery ) throws IOException {
                LOGGER.info( "Enqueue url: {}", BURL.fromNormalizedSchemeAuthorityAndPathQuery( visitState.schemeAuthority, pathQuery.pathQuery ) );
				
                try(ByteArrayOutputStream b = new ByteArrayOutputStream()){
                    try(ObjectOutputStream o = new ObjectOutputStream(b)){
                        o.writeObject(pathQuery);
                    }
                    byte[] serialized = b.toByteArray();
                    byteArrayDiskQueues.enqueue( visitState, serialized, 0, serialized.length ); 
                }
	}

	/** Performs a garbage collection if the space used is below a given threshold, reaching a given target ratio.
	 * 
	 * @param threshold if {@link ByteArrayDiskQueues#ratio()} is below this value, a garbage collection will be performed.
	 * @param targetRatio passed to {@link ByteArrayDiskQueues#count(Object)}.
	 */
	public void collectIf( final double threshold, final double targetRatio ) throws IOException {
		if ( byteArrayDiskQueues.ratio() < threshold ) {
			LOGGER.info( "Starting collection..." );
			byteArrayDiskQueues.collect( targetRatio );
			LOGGER.info( "Completed collection." );
		}
	}

	@Override
	public void close() throws IOException {
		final ObjectOutputStream oos = new ObjectOutputStream( new FastBufferedOutputStream( new FileOutputStream( new File( directory, "metadata" ) ) ) );
		byteArrayDiskQueues.close();
		writeMetadata( oos );
	}

	@Override
	public String toString() {
		return "URLs on disk: " + byteArrayDiskQueues.size64() + "; fill ratio: " + byteArrayDiskQueues.ratio();
	}
	
	private void writeMetadata( final ObjectOutputStream oos ) throws IOException {
		oos.writeLong( byteArrayDiskQueues.size );
		oos.writeLong( byteArrayDiskQueues.appendPointer );
		oos.writeLong( byteArrayDiskQueues.used );
		oos.writeLong( byteArrayDiskQueues.allocated );
		oos.writeInt( byteArrayDiskQueues.buffers.size() );
		oos.writeInt( byteArrayDiskQueues.key2QueueData.size() );
		final ObjectIterator<Reference2ObjectMap.Entry<Object, QueueData>> fastIterator = byteArrayDiskQueues.key2QueueData.reference2ObjectEntrySet().fastIterator();
		for( int i = byteArrayDiskQueues.key2QueueData.size(); i-- != 0; ) {
			Reference2ObjectMap.Entry<Object, QueueData> next = fastIterator.next();
			final VisitState visitState = (VisitState)next.getKey();
			// TODO: temporary, to catch serialization bug
			if ( visitState == null ) LOGGER.error( "Map iterator returned null key" );
			else if ( visitState.schemeAuthority == null ) LOGGER.error( "Map iterator returned visit state with null schemeAuthority" );
			else Util.writeVByte( visitState.schemeAuthority.length, oos );
			oos.write( visitState.schemeAuthority ); 
			oos.writeObject( next.getValue() );
		}
		
		oos.close();
	}

	public void readMetadata() throws IOException, ClassNotFoundException {
		final ObjectInputStream ois = new ObjectInputStream( new FastBufferedInputStream( new FileInputStream( new File( directory, "metadata" ) ) ) );
		byteArrayDiskQueues.size = ois.readLong();
		byteArrayDiskQueues.appendPointer = ois.readLong();
		byteArrayDiskQueues.used = ois.readLong();
		byteArrayDiskQueues.allocated = ois.readLong();
		final int n = ois.readInt();
		byteArrayDiskQueues.buffers.size( n );
		byteArrayDiskQueues.files.size( n );
		final VisitStateSet schemeAuthority2VisitState = frontier.distributor.schemeAuthority2VisitState;
		byte[] schemeAuthority = new byte[ 1024 ];
		for( int i = ois.readInt(); i-- != 0; ) {
			final int length = Util.readVByte( ois );
			if ( schemeAuthority.length < length ) schemeAuthority = new byte[ length ];
			ois.readFully( schemeAuthority, 0, length );
			final VisitState visitState = schemeAuthority2VisitState.get( schemeAuthority, 0, length );
			// This can happen if the serialization of the visit states has not been completed.
			if ( visitState != null ) byteArrayDiskQueues.key2QueueData.put( visitState, (QueueData)ois.readObject() );
			else LOGGER.error( "No visit state found for " + Util.toString( schemeAuthority ) );
		}
		
		ois.close();
	}
}
