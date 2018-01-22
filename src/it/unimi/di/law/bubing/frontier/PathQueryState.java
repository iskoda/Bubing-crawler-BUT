/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unimi.di.law.bubing.frontier;

import java.io.Serializable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author karel
 */
public class PathQueryState implements Delayed, Serializable {
	public byte[] pathQuery;
	public volatile long nextFetch;
	public volatile long fetchInterval;
    
        public PathQueryState( byte[] pathQuery ) {
            this.pathQuery = pathQuery;
            this.nextFetch = System.currentTimeMillis();
        }
        
        public PathQueryState( byte[] pathQuery, long nextFetch ) {
            this.pathQuery = pathQuery;
            this.nextFetch = nextFetch;
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
