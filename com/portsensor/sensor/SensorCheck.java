package com.portsensor.sensor;

import java.text.DecimalFormat;
import java.text.ParseException;

public class SensorCheck {
	public static final int STATUS_OK = 0;
	public static final int STATUS_WARNING = 1;
	public static final int STATUS_CRITICAL = 2;
	
	public static final String OPER_EQ = "eq";
	public static final String OPER_NEQ = "neq";
	public static final String OPER_LT = "lt";
	public static final String OPER_GT = "gt";
	
	private int status = STATUS_OK;
	private String message = "";
	private String oper = "";
	private String value = "";
	
	public SensorCheck(int status, String oper, String value, String message) {
		this.status = status;
		this.oper = oper;
		this.value = value;
		this.message = message;
	}
	
	/**
	 * Returns true if the check passes (usually bad) and false otherwise
	 * 
	 * @param String metric
	 * @return boolean
	 */
	public boolean check(String metric) {
		// Equal
		if(this.oper.equals(OPER_EQ)) {
			if(this.value.equals(metric))
				return true;
		}
		
		// Not Equal
		if(this.oper.equals(OPER_NEQ)) {
			if(!this.value.equals(metric))
				return true;
		}
		
		// Less Than
		// Greater Than
		if(this.oper.equals(OPER_LT) || this.oper.equals(OPER_GT)) {
			try {
				double dMetric = DecimalFormat.getNumberInstance().parse(metric).doubleValue();
				double dValue = DecimalFormat.getNumberInstance().parse(this.value).doubleValue();
				
				if(this.oper.equals(OPER_LT) && dMetric < dValue)
					return true;
				
				if(this.oper.equals(OPER_GT) && dMetric > dValue)
					return true;
				
			} catch(ParseException pe) {
				pe.printStackTrace();
				return true;
			}
		}
		
		return false;
	}

	public String getMessage() {
		return message;
	}

	public int getStatus() {
		return status;
	}
}
