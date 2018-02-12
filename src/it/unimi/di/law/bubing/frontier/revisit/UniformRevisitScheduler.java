/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.unimi.di.law.bubing.frontier.revisit;

import it.unimi.di.law.bubing.frontier.PathQueryState;
import it.unimi.di.law.bubing.util.FetchData;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author karel
 */
public class UniformRevisitScheduler implements RevisitScheduler {

	long defaultInterval;

	public void UniformRevisitScheduler() {
		UniformRevisitScheduler( TimeUnit.SECONDS.toMillis( 60 ) );
	}

	public void UniformRevisitScheduler( long defaultInterval ) {
		this.defaultInterval = defaultInterval;
	}

	@Override
	public PathQueryState schedule( FetchData fetchData, PathQueryState pathQuery, boolean modified ) {

		if ( modified || pathQuery.modified == PathQueryState.FIRST_VISIT ) {
			pathQuery.modified = fetchData.endTime;
		}
		pathQuery.nextFetch = fetchData.endTime + this.defaultInterval;
		pathQuery.fetchInterval = this.defaultInterval;

		return pathQuery;
	}
}
