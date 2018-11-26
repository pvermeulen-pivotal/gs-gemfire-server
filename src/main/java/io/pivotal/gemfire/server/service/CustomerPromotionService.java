package io.pivotal.gemfire.server.service;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Region;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.Struct;
import org.apache.geode.pdx.PdxInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pivotal.gemfire.domain.BeaconResponse;

public class CustomerPromotionService {

	private Logger log = LoggerFactory.getLogger(CustomerPromotionService.class);

	private final static String ERROR_1_MESSAGE = "Customer does not exist";
	private final static String ERROR_2_MESSAGE = "Store does not exist";
	private final static String ERROR_3_MESSAGE = "Beacon category does not exists";
	private final static String ERROR_4_MESSAGE = "Category does not exists";
	private final static String ERROR_5_MESSAGE = "No products found for category";

	private final static String REMINDER_1 = "Reminder.. You usually purchase ";
	private final static String REMINDER_1_1 = " when you purchase ";

	public BeaconResponse getCustomerPromotion(PdxInstance beaconReq) {
		BeaconResponse beaconResp = new BeaconResponse();
		int major = (Integer) beaconReq.getField("major");
		int minor = (Integer) beaconReq.getField("minor");
		beaconResp.setId(new Random().nextLong() & Long.MAX_VALUE);
		beaconResp.setCustomerId((Long) beaconReq.getField("customerId"));
		beaconResp.setDeviceId((String) beaconReq.getField("deviceId"));
		beaconResp.setUuid((String) beaconReq.getField("uuid"));
		beaconResp.setMajor((Integer) beaconReq.getField("major"));
		beaconResp.setMinor((Integer) beaconReq.getField("minor"));
		beaconResp.setPromotionId(0);
		beaconResp.setMessage("");
		beaconResp.setUrl("");
		beaconResp.setError("");

		log.info("Customer promotion - beacon response: " + beaconResp.toString());

		// Verify customer
		PdxInstance customer = getCustomer(beaconReq);
		if (customer == null) {
			beaconResp.setError(ERROR_1_MESSAGE);
			log.error("Customer promotion - No customer found ");
			return beaconResp;
		}

		// Get store details
		PdxInstance store = getStoreInfo(major);
		if (store == null) {
			log.error("Customer promotion - No store found ");
			beaconResp.setError(ERROR_2_MESSAGE);
			return beaconResp;
		}

		// If beacon request is store entrance
		if (minor == 0) {
			String str = "Welcome {0} to {1}";
			Object[] values = new Object[2];
			values[0] = customer.getField("firstName");
			values[1] = store.getField("name");
			beaconResp.setMessage(MessageFormat.format(str, values));
			log.info("Customer promotion - Store entrance");
			return beaconResp;
		}

		// If beacon request store checkout
		if (minor == 99) {
			String str = "Thank you {0} for shopping at {1}";
			Object[] values = new Object[2];
			values[0] = customer.getField("firstName");
			values[1] = store.getField("name");
			beaconResp.setMessage(MessageFormat.format(str, values));
			log.info("Customer promotion - Checkout");
			return beaconResp;
		}

		// Verify beacon and get category id
		int categoryId = getBeaconCategoryId(beaconReq);
		if (categoryId == -1) {
			beaconResp.setError(ERROR_3_MESSAGE);
			log.error("Customer promotion - No beacon category found");
			return beaconResp;
		}

		// Get category from beacon categoryId
		PdxInstance category = getCategory(categoryId);
		if (category == null) {
			beaconResp.setError(ERROR_4_MESSAGE);
			log.error("Customer promotion - No category found ");
			return beaconResp;
		}

		// Get all products where category and subCategory ids match
		List<PdxInstance> products = getProductFromCategory((Integer) category.getField("categoryId"),
				(Integer) category.getField("subCategoryId"));
		if (products == null || products.size() == 0) {
			beaconResp.setError(ERROR_5_MESSAGE);
			log.error("Customer promotion - No products found for category");
			return beaconResp;
		}

		// Get any promotion for product
		List<PdxInstance> promotions = new ArrayList<PdxInstance>();
		for (PdxInstance product : products) {
			PdxInstance promotion = getProductPromotions((Long) product.getField("productId"));
			if (promotion != null)
				promotions.add(promotion);
		}

		// See if there are promotions like a bogo or other coupon
		if (promotions.size() > 0 && checkForPromotions(promotions, beaconResp)) {
			log.info("Customer promotion - bogo/coupon promotions found");
			return beaconResp;
		} else {
			// otherwise look at the product and use association
			// rules for rendering a frequent association
			checkForAssociation(products, (Long) customer.getField("customerId"), beaconResp);
			log.info("Customer promotion - accociation");
		}
		return beaconResp;
	}

	@SuppressWarnings("rawtypes")
	private void checkForAssociation(List<PdxInstance> products, long customerId, BeaconResponse response) {
		String str = "select distinct productId from /CustomerFavorites where customerId = {0} and (productId in set ({1}))";
		Object[] values = new Object[2];
		values[0] = String.valueOf(customerId) + "L";
		StringBuilder sb = new StringBuilder();
		for (PdxInstance product : products) {
			sb.append(String.valueOf((Long) product.getField("productId")) + "L,");
		}
		values[1] = sb.toString().substring(0, sb.toString().length() - 1);
		String queryStr = MessageFormat.format(str, values);
		try {
			QueryService service = CacheFactory.getAnyInstance().getQueryService();
			SelectResults results = (SelectResults) service.newQuery(queryStr).execute();
			if (results != null) {
				List<Long> productIds = new ArrayList<Long>();
				for (Iterator iter = results.iterator(); iter.hasNext();) {
					productIds.add((Long) iter.next());
				}
				if (productIds.size() > 0) {
					getProductAssociations(products, response);
				}
			}
		} catch (Exception ex) {
			log.error("Check For Association Exception", ex);
		}
	}

	@SuppressWarnings("rawtypes")
	private void getProductAssociations(List<PdxInstance> products, BeaconResponse response) {
		String str1 = "select distinct ruleId, preProductId, postProductId, confidence from /AssociationRules where preProductId in set({0})";
		StringBuilder set = new StringBuilder();
		for (PdxInstance product : products) {
			set.append(String.valueOf((Long) product.getField("productId")) + "L,");
		}
		String queryStr = MessageFormat.format(str1, set.toString().substring(0, set.toString().length() - 1));
		try {
			QueryService service = CacheFactory.getAnyInstance().getQueryService();
			SelectResults results = (SelectResults) service.newQuery(queryStr).execute();
			if (results != null) {
				double confidence = 0;
				long postProductId = 0;
				long preProductId = 0;
				int ruleId = 0;
				for (Iterator iter = results.iterator(); iter.hasNext();) {
					Struct struct = (Struct) iter.next();
					long preId = (Long) struct.get("preProductId");
					long postId = (Long) struct.get("postProductId");
					double confid = (Double) struct.get("confidence");
					int rule = (Integer) struct.get("ruleId");
					if (confid > confidence) {
						confidence = confid;
						postProductId = postId;
						preProductId = preId;
						ruleId = rule;
					}
				}
				if (postProductId != 0 && confidence != 0) {
					PdxInstance preProduct = getProduct(preProductId);
					PdxInstance postProduct = getProduct(postProductId);
					StringBuilder sb = new StringBuilder();
					sb.append(REMINDER_1);
					sb.append((String) postProduct.getField("productName") + " ");
					sb.append(REMINDER_1_1);
					sb.append((String) preProduct.getField("productName"));
					response.setMessage(sb.toString());
					response.setUrl((String) postProduct.getField("url"));
					response.setPromotionId(ruleId);
				}
			}
		} catch (Exception ex) {
			log.error("Get Product Association Exception", ex);
		}
	}

	private boolean checkForPromotions(List<PdxInstance> promotions, BeaconResponse beaconResp) {
		StringBuilder sb = new StringBuilder();
		String url = null;
		for (PdxInstance promotion : promotions) {
			PdxInstance promo = getPromotion((Integer) promotion.getField("promotionId"));
			if (promo != null) {
				beaconResp.setPromotionId((Integer) promotion.getField("promotionId"));
				if (sb.length() > 0) {
					if (promo.getField("message") != null && ((String) promo.getField("message")).length() > 0) {
						sb.append("\n");
						sb.append((String) promo.getField("message"));
					}
				} else {
					if (promo.getField("message") != null && ((String) promo.getField("message")).length() > 0)
						sb.append((String) promo.getField("message"));
				}
				if (promo.getField("url") != null && ((String) promo.getField("url")).length() > 0) {
					url = (String) promo.getField("url");
				}
			}
		}

		// check for promotions and send to client if found
		if (sb.length() > 0 || url != null) {
			if (sb.length() > 0) {
				sb.append("\n");
				beaconResp.setMessage(sb.toString());
			}
			if (url != null)
				beaconResp.setUrl(url);
			return true;
		} else {
			return false;
		}

	}

	private PdxInstance getPromotion(int promotionId) {
		Region region = CacheFactory.getAnyInstance().getRegion("Promotion");
		if (region != null) {
			return (PdxInstance) region.get(promotionId);
		} else {
			log.error("No Promotion region found");
		}
		return null;
	}

	private PdxInstance getCustomer(PdxInstance request) {
		Region region = CacheFactory.getAnyInstance().getRegion("Customer");
		if (region != null) {
			return (PdxInstance) region.get((Long) request.getField("customerId"));
		} else {
			log.error("No Customer region found");
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	private int getBeaconCategoryId(PdxInstance request) {
		String queryStr = new String("select distinct categoryId from /Beacon where uuid=$1 and major=$2 and minor=$3");
		Object[] parms = new Object[3];
		parms[0] = request.getField("uuid");
		parms[1] = request.getField("major");
		parms[2] = request.getField("minor");
		try {
			QueryService service = CacheFactory.getAnyInstance().getQueryService();
			SelectResults results = (SelectResults) service.newQuery(queryStr).execute(parms);
			if (results != null) {
				for (Iterator iter = results.iterator(); iter.hasNext();) {
					return (Integer) iter.next();
				}
			}
		} catch (Exception ex) {
			log.error("Get Beacon Category Id Exception", ex);
		}
		return -1;
	}

	private PdxInstance getCategory(int categoryId) {
		Region region = CacheFactory.getAnyInstance().getRegion("Category");
		if (region != null) {
			return (PdxInstance) region.get(categoryId);
		} else {
			log.error("No Category region found");
		}
		return null;
	}

	@SuppressWarnings("rawtypes")
	private List<PdxInstance> getProductFromCategory(int categoryId, int subCategoryId) {
		List<PdxInstance> products = new ArrayList<PdxInstance>();
		String queryStr = new String("select distinct * from /Product where categoryId=$1 and subCategoryId=$2");
		Object[] parms = new Object[2];
		parms[0] = categoryId;
		parms[1] = subCategoryId;
		try {
			QueryService service = CacheFactory.getAnyInstance().getQueryService();
			SelectResults results = (SelectResults) service.newQuery(queryStr).execute(parms);
			if (results != null) {
				for (Iterator iter = results.iterator(); iter.hasNext();) {
					products.add((PdxInstance) iter.next());
				}
				return products;
			}
		} catch (Exception ex) {
			log.error("Get Product From Category Exception", ex);
		}
		return null;
	}

	private PdxInstance getProductPromotions(long productId) {
		Region region = CacheFactory.getAnyInstance().getRegion("ProductPromotion");
		if (region != null) {
			return (PdxInstance) region.get(productId);
		} else {
			log.error("No ProductPromotion region found");
		}
		return null;
	}

	private PdxInstance getStoreInfo(int major) {
		Region region = CacheFactory.getAnyInstance().getRegion("Store");
		if (region != null) {
			return (PdxInstance) region.get(major);
		} else {
			log.error("No Store region found");
		}
		return null;
	}

	private PdxInstance getProduct(long productId) {
		Region region = CacheFactory.getAnyInstance().getRegion("Product");
		if (region != null) {
			return (PdxInstance) region.get(productId);
		} else {
			log.error("No Product region found");
		}
		return null;
	}

}
