package io.pivotal.gemfire.server.statistics;

import org.apache.geode.StatisticDescriptor;
import org.apache.geode.Statistics;
import org.apache.geode.StatisticsType;
import org.apache.geode.cache.CacheFactory;

public class BeaconResponseStatistics {

	private Statistics beconResponseStat;
	private int numberEntries;
	private int createGreenplumBatchTime;
	private int greenplumDatabaseUpdateTime;
	private int greenplumDatabaseErrors;

	public BeaconResponseStatistics() {
		StatisticsType beaconResponseStatistic = CacheFactory.getAnyInstance().getDistributedSystem().createType(
				"beacon-response-greenplum", "Statistics for GemFire updating Greenplum database from BeaconResponse region",
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

		this.beconResponseStat = CacheFactory.getAnyInstance().getDistributedSystem().createStatistics(beaconResponseStatistic,
				"beacon-response-greenplum");
		this.numberEntries = this.beconResponseStat.nameToId("numberEntries");
		this.createGreenplumBatchTime = this.beconResponseStat.nameToId("createGreenplumBatchTime");
		this.greenplumDatabaseUpdateTime = this.beconResponseStat.nameToId("greenplumDatabaseUpdateTime");
		this.greenplumDatabaseErrors = this.beconResponseStat.nameToId("greenplumDatabaseErrors");
	}

	public void setNumberEntries(int count) {
		this.beconResponseStat.setInt(this.numberEntries, count);
	}

	public void updateNumberEntries(int count) {
		this.beconResponseStat.incInt(this.numberEntries, count);
	}

	public void setCreateGPBatchTime(long time) {
		this.beconResponseStat.setLong(this.createGreenplumBatchTime, time);
	}

	public void updateCreateGPBatchTime(long time) {
		this.beconResponseStat.incLong(this.createGreenplumBatchTime, time);
	}

	public void setGreenplumUpdateTime(long time) {
		this.beconResponseStat.setLong(this.greenplumDatabaseUpdateTime, time);
	}

	public void updateGreenplumUpdateTime(long time) {
		this.beconResponseStat.incLong(this.greenplumDatabaseUpdateTime, time);
	}

	public void setGreenplumDatabaseErrors(int count) {
		this.beconResponseStat.setInt(this.greenplumDatabaseErrors, count);
	}

	public void updateGreenplumDatabaseErrors(int count) {
		this.beconResponseStat.incInt(this.greenplumDatabaseErrors, count);
	}
}
