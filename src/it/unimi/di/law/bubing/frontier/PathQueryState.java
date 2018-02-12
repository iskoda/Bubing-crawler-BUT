/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unimi.di.law.bubing.frontier;

import it.unimi.di.law.bubing.util.BURL;
import java.io.Serializable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author karel
 */
public class PathQueryState implements Delayed, Serializable {

	public static final long FIRST_VISIT = 0;

	public byte[] pathQuery;
	public volatile long modified;
	public volatile long nextFetch;
	public volatile long fetchInterval;
	public volatile VisitState visitState;

	public PathQueryState( byte[] pathQuery ) {
		this( null, pathQuery, System.currentTimeMillis() );
	}
        
	public PathQueryState( VisitState visitState, byte[] pathQuery ) {
		this( visitState, pathQuery, System.currentTimeMillis() );
	}

	public PathQueryState( VisitState visitState, byte[] pathQuery, long nextFetch ) {
		this.pathQuery = pathQuery;
		this.modified = PathQueryState.FIRST_VISIT;
		this.nextFetch = nextFetch;
		this.fetchInterval = 0;
		this.visitState = visitState;
	}

	@Override
	public String toString() {
		return BURL.fromNormalizedSchemeAuthorityAndPathQuery( this.visitState.schemeAuthority, this.pathQuery ).toString();
	}

	@Override
	public long getDelay( final TimeUnit unit ) {
		return unit.convert( Math.max( 0, nextFetch - System.currentTimeMillis() ), TimeUnit.MILLISECONDS );
	}

	@Override
	public int compareTo( final Delayed o ) {
		return Long.signum( nextFetch - ( (PathQueryState)o ).nextFetch );
	}
}
