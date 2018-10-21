package io.pivotal.gemfire.server.service;

import java.io.File;

public class HostInfo {

	private String name;
	private String location;
	private String likeTable;
	private String directory;
	private int port;

	public HostInfo(String hostName, int hostPort, String directory, String location, String likeTable) {
		this.name = hostName;
		this.port = hostPort;
		this.directory = directory;
		this.location = location;
		this.likeTable = likeTable;
	}

	public String getName() {
		return name;
	}

	public int getPort() {
		return port;
	}

	public String getLocation() {
		if (this.location == null || location.length() == 0) {
			return File.separator;
		} else {
			if (!this.location.endsWith("/")) {
				return this.location + File.separatorChar;
			}
		}
		return location;
	}

	public String getLikeTable() {
		return likeTable;
	}

	public String getDirectory() {
		if (this.directory == null || directory.length() == 0) {
			return File.separator;
		} else {
			if (!this.directory.endsWith("/")) {
				return this.directory + File.separatorChar;
			}
		}
		return directory;
	}
}
