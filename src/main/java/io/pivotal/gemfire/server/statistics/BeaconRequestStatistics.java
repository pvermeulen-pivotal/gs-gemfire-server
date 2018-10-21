package io.pivotal.gemfire.server.statistics;

import org.apache.geode.StatisticDescriptor;
import org.apache.geode.Statistics;
import org.apache.geode.StatisticsType;
import org.apache.geode.cache.CacheFactory;

public class BeaconRequestStatistics {

	private Statistics beconRequestStat;
	private int numberEntries;
	private int createGreenplumBatchTime;
	private int greenplumDatabaseUpdateTime;
	private int greenplumDatabaseErrors;

	public BeaconRequestStatistics() {
		StatisticsType beaconRequestStatistic = CacheFactory.getAnyInstance().getDistributedSystem().createType(
				"beacon-request-greenplum", "Statistics for GemFire updating Greenplum database from BeaconRequest region",
				new StatisticDescriptor[] {
						CacheFactory.getAnyInstance().getDistributedSystem().createIntGauge("numberEntries",
								"Total number of entries processed in batch", "count"),
						CacheFactory.getAnyInstance().getDistributedSystem().createLongGauge("createGreenplumBatchTime",
								"Total amount of time to create batch file to update Greenplum database",
								"milliseconds", true),
						CacheFactory.getAnyInstance().getDistributedSystem().createLongGauge("greenplumDatabaseUpdateTime",
								"Total amount of time to to update Greenplum database", "milliseconds", true), 
						CacheFactory.getAnyInstance().getDistributedSystem().createIntGauge("greenplumDatabaseErrors",
								"The number of errors written to Greenplum error queue", "count", true), 
						});

		this.beconRequestStat = CacheFactory.getAnyInstance().getDistributedSystem().createStatistics(beaconRequestStatistic,
				"beacon-request-greenplum");
		this.numberEntries = this.beconRequestStat.nameToId("numberEntries");
		this.createGreenplumBatchTime = this.beconRequestStat.nameToId("createGreenplumBatchTime");
		this.greenplumDatabaseUpdateTime = this.beconRequestStat.nameToId("greenplumDatabaseUpdateTime");
		this.greenplumDatabaseErrors = this.beconRequestStat.nameToId("greenplumDatabaseErrors");
	}

	public void setNumberEntries(int count) {
		this.beconRequestStat.setInt(this.numberEntries, count);
	}

	public void updateNumberEntries(int count) {
		this.beconRequestStat.incInt(this.numberEntries, count);
	}

	public void setCreateGPBatchTime(long time) {
		this.beconRequestStat.setLong(this.createGreenplumBatchTime, time);
	}

	public void updateCreateGPBatchTime(long time) {
		this.beconRequestStat.incLong(this.createGreenplumBatchTime, time);
	}

	public void setGreenplumUpdateTime(long time) {
		this.beconRequestStat.setLong(this.greenplumDatabaseUpdateTime, time);
	}

	public void updateGreenplumUpdateTime(long time) {
		this.beconRequestStat.incLong(this.greenplumDatabaseUpdateTime, time);
	}

	public void setGreenplumDatabaseErrors(int count) {
		this.beconRequestStat.setInt(this.greenplumDatabaseErrors, count);
	}

	public void updateGreenplumDatabaseErrors(int count) {
		this.beconRequestStat.incInt(this.greenplumDatabaseErrors, count);
	}
}
