package io.pivotal.gemfire.server.service;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.pdx.PdxInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pivotal.gemfire.domain.BeaconResponse;

public class CustomerPromotionRunnableService implements Runnable {

	private final String REGION = "BeaconResponse";
	private final Logger log = LoggerFactory.getLogger(CustomerPromotionRunnableService.class);

	private PdxInstance beaconReq;

	public CustomerPromotionRunnableService(PdxInstance beaconRequest) {
		this.beaconReq = beaconRequest;
	}

	public void run() {
		BeaconResponse beaconResp;
		log.info("Starting Customer Promotion Service");
		try {
			CustomerPromotionService service = new CustomerPromotionService();
			beaconResp = service.getCustomerPromotion(beaconReq);
			CacheFactory.getAnyInstance().getRegion(REGION).put(beaconResp.getKey(), beaconResp);
			log.info("Response written to BeaconResponse region");
		} catch (Exception e) {
			log.error("Error writing response to BeaconResponse region exception: " + e.getMessage());
		}
		beaconResp = null;
		beaconReq = null;
	}

}
