package com.portsensor.sensor;

import java.util.ArrayList;
import java.util.List;

public class ConsoleSensor {
	private String deviceId = "";
	private String name = "";
	private String command = "";
	private String type = ConsoleSensor.TYPE_TEXT;
	private List<SensorCheck> checks = new ArrayList<SensorCheck>();
	
	public static final String TYPE_TEXT = "text";
	public static final String TYPE_NUMBER = "number";
	public static final String TYPE_PERCENT = "percent";
	public static final String TYPE_DECIMAL = "decimal";
	
	public ConsoleSensor(String deviceId, String name, String command, String type) {
		this.setDeviceId(deviceId);
		this.setName(name);
		this.setCommand(command);
		this.setType(type);
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public String getDeviceId() {
		return deviceId;
	}

	public void setDeviceId(String deviceId) {
		this.deviceId = deviceId;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public List<SensorCheck> getChecks() {
		return checks;
	}

	public void setChecks(List<SensorCheck> checks) {
		this.checks = checks;
	}
}
