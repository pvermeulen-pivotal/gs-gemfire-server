package io.pivotal.gemfire.server.service;

import org.apache.geode.pdx.PdxInstance;

import io.pivotal.gemfire.domain.BeaconResponse;

public class CustomerPromotionService {

	private final static String ENTRANCE_MSG = "Welcome John to out Boston store\n We have many specials available just for you";
	private final static String CHECKOUT_MSG = "Thank You John for Shopping with us today and we would like to offer you a coupon for $10 off your next visit for being such a loyal customer";
	private final static String AISLE_2_MSG = "We know that you usually prefer PBR but we would like for you to try Fat Tire and will give $5.00 off coupon to redeem at checkout";
	private final static String AISLE_3_MSG = "We have a BOGO on the potato chips you usally purchase";
	private final static String DEFAULT_MSG = "Welcome to Pivotal Supermarket";

	public BeaconResponse getCustomerPromotion(PdxInstance beaconReq) {
		int major = (Integer) beaconReq.getField("major");
		int minor = (Integer) beaconReq.getField("minor");
		String message = null;
		BeaconResponse beaconResp = new BeaconResponse();
		if (major == 1000 && minor == 1) { // enter store
			message = ENTRANCE_MSG;
			beaconResp.setPromotionId("promo-1");
		} else if (major == 1000 && minor == 2) { // asile 2
			message = AISLE_2_MSG;
			beaconResp.setPromotionId("promo-2");
		} else if (major == 1000 && minor == 3) { // asile 3
			message = AISLE_3_MSG;
			beaconResp.setPromotionId("promo-3");
		} else if (major == 1000 && minor == 4) { // checkout
			beaconResp.setPromotionId("promo-4");
			message = CHECKOUT_MSG;
		} else {
			beaconResp.setPromotionId("default-promo");
			message = DEFAULT_MSG;
		}
		beaconResp.setCustomerId((String) beaconReq.getField("customerId"));
		beaconResp.setDeviceId((String) beaconReq.getField("deviceId"));
		beaconResp.setMajor((Integer) beaconReq.getField("major"));
		beaconResp.setMinor((Integer) beaconReq.getField("minor"));
		beaconResp.setUuid((String) beaconReq.getField("uuid"));
		beaconResp.setMarketingMessage(message);
		return beaconResp;
	}
}
