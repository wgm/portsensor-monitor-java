package com.portsensor.testers;

import java.net.Socket;

public class PortTester {
	public static boolean testPort(String host, int port) {
		try {
			Socket s = new Socket(host, port);
//			DataOutputStream os = new DataOutputStream(s.getOutputStream());
//			DataInputStream is = new DataInputStream(s.getInputStream());
			s.close();
			return true;
			
		} catch(Exception e) {
			return false;
		}
	}
}
