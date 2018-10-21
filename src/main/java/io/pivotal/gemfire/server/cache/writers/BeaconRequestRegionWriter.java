package io.pivotal.gemfire.server.cache.writers;

import org.apache.geode.cache.CacheWriter;
import org.apache.geode.cache.CacheWriterException;
import org.apache.geode.cache.EntryEvent;
import org.apache.geode.cache.RegionEvent;
import org.apache.geode.pdx.PdxInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pivotal.gemfire.domain.BeaconRequest;
import io.pivotal.gemfire.server.Executor.SerialExecutor;
import io.pivotal.gemfire.server.cache.listeners.BeaconRequestRegionListener;
import io.pivotal.gemfire.server.service.CustomerPromotionRunnableService;

public class BeaconRequestRegionWriter implements CacheWriter {
	private final Logger log = LoggerFactory.getLogger(BeaconRequestRegionWriter.class);

	public void beforeUpdate(EntryEvent event) throws CacheWriterException {
		log.info("Received BeaconRequest update event: " + event);
		processEvent(event);
	}

	public void beforeCreate(EntryEvent event) throws CacheWriterException {
		log.info("Received BeaconRequest create event: " + event);
		processEvent(event);
	}

	public void beforeDestroy(EntryEvent event) throws CacheWriterException {
		// TODO Auto-generated method stub
	}

	public void beforeRegionDestroy(RegionEvent event) throws CacheWriterException {
		// TODO Auto-generated method stub
	}

	public void beforeRegionClear(RegionEvent event) throws CacheWriterException {
		// TODO Auto-generated method stub
	}

	private void processEvent(EntryEvent event) {
		SerialExecutor executor = SerialExecutor.getInstance();
		PdxInstance pdx = (PdxInstance) event.getNewValue();
		CustomerPromotionRunnableService cps = new CustomerPromotionRunnableService(pdx); 
		executor.execute(cps);
	}
}
