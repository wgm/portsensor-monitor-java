/**
 * @author Jeff Standen <jeff@webgroupmedia.com>
 */

package com.portsensor.main;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeMap;

import org.hyperic.sigar.FileSystemUsage;
import org.hyperic.sigar.ProcCpu;
import org.hyperic.sigar.ProcCredName;
import org.hyperic.sigar.ProcMem;
import org.hyperic.sigar.ProcTime;
import org.hyperic.sigar.ProcUtil;
import org.hyperic.sigar.Sigar;
import org.hyperic.sigar.SigarException;
import org.hyperic.sigar.Who;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import sun.misc.BASE64Encoder;

import com.portsensor.command.CommandFactory;
import com.portsensor.sensor.ConsoleSensor;
import com.portsensor.sensor.SensorCheck;
import com.portsensor.testers.PortTester;


public class Main {

	@SuppressWarnings("serial")
	public static class PSSensorCheckException extends Exception {
		public PSSensorCheckException(Throwable cause) {
			super("Exception checking sensor", cause);
		}
	}

	@SuppressWarnings("serial")
	public static class PSSensorResultParseException extends Exception {
		public PSSensorResultParseException(Throwable cause) {
			super("Exception parsing Sensor result", cause);
		}
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		if(1 == args.length && args[0].equalsIgnoreCase("--config")) {
			ConfigurationBuilder.autoConfigure();
			
		} else if(0 == args.length || 1 == args.length && args[0].equalsIgnoreCase("--help")) {
			Main.printHelp();
			
		} else {
			Configuration cfg = Configuration.getInstance();
			if(!cfg.load(args[0])) {
				System.exit(1);
			}

			if(2 == args.length && args[1].equalsIgnoreCase("--list-devices")) {
				cfg.printDevices();
			} else if(3 == args.length && args[1].equalsIgnoreCase("--list-sensors")) {
				cfg.printSensors(args[0], args[2]);
			} else if(6 == args.length && args[1].equalsIgnoreCase("--add-sensor")) {
				cfg.addPort(args[0], args[2], args[3], args[4], args[5]);
			} else if(4 == args.length && args[1].equalsIgnoreCase("--remove-sensor")) {
				cfg.removeSensor(args[0], args[2], args[3]);
			} else if(2 == args.length && args[1].equalsIgnoreCase("--test")) {
				String xml = Main.getXML();
				System.out.println(xml);
			} else if(1==args.length) {
				String xml = Main.getXML();
				Main.postXML(xml);
			} else {
				Main.printHelp();
			}
			
		}
		
		System.exit(0);
	}
	
	private static void printHelp() {
		System.out.println("Syntax:");
		System.out.println("<config file>");
		System.out.println("<config file> --test");
		System.out.println("<config file> --list-devices");
		System.out.println("<config file> --list-sensors <device>");
		System.out.println("<config file> --add-sensor <device> <host> <service> <port>");
		System.out.println("<config file> --remove-sensor <device> <sensor>");
		System.out.println("--config");
		System.out.println("--help");
	}
	
	private static String getXML() {
		Configuration cfg = Configuration.getInstance();
		Sigar sigar = new Sigar();
		
		String monitor_guid = cfg.getSetting(Configuration.SETTING_MONITOR_GUID, "");

		// Formatters
		NumberFormat loadFormatter = DecimalFormat.getNumberInstance();
		loadFormatter.setMaximumFractionDigits(2);
		
		SimpleDateFormat dateFormatter = new SimpleDateFormat("MMM dd HH:mm");
		
		// Output XML
		Element eRoot = new Element("sensors");
		Document doc = new Document(eRoot);
		
		Element eMonitorGUID = new Element("monitor_guid");
		eMonitorGUID.setText(monitor_guid);
		
		eRoot.addContent(eMonitorGUID);
		
		Iterator<ConsoleSensor> iCommands = cfg.getSensors().iterator();
		while(iCommands.hasNext()) {
			ConsoleSensor sensor = iCommands.next();
			String cmd = sensor.getCommand();
			int iStatus = SensorCheck.STATUS_OK;

			String sSensorOut = "";
			String sOut = "";
			
			// [TODO] Move commands into their own classes
			
			try {
				
				// Built-in commands
				if(cmd.startsWith("#PORT")) {
					try {
						String[] parts = cmd.split(" ");
						String sHost = parts[1];
						Integer iPort = Integer.parseInt(parts[2]);
						sSensorOut = (PortTester.testPort(sHost, iPort)) ? "UP" : "DOWN";
					} catch(Exception e) {
						throw new PSSensorCheckException(e);
					}
					
				} else if(cmd.startsWith("#WHO")) {
					StringBuilder str = new StringBuilder();
					try {
						Who[] whos = sigar.getWhoList();
						for (Who who : whos) {
							String host = who.getHost();
							str.append(String.format("%s\t%s\t%s%s\n", 
									new Object[] {
										who.getUser(), 
										who.getDevice(), 
										dateFormatter.format(new Date(who.getTime()*1000)),
										((0 == host.length()) ? "" : String.format("\t(%s)", host))
									}));
						}
						sSensorOut = str.toString();
					} catch (SigarException e) {
						throw new PSSensorCheckException(e);
					} catch(Exception e) {
						throw new PSSensorCheckException(e);
					}
	
				} else if(cmd.startsWith("#DF")) {
					String[] parts = cmd.split(" ");
					String sFileSystem = parts[1];
					try {
						FileSystemUsage usage = sigar.getFileSystemUsage(sFileSystem);
						sSensorOut = Double.toString(usage.getUsePercent());
					} catch (SigarException e) {
						throw new PSSensorCheckException(e);
					} catch(Exception e) {
						throw new PSSensorCheckException(e);
					}
						
				} else if(cmd.startsWith("#LOAD")) {
					try {
						double loads[] = sigar.getLoadAverage();
						sSensorOut = String.valueOf(loadFormatter.format(loads[1])); // 5 min avg
					} catch (SigarException e) {
						throw new PSSensorCheckException(e);
					} catch(Exception e) {
						throw new PSSensorCheckException(e);
					}
					
				} else if(cmd.startsWith("#PS")) {
					String[] parts = cmd.split(" ");
					String sSortBy = (2==parts.length) ? parts[1] : "mem";
					
					try {
						StringBuilder str = new StringBuilder();
						String sProcUser = "";
						
						// Sort by the desired criteria
						TreeMap<Long, String> pidTree = new TreeMap<Long, String>(Collections.reverseOrder()); 
						long[] pids = sigar.getProcList();
						for(long pid : pids) {
							ProcCredName procUser = sigar.getProcCredName(pid);
							sProcUser = procUser.getUser();
							
							try {
								if(sSortBy.equals("cpu")) { // [TODO] Format CPU time
									ProcCpu procCpu = sigar.getProcCpu(pid);
									String out = String.format("%s\t[%d]\t%s\t%s\n", new Object[] {
											procCpu.getSys(),
											pid,
											ProcUtil.getDescription(sigar, pid),
											sProcUser
										});
									pidTree.put(procCpu.getSys(), out);
									
								} else if(sSortBy.equals("time")) { // [TODO] Format run time
									ProcTime procTime = sigar.getProcTime(pid);
									String out = String.format("%s\t[%d]\t%s\t%s\n", new Object[] {
											procTime.getSys(),
											pid,
											ProcUtil.getDescription(sigar, pid),
											sProcUser
										});
									pidTree.put(procTime.getSys(), out);
									
								} else { // "mem"
									ProcMem procMem = sigar.getProcMem(pid);
									String out = String.format("%s\t[%d]\t%s\t%s\n", new Object[] {
											Sigar.formatSize(procMem.getResident()),
											pid,
											ProcUtil.getDescription(sigar, pid),
											sProcUser
										});
									pidTree.put(procMem.getResident(), out);
									
								}
							} catch(Exception e) { /* ignore partial processes */ }
						}
						
						// Make sure we have 5 max items [TODO] Make configurable?
						while(pidTree.size() > 5) {
							pidTree.remove(pidTree.lastKey());
						}
						
						// Lazy load the remaining items
						for(String process : pidTree.values()) {
							str.append(process);
						}
						
						sSensorOut = str.toString();
						
					} catch (SigarException e) {
						throw new PSSensorCheckException(e);
					} catch(Exception e) {
						throw new PSSensorCheckException(e);
					}
					
				} else { // command wrapper
					try {
						sSensorOut = CommandFactory.quickRun(cmd).trim();
					}
					catch(Exception e) {
						throw new PSSensorCheckException(e);
					}
				}
				
				try {
					// Typecast as requested
					if(sensor.getType().equals(ConsoleSensor.TYPE_NUMBER)) {
						sSensorOut = String.valueOf(DecimalFormat.getNumberInstance().parse(sSensorOut).longValue());
					} else if(sensor.getType().equals(ConsoleSensor.TYPE_PERCENT)) {
						sSensorOut = String.valueOf(Math.round(100*DecimalFormat.getNumberInstance().parse(sSensorOut).doubleValue())) + "%";
					} else if (sensor.getType().equals(ConsoleSensor.TYPE_DECIMAL)) {
						sSensorOut = String.valueOf(DecimalFormat.getNumberInstance().parse(sSensorOut).doubleValue());
					}
	
					sOut = sSensorOut;
					
					// Rule check
					Iterator<SensorCheck> i = sensor.getChecks().iterator();
					while(i.hasNext()) {
						SensorCheck rule = i.next();
						if(rule.check(sSensorOut)) {
							iStatus = rule.getStatus();
							sOut = rule.getMessage() + ": " + sSensorOut;
						}
					}
	
				} catch(ParseException e) {
					throw new PSSensorResultParseException(e);
				} catch(Exception e) {
					throw new PSSensorResultParseException(e);
				}
			
			} catch(PSSensorCheckException e) {
				e.printStackTrace();
				iStatus = SensorCheck.STATUS_CRITICAL;
				sOut = "There was a problem checking "+sensor.getName() +". " + e.getMessage();
			} catch(PSSensorResultParseException e) {
				e.printStackTrace();
				iStatus = SensorCheck.STATUS_CRITICAL;
				sOut = "Sensor result was not a valid "+sensor.getType() + ". "+e.getMessage();
			}
			
			//if(0 != sSensorOut.length()) {
				Element e = new Element("sensor");
				e.addContent(new Element("name").setText(sensor.getName()));
				e.addContent(new Element("device").setText(sensor.getDeviceId()));
				e.addContent(new Element("status").setText(String.valueOf(iStatus)));
				e.addContent(new Element("metric").setText(sSensorOut));
				e.addContent(new Element("output").setText(sOut));
				e.addContent(new Element("runtime").setText(String.valueOf(System.currentTimeMillis()/1000)));
				eRoot.addContent(e);
			//}
		}

	    XMLOutputter outputter = new XMLOutputter();
	    outputter.setFormat(Format.getPrettyFormat());
    	return outputter.outputString(doc);       
	}
	
	private static void postXML(String xml) {
		Configuration cfg = Configuration.getInstance();
        BASE64Encoder encoder = new BASE64Encoder();
    	MessageDigest md = null;
        
        try {
			md = MessageDigest.getInstance("SHA-1");
        } catch(NoSuchAlgorithmException nse) {
        	System.out.println("Error: Could not generate SHA-1 hash.");
        	System.exit(1);
        }

		String monitor_guid = cfg.getSetting(Configuration.SETTING_MONITOR_GUID, "");
		String secret_key = cfg.getSetting(Configuration.SETTING_SECRET_KEY, "");
		String postUrl = cfg.getSetting(Configuration.SETTING_PORTAL_URL, "");

		String httpDate = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss Z").format(new Date()).toString();
		
		System.out.println("Posting to " + postUrl);

		try {
	        // Construct data
	        String stringToSign = "POST\n"+httpDate+"\n"+xml+"\n"+secret_key+"\n";
	    	md.update(stringToSign.getBytes("iso-8859-1"));
	    	ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    	baos.write(md.digest());
	    	String signedString = encoder.encode(baos.toByteArray()).toString();
	    	
	        // Send data
	        URL url = new URL(postUrl);
	        URLConnection conn = url.openConnection();
	        conn.setDoOutput(true);
//	        conn.setDoInput(false);
	        conn.addRequestProperty("Portsensor-Auth", monitor_guid+":"+signedString);
	        conn.addRequestProperty("Date", httpDate);
	        OutputStreamWriter wr = new OutputStreamWriter(conn.getOutputStream());
	        wr.write(xml);
	        wr.flush();
	    
	        // Get the response
	        BufferedReader rd = new BufferedReader(new InputStreamReader(conn.getInputStream()));
	        String line;
	        while ((line = rd.readLine()) != null) {
	            // Process line...
	        	System.out.println(line);
	        }
	        wr.close();
	        rd.close();
	        
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }		
	}

}
