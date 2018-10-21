package io.pivotal.gemfire.server.cache.listeners;

import org.apache.geode.cache.CacheListener;
import org.apache.geode.cache.Declarable;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.RegionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pivotal.gemfire.domain.BeaconRequest;
import io.pivotal.gemfire.server.Executor.SerialExecutor;

public class BeaconRequestRegionListener implements CacheListener<String, BeaconRequest>, Declarable {

	private final Logger log = LoggerFactory.getLogger(BeaconRequestRegionListener.class);

	public void afterCreate(EntryEvent<String, BeaconRequest> event) {
		// TODO Auto-generated method stub
	}

	public void afterUpdate(EntryEvent<String, BeaconRequest> event) {
		// TODO Auto-generated method stub
	}

	public void afterInvalidate(EntryEvent<String, BeaconRequest> event) {
		// TODO Auto-generated method stub
	}

	public void afterDestroy(EntryEvent<String, BeaconRequest> event) {
		// TODO Auto-generated method stub
	}

	public void afterRegionInvalidate(RegionEvent<String, BeaconRequest> event) {
		// TODO Auto-generated method stub
	}

	public void afterRegionDestroy(RegionEvent<String, BeaconRequest> event) {
		 log.debug("BeaconRequest region destroy event " + event.toString());
		 SerialExecutor exec = SerialExecutor.getInstance();
		 exec.shutdown();
	}

	public void afterRegionClear(RegionEvent<String, BeaconRequest> event) {
		// TODO Auto-generated method stub
	}

	public void afterRegionCreate(RegionEvent<String, BeaconRequest> event) {
		// TODO Auto-generated method stub
	}

	public void afterRegionLive(RegionEvent<String, BeaconRequest> event) {
		// TODO Auto-generated method stub
	}

}
