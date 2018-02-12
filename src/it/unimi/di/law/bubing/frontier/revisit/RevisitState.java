/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unimi.di.law.bubing.frontier.revisit;

import it.unimi.di.law.bubing.frontier.VisitState;
import java.io.Serializable;
import java.util.concurrent.Delayed;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author karel
 */
public class RevisitState implements Delayed, Serializable {
	public VisitState visitState;
	public long nextFetch;

	public RevisitState( VisitState visitState ) {
		this( visitState,  Long.MAX_VALUE );
	}

	public RevisitState( VisitState visitState, long nextFetch ) {
		this.visitState = visitState;
		this.nextFetch = nextFetch;
	}

	@Override
	public long getDelay( final TimeUnit unit ) {
		return unit.convert( Math.max( 0, nextFetch - System.currentTimeMillis() ), TimeUnit.MILLISECONDS );
	}

	@Override
	public int compareTo( final Delayed o ) {
		return Long.signum( nextFetch - ( (RevisitState)o ).nextFetch );
	}
}
