package edu.buffalo.cse.cse486586.simpledht;

/**
 * Device Information - Static Class for storing the Port information and
 * fetching the device name from the port defined
 * 
 * @author Babu
 */
public class DeviceInfo {
	
	static final String[] REMOTE_PORTS = {	"11108", 
											"11112",
											"11116",
											"11120",
											"11124"};     
	static final int SERVER_PORT = 10000;
	
	static final String BASE_PORT = REMOTE_PORTS[0];
	
	/**
	 * Get device name from the Port Number
	 * @param portNo -  Port No
	 * @return - Device Name
	 */
	public static String getDeviceName(String portNo)
	{		
		for (int i = 0; i < REMOTE_PORTS.length; i++) {
			if(portNo.equals(REMOTE_PORTS[i]))
				return "avd"+i;
		}
		return "avd-invalid";
	}

}
