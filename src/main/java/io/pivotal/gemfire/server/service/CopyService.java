package io.pivotal.gemfire.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class CopyService {

	private JdbcTemplate jdbcTemplate;
	private final HostInfo hostInfo;

	private static final Logger LOG = LoggerFactory.getLogger(CopyService.class);

	public CopyService(JdbcTemplate jdbcTemplate, HostInfo hostInfo) {
		this.jdbcTemplate = jdbcTemplate;
		this.hostInfo = hostInfo;
	}

	public void copy(String uuid, String location) throws Exception {
		LOG.info("copy");
		String extTableName = createExtTable(uuid, location);
		try {
			copyRows(extTableName);
		} finally {
			dropExtTable(extTableName);
		}
	}

	private void dropExtTable(String extTableName) {
		String ddl = "drop external table " + extTableName;
		LOG.info("dropExtTable: ddl={}", ddl);
		jdbcTemplate.execute(ddl);
	}

	private void copyRows(String extTableName) throws Exception {
		String ddl = "insert into " + hostInfo.getLikeTable() + " (select * from " + extTableName + ")";
		LOG.info("copyRows: ddl={}", ddl);
		jdbcTemplate.execute(ddl);
	}

	private String createExtTable(String uuid, String location) throws Exception {
		String extTableName = "gemfire_greenplum_" + uuid;
		String ddl = "create external table " + extTableName + " (like " + hostInfo.getLikeTable()
				+ ") location ('gpfdist://" + hostInfo.getName() + ":" + hostInfo.getPort() + location
				+ "') format 'TEXT' (DELIMITER AS '|' NULL AS 'null');";
		LOG.info("createExtTable: ddl={}", ddl);
		jdbcTemplate.execute(ddl);
		return extTableName;
	}
}
