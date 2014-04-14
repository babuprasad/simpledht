package edu.buffalo.cse.cse486586.simpledht;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Formatter;
import android.util.Log;

/**
 * POJO Node class which has the DHT node properties
 * and methods 
 * @author Babu Prasad
 *
 */
public class Node {
	private String nodeID;
	private String deviceID;
	private String prevNodeID;
	private String nextNodeID;
	private String prevDeviceID;
	private String nextDeviceID;
	
	/**
	 * Singleton Instance as we need only one Node object per App/Device
	 */
	private static Node nodeInstance;
	
	
	private Node() {
		// Private constructor to defeat instantiation
	}
	
	/**
	 * Initialize Singleton Node instance
	 * @return Singleton Node Instance
	 */
	public static void initNodeInstance(String emulatorPort)
	{
		if(nodeInstance == null)
		{
			try {				
				String deviceID = DeviceInfo.getDeviceName(emulatorPort);
				if(deviceID.compareTo("InvalidPortNo") == 0)
					throw new Exception("Node Initailization failed -- "+deviceID);
				
				nodeInstance = new Node();
				nodeInstance.deviceID = deviceID;				
				nodeInstance.prevDeviceID = deviceID;
				nodeInstance.nextDeviceID = deviceID;
								 
				nodeInstance.nodeID =  genHash(deviceID);
				nodeInstance.prevNodeID =  genHash(deviceID);
				nodeInstance.nextNodeID =  genHash(deviceID);
			} 
			catch (NoSuchAlgorithmException e) {
				Log.e("Node", "Node instance creation failed : "+e.getMessage());
				e.printStackTrace();
			} catch (Exception e) {
				Log.e("Node", "Node instance creation failed : "+e.getMessage());
				e.printStackTrace();
			}
		}
		
	}
	
	/**
	 * Get Singleton Node instance
	 * @return Node Instance
	 * @throws Exception - No Node initialization
	 */
	public static Node getInstance() throws Exception
	{
		/* Make sure that initNodeInstance is called before getInstance*/
		if(nodeInstance == null)
		{
			throw new Exception("No Node initialization");
		}
		return nodeInstance;
	}

	/********************************************************************
	 * Node ID Helper Functions - Babu
	 *******************************************************************/
	
	/**
	 * @return the nodeID
	 */
	public String getNodeID() {
		return nodeID;
	}

	/**
	 * @param nodeID the nodeID to set
	 */
	public void setNodeID(String nodeID) {
		this.nodeID = nodeID;
	}


	/**
	 * @return the prevNodeID
	 */
	public String getPrevNodeID() {
		return prevNodeID;
	}

	/**
	 * @param prevNodeID the prevNodeID to set
	 */
	public void setPrevNodeID(String prevNodeID) {
		this.prevNodeID = prevNodeID;
	}

	/**
	 * @return the nextNodeID
	 */
	public String getNextNodeID() {
		return nextNodeID;
	}

	/**
	 * @param nextNodeID the next node id to set
	 */
	public void setNextNodeID(String nextNodeID) {
		this.nextNodeID = nextNodeID;
	}
	
	
	/********************************************************************
	 * Device ID Helper Functions - Babu
	 *******************************************************************/
	
	/**
	 * @return the device id
	 */
	public String getDeviceID() {
		return deviceID;
	}
	

	/**
	 * @param deviceID the emulatorPort to set
	 */
	public void setDeviceID(String deviceID) {
		this.deviceID = deviceID;
	}
	
	/**
	 * @return the prevDeviceID
	 */
	public String getPrevDeviceID() {
		return prevDeviceID;
	}

	/**
	 * @param prevDeviceID the previous device id to set
	 */
	public void setPrevDeviceID(String prevDeviceID) {
		this.prevDeviceID = prevDeviceID;
	}

	/**
	 * @return the next Device Id
	 */
	public String getNextDeviceID() {
		return nextDeviceID;
	}

	/**
	 * @param nextDeviceID the next device id to set
	 */
	public void setNextDeviceID(String nextDeviceID) {
		this.nextDeviceID = nextDeviceID;
	}
	
	
	/**
     * Generate Hash function using SHA1 for key in DHT implementation
     * @param input - key
     * @return Hash value
     * @throws NoSuchAlgorithmException
     */
    public static String genHash(String input) throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
        byte[] sha1Hash = sha1.digest(input.getBytes());
        Formatter formatter = new Formatter();
        for (byte b : sha1Hash) {
            formatter.format("%02x", b);
        }
        
        String formattedString = formatter.toString();
        formatter.close();
        return formattedString;
    }
	
    /**
    * Node Lookup for the key insert/query/delete operation
   	* @param key - Key to be inserted/queried/deleted
   	* @return NODE - CURRENT, PREVIOUS or NEXT
   	* @throws Exception
   	* 
   	* @author Babu
   	*/
	public static NODE nodeLookup(String key) throws Exception
	{
		Node node = Node.getInstance();
		String hashkey = Node.genHash(key);
		Log.i("NodeLookup", "key : "+key);
		Log.i("NodeLookup", "Hash key : "+hashkey);
		
		/* 
		 * If wildcard is used then return current node else check whether
		 * key to be inserted lies between the previous node id and the current node id 
		 * or greater than the current node
		 */
		//Log.i("NodeLookup", "Already visited : "+node.alreadyVisited.keySet());
		if(key.compareTo("*") == 0 || key.compareTo("@") == 0)
		{
			Log.i("NodeLookup", "key is * or @");
			return NODE.CURRENT;
		}
		else if(node.getNodeID().compareTo(node.getNextNodeID()) == 0 || node.getNodeID().compareTo(node.getPrevNodeID()) == 0)
		{
			Log.i("NodeLookup", "Key belongs to current node as node and prev/next are same");
			return NODE.CURRENT;
		}
		
		else if (hashkey.compareTo(node.getNodeID()) <= 0 && hashkey.compareTo(node.getPrevNodeID()) > 0)		
		{
			Log.i("NodeLookup", "key belongs to current node as key is lesser than node and greater than prev nodeid");
			return NODE.CURRENT;
		}

		else if(node.getPrevNodeID().compareTo(node.getNodeID()) > 0 && hashkey.compareTo(node.getPrevNodeID()) > 0
				&& hashkey.compareTo(node.getNodeID()) > 0)
		{
			Log.e("NodeLookup", "CORNER CASE -- Key belongs to current node as node.prev is greater than node id");
			return NODE.CURRENT;
		}
		else if(node.getPrevNodeID().compareTo(node.getNodeID()) > 0 && hashkey.compareTo(node.getPrevNodeID()) < 0
				&& hashkey.compareTo(node.getNodeID()) < 0)
		{
			Log.e("NodeLookup", "CORNER CASE -- Key belongs to current node as node.prev is greater than node id");
			return NODE.CURRENT;
		}
		
		else
		{
			Log.e("NodeLookup", "key goes to next node as none of the condition is met");
			return NODE.NEXT;
		}
	}
    
	/**
	 * Enum for NODE 
	 * @author Babu 
	 *
	 */
    public enum NODE
    {
    	PREVIOUS,
    	CURRENT,
    	NEXT
    };
}
