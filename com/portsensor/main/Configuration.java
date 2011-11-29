package com.portsensor.main;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.SAXBuilder;

import com.portsensor.sensor.ConsoleSensor;
import com.portsensor.sensor.SensorCheck;

public class Configuration {
	private ArrayList<ConsoleSensor> sensors = new ArrayList<ConsoleSensor>();
	private HashMap<String,String> settings = new HashMap<String,String>();
	static private Configuration instance = null;
	
	static public final String SETTING_PORTAL_URL = "portal_url"; 
	static public final String SETTING_MONITOR_GUID = "monitor_guid"; 
	static public final String SETTING_SECRET_KEY = "secret_key"; 
	
	private Configuration() {}
	
	static public Configuration getInstance() {
		if(null == Configuration.instance) {
			Configuration.instance = new Configuration();
		}
		return Configuration.instance;
	}
	
	public boolean load(String cfgFile) {
		Document doc = this.getConfigFileAsXml(cfgFile);
		Element eRoot = doc.getRootElement();
	    Iterator<Element> i;
	    
	    // Settings
	    List<Element> eSettings = eRoot.getChild("settings").getChildren();
	    i = eSettings.iterator();
	    while(i.hasNext()) {
	    	Element eSetting = (Element) i.next();
	    	settings.put(eSetting.getName(), eSetting.getTextTrim());
	    }
	    
	    // Devices
	    List<Element> eDevices = eRoot.getChildren("device");
	    i = eDevices.iterator();
	    while(i.hasNext()) {
	    	Element eDevice = (Element) i.next();
	    	String sDeviceId = eDevice.getAttributeValue("id");
	    	
	    	// Sensors
		    List<Element> eSensors = eDevice.getChildren("sensor");
		    Iterator<Element> j = eSensors.iterator();
		    while(j.hasNext()) {
		    	Element eSensor = (Element) j.next();
		    	
		    	// Validate and normalize datatype
		    	String eType = eSensor.getChildText("type");
		    	String sType = ConsoleSensor.TYPE_TEXT; // default
		    	
		    	if(null != eType) {
			    	if(eType.equalsIgnoreCase("number")) {
			    		sType = ConsoleSensor.TYPE_NUMBER;
			    	} else if(eType.equalsIgnoreCase("percent")) {
			    			sType = ConsoleSensor.TYPE_PERCENT;
			    	} else if(eType.equalsIgnoreCase("decimal")) {
			    		sType = ConsoleSensor.TYPE_DECIMAL;
			    	}
		    	}
		    	
		    	ConsoleSensor sensor = new ConsoleSensor(
		    			sDeviceId,
		    			eSensor.getChildText("name"),
		    			eSensor.getChildText("command"),
		    			sType
		    	);
		    	
		    	List<SensorCheck> checks = new ArrayList<SensorCheck>();
		    	
		    	// Warnings
		    	List<Element> eWarnings = eSensor.getChildren("warning");
		    	Iterator<Element> k = eWarnings.iterator();
		    	
		    	while(k.hasNext()) {
		    		Element eWarning = (Element) k.next();
		    		checks.add(new SensorCheck(
		    			SensorCheck.STATUS_WARNING,
		    			eWarning.getAttributeValue("oper"),
		    			eWarning.getAttributeValue("value"),
		    			eWarning.getText()
		    		));
		    	}
		    	
		    	// Criticals
		    	List<Element> eCriticals = eSensor.getChildren("critical");
		    	k = eCriticals.iterator();
		    	
		    	while(k.hasNext()) {
		    		Element eCritical = (Element) k.next();
		    		checks.add(new SensorCheck(
			    			SensorCheck.STATUS_CRITICAL,
			    			eCritical.getAttributeValue("oper"),
			    			eCritical.getAttributeValue("value"),
			    			eCritical.getText()
			    		));
		    	}
		    	
		    	sensor.setChecks(checks);
		    	this.sensors.add(sensor);
		    }
	    }
	    
	    return true;
	}

	public String getSetting(String key, String defaultValue) {
		if(this.settings.containsKey(key)) {
			return this.settings.get(key);
		}
		
		return (null != defaultValue) ? defaultValue : null;
	}
	
	public ArrayList<ConsoleSensor> getSensors() {
		return sensors;
	}

	private Document getConfigFileAsXml(String cfgFile) {
		SAXBuilder builder = new SAXBuilder();
		Document doc = null;
		
		try {
			doc = builder.build(cfgFile);
		} catch (JDOMException e) {
			// [JAS]: [TODO] Logger
	        System.err.println(cfgFile + " is not well-formed.");
	        System.err.println(e.getMessage());
	        return null;
	        
	    } catch (IOException e) {
	    	// [JAS]: [TODO] Logger	    	
	        System.err.println("Could not check " + cfgFile);
	        System.err.println(" because " + e.getMessage());
	        return null;
		}
	    
	    return doc; // Configuration
	}
	
	public void printDevices() {
		TreeSet<String> devices = new TreeSet<String>();
		ArrayList<ConsoleSensor> sensors = this.getSensors();
		Iterator<ConsoleSensor> i = sensors.iterator();
		
		while(i.hasNext()) {
			ConsoleSensor sensor = i.next();
			devices.add(sensor.getDeviceId());
		}
		
		Iterator<String> iDevices = devices.iterator(); 
		while(iDevices.hasNext()) {
			System.out.println(iDevices.next());
		}
	}
	
	public void printSensors(String cfgFile, String deviceId) {
		Document doc = this.getConfigFileAsXml(cfgFile);
		Element eRoot = doc.getRootElement();
	    
		List<Element> eDevices = eRoot.getChildren("device");
	    Iterator<Element> i = eDevices.iterator();
	    
	    while(i.hasNext()) {
	    	Element eDevice = (Element) i.next();
	    	
	    	// Find Device
	    	if(eDevice.getAttributeValue("id").equalsIgnoreCase(deviceId)) {
	    		List<Element> eSensors = eDevice.getChildren("sensor");
	    		Iterator<Element> iSensors = eSensors.iterator();
	    		
	    		// Make sure we aren't duplicating the sensor name in this device
	    		while(iSensors.hasNext()) {
	    			Element e = (Element) iSensors.next();
	    			System.out.println(e.getChildText("name"));
	    		}
	    	}
	    }		
	}
	
	public void addPort(String cfgFile, String deviceId, String hostName, String portName, String port) {
		Document doc = this.getConfigFileAsXml(cfgFile);
		Element eRoot = doc.getRootElement();
	    
		List<Element> eDevices = eRoot.getChildren("device");
	    Iterator<Element> i = eDevices.iterator();
	    
	    while(i.hasNext()) {
	    	Element eDevice = (Element) i.next();
	    	
	    	// Find Device
	    	if(eDevice.getAttributeValue("id").equalsIgnoreCase(deviceId)) {
	    		List<Element> eSensors = eDevice.getChildren("sensor");
	    		Iterator<Element> iSensors = eSensors.iterator();
	    		
	    		// Make sure we aren't duplicating the sensor name in this device
	    		while(iSensors.hasNext()) {
	    			Element e = (Element) iSensors.next();
	    			if(e.getChildText("name").equalsIgnoreCase(portName)) {
	    				System.err.println("Sensor '"+portName+"' already exists on '"+deviceId+"'.");
	    				return;
	    			}
	    		}
	    		
				Element eSensor = new Element("sensor");
				eDevice.addContent(eSensor);
				eSensor.addContent(new Element("name").setText(portName));
				eSensor.addContent(new Element("command").setText("#PORT " + hostName + " " + port));
				eSensor.addContent(new Element("type").setText("text"));
				Element eCritical = new Element("critical"); 
				eSensor.addContent(eCritical);
				eCritical.setAttribute("oper", "eq");
				eCritical.setAttribute("value", "DOWN");
				eCritical.setText("Critical");
	    	}
	    }
	    
	    ConfigurationBuilder.saveConfigXml(doc, cfgFile);
	}

	public void removeSensor(String cfgFile, String deviceId, String sensorName) {
		Document doc = this.getConfigFileAsXml(cfgFile);
		Element eRoot = doc.getRootElement();
	    
		List<Element> eDevices = eRoot.getChildren("device");
	    Iterator<Element> i = eDevices.iterator();
	    
	    while(i.hasNext()) {
	    	Element eDevice = (Element) i.next();
	    	
	    	// Find Device
	    	if(deviceId.equals("all") || eDevice.getAttributeValue("id").equalsIgnoreCase(deviceId)) {
	    		List<Element> eSensors = eDevice.getChildren("sensor");
	    		Iterator<Element> iSensors = eSensors.iterator();
	    		
	    		// Make sure we aren't duplicating the sensor name in this device
	    		while(iSensors.hasNext()) {
	    			Element e = (Element) iSensors.next();
	    			if(e.getChildText("name").equalsIgnoreCase(sensorName)) {
//	    				System.err.println("Sensor '"+portName+"' already exists on '"+deviceId+"'.");
	    				eDevice.removeContent(e);
	    				break;
	    			}
	    		}
	    	}
	    }
	    
	    ConfigurationBuilder.saveConfigXml(doc, cfgFile);
	}	
	
//	public void removePort(String deviceId, String portName, Integer port) {
//	}
}
