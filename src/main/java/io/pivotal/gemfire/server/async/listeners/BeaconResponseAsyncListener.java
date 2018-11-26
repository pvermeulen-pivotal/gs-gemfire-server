package io.pivotal.gemfire.server.async.listeners;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.nio.charset.Charset;
import java.sql.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import javax.sql.DataSource;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.Operation;
import org.apache.geode.cache.asyncqueue.AsyncEvent;
import org.apache.geode.cache.asyncqueue.AsyncEventListener;
import org.apache.geode.pdx.PdxInstance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.dao.ConcurrencyFailureException;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.TransientDataAccessResourceException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.UncategorizedSQLException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.SQLStateSQLExceptionTranslator;

import io.pivotal.gemfire.server.service.CopyService;
import io.pivotal.gemfire.server.service.ErrorMessage;
import io.pivotal.gemfire.server.service.HostInfo;
import io.pivotal.gemfire.server.statistics.BeaconResponseStatistics;

public class BeaconResponseAsyncListener implements AsyncEventListener {
	private final String ERROR_REGION = "GreenplumErrorQueue";
	private final int ERROR_LOG_LIMIT = 25;
	private final Logger log = LoggerFactory.getLogger(BeaconResponseAsyncListener.class);
	private final CopyService copyService;
	private final HostInfo hostInfo;

	private AbstractApplicationContext ctx;

	private Exception lastException;

	private Timer timer;
	private TimerTask task;

	private int logErrorMessageCount = ERROR_LOG_LIMIT;
	private long delay;
	private String[] mapFields;
	private boolean initialized = false;

	private BeaconResponseStatistics beaconRespStats;

	public BeaconResponseAsyncListener() {
		ctx = new ClassPathXmlApplicationContext("classpath*:gemfire-beacon-response-context.xml");
		JdbcTemplate jdbcTemplate = new JdbcTemplate((DataSource) ctx.getBean("dataSource"));
		jdbcTemplate.setExceptionTranslator(new SQLStateSQLExceptionTranslator());
		this.hostInfo = (HostInfo) ctx.getBean("hostInfo");
		copyService = new CopyService(jdbcTemplate, hostInfo);
		beaconRespStats = new BeaconResponseStatistics();
		initialize();
		delay = (long) CacheFactory.getAnyInstance().getAsyncEventQueue("beacon-response").getBatchTimeInterval();
	}

	private void initialize() {
		String mapping = null;
		StringWriter writer = new StringWriter();
		InputStream stream = BeaconResponseAsyncListener.class.getResourceAsStream("/beacon-response-mapping.txt");
		try {
			IOUtils.copy(stream, writer, "UTF-8");
			mapping = writer.toString();
			mapFields = mapping.split(";");
			writer.close();
			stream.close();
			statEntryTimer(delay);
			initialized = true;
		} catch (IOException e) {
			log.error("BeaconResponse Async Listener failed during initialization- exception: " + e.getMessage());
		}
	}

	public void close() {
		ctx.close();
	}

	@SuppressWarnings("rawtypes")
	public boolean processEvents(List<AsyncEvent> events) {
		long startTm = 0;

		try {
			timer.cancel();
			timer.purge();
		} catch (Exception e) {
			// don't care do nothing
		}

		StringBuilder strBuilder = new StringBuilder();
		Date dt = new Date(new java.util.Date().getTime());
		if (initialized) {
			startTm = System.currentTimeMillis();
			for (AsyncEvent<?, ?> event : events) {
				if (!processEvent(event, strBuilder, dt))
					return false;
			}
			beaconRespStats.setCreateGPBatchTime(System.currentTimeMillis() - startTm);
			beaconRespStats.setNumberEntries(events.size());
			statEntryTimer(delay);
			if (strBuilder.length() > 0) {
				startTm = System.currentTimeMillis();
				boolean ret = processExternalTable(strBuilder);
				beaconRespStats.setGreenplumUpdateTime(System.currentTimeMillis() - startTm);
				return ret;
			} else {
				return true;
			}
		} else {
			return false;
		}
	}

	private boolean processExternalTable(StringBuilder strBuilder) {
		boolean retCode = true;
		String uuid = UUID.randomUUID().toString().replace('-', '_');
		File file = new File(this.hostInfo.getDirectory() + this.hostInfo.getLocation() + uuid + ".txt");
		try {
			FileUtils.writeStringToFile(file, strBuilder.toString(), Charset.defaultCharset());
			copyService.copy(uuid, File.separator + this.hostInfo.getLocation() + uuid + ".txt");
		} catch (Exception e) {
			if (e instanceof IOException) {
				log.error("Unable to write file uuid=" + uuid + " for gpfdist: " + e.getMessage());
			} else {
				log.error("Unable to process table insert uuid=" + uuid + " for gpfdist: " + e.getMessage());
			}
			retCode = false;
		} finally {
			if (file.exists())
				file.delete();
			strBuilder = null;
		}
		return retCode;
	}

	private boolean processEvent(AsyncEvent<?, ?> event, StringBuilder strBuilder, Date date) {
		boolean returnCode = true;
		ErrorMessage errorMessage = null;
		Operation operation = event.getOperation();
		PdxInstance pdx = (PdxInstance) event.getDeserializedValue();
		try {
			if (operation.isCreate()) {
				strBuilder.append(event.getKey() + "|");
				strBuilder.append(buildRow(pdx, date) + "\n");
			} else {
				log.info(event.getRegion().getName() + " region event was not a create operation; Record bypassed. key="
						+ event.getKey());
			}
		} catch (DataAccessException e) {
			returnCode = false;
			if ((e instanceof BadSqlGrammarException) || (e instanceof DataIntegrityViolationException)
					|| (e instanceof TransientDataAccessResourceException) || (e instanceof ConcurrencyFailureException)
					|| (e instanceof UncategorizedSQLException)) {
				errorMessage = new ErrorMessage(event.getKey(), pdx, operation, e.getMessage(), e);
				returnCode = true;
			}
			if (returnCode) {
				log.warn("Process event exception: Exception written to " + ERROR_REGION + " region for operation="
						+ operation.toString() + " key=" + event.getKey() + " value=" + pdx + " exception="
						+ e.getMessage());
			} else {
				if ((lastException == null)
						|| (lastException.getClass().getCanonicalName() == e.getClass().getCanonicalName())) {
					logErrorMessageCount++;
					if (logErrorMessageCount > ERROR_LOG_LIMIT) {
						logErrorMessageCount = 0;
						log.warn("Process event exception: operation=" + operation.toString() + " key=" + event.getKey()
								+ " value=" + pdx + " exception=" + e.getMessage());
					}
				} else {
					log.warn("Process event exception: operation=" + operation.toString() + " key=" + event.getKey()
							+ " value=" + pdx + " exception=" + e.getMessage());
				}
			}
			lastException = e;
		}
		if (errorMessage != null) {
			beaconRespStats.updateGreenplumDatabaseErrors(1);
			writeErrorMessage(errorMessage);
		}
		return returnCode;
	}

	private void writeErrorMessage(ErrorMessage errorMessage) {
		try {
			CacheFactory.getAnyInstance().getRegion(ERROR_REGION).put(errorMessage.getKey(), errorMessage);
		} catch (Exception e) {
			log.error("Unable to write meesage to " + ERROR_REGION + " " + errorMessage.toString() + " Exception: "
					+ e.getMessage());
		}
	}

	private String buildRow(PdxInstance pdx, Date date) {
		StringBuilder sb = new StringBuilder();
		for (String mfld : mapFields) {
			Object obj = pdx.getField(mfld);
			if (obj != null) {
				if (obj instanceof String) {
					sb.append((String) obj + "|");
				} else if (obj instanceof BigDecimal) {
					sb.append(((BigDecimal) obj).toString() + "|");
				}
			} else {
				if (mfld.startsWith("receive_datetime"))
					sb.append(date.toString() + "|");
			}
		}

		if (sb.length() > 0) {
			return sb.toString().substring(0, sb.length() - 1);
		} else {
			return sb.toString();
		}
	}

	private void statEntryTimer(long delay) {
		task = new TimerTask() {
			public void run() {
				beaconRespStats.setNumberEntries(0);
			}
		};
		timer = new Timer("EntryTimer");
		timer.schedule(task, delay);
	}
}
