package edu.buffalo.cse.cse486586.simpledht;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map.Entry;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class SimpleDhtProvider extends ContentProvider {
	
	public static final String TAG = SimpleDhtProvider.class.getSimpleName();		
	private boolean isProcessComplete = false;
	private String resultCursorString = "";
	private int resultDeletedRows = 0;
	private Object lock = new Object();
	
    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
    	
    	int retVal = 0;
    	try {
			/*
			 * Addded By Babu - Code to query key and values from the storage
			 * from the shared preference used during insert function
			 */
			Log.v(TAG,"delete content provider - start");
			Log.v(TAG,"uri :" + uri);
			Log.v(TAG,"selection :" + selection);
			
			Node node = Node.getInstance();
			SharedPreferences sharedPreference = getContext().getSharedPreferences("SimpleDhtSP", Context.MODE_PRIVATE);
			String target = "";
		
			// Forming MessagePacket incase of forwarding
			MessagePacket msgPacket = new MessagePacket();
			msgPacket.setMsgId(selection);
			msgPacket.setMsgType(MSG_TYPE.REQUEST);
			msgPacket.setMsgOperation(MSG_OPER.DELETE);
			msgPacket.setMsgInitiator(node.getDeviceID());
			
			/* 
			 * If key to be inserted lies between the previous node id and the current node id 
			 * then insert into the local storage
			 */
			switch(Node.nodeLookup(selection))
			{
				/*
				 * As we are using shared preference which replace the existing value, 
				 * we check for key existence before insert operation - Babu 
				 */
				case CURRENT:		
				{
					if(selection.compareTo("*") == 0 || selection.compareTo("@") == 0)
					{	
						Log.v(TAG,"Deleting all rows...");
						retVal = sharedPreference.getAll().size();
						Editor editor = sharedPreference.edit();				
						editor.clear();
						editor.commit();				
						Log.v(TAG,"All rows deleted...");
						
						// Forward the request if the selection is '*' and current node is the initiator
						if(selection.compareTo("*") == 0 && node.getNodeID().compareTo(node.getNextNodeID()) != 0)
						{
							target = node.getNextDeviceID();							
							MessagePacket.sendMessage(target, msgPacket);
							isProcessComplete = false;
						}
						else
							msgPacket = null;
					}
					else
					{
						Log.v(TAG, "Deleting one row ..");
						Editor editor = sharedPreference.edit();
						editor.remove(selection);
						editor.commit();
						retVal = 1;
						isProcessComplete = true;
					}					
					break;
				}
				/* 
				 * Forward the insert request to next ID if the node id is greater than current node id
				 * or lesser the previous node id
				 */
				case NEXT:			
				{
					target = node.getNextDeviceID();					
					MessagePacket.sendMessage(target, msgPacket);
					isProcessComplete = false;
					break;
				}	
				case PREVIOUS:
				{
					target = node.getPrevDeviceID();					
					MessagePacket.sendMessage(target, msgPacket);					
					isProcessComplete = false;
					break;
				}
				default:
				{
					Log.e(TAG,"Invalid Lookup node");
				}
			}
			

			if(selection.compareTo("@") != 0)
			{
				// Dont return until the initiator node gets value from all the nodes
				while(!isProcessComplete);
				
				// Adding total rows deleted across all nodes
				retVal += resultDeletedRows;				
				resultDeletedRows = 0;
			}		
			
			Log.v(TAG,"delete content provider - end ");
			
		} catch (Exception e) {
			Log.e(TAG, "Exception occured. ");
			e.printStackTrace();
			retVal = 0;
		}    
        return retVal;        
    }

    @Override
    public String getType(Uri uri) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {

    	Log.v("SimpleDhtProvider","insert Content Provider - start");
    	Log.v("SimpleDhtProvider", "Content Values" + values.toString());
    	
        try {
        	/* Fetch the device node and then insert the data*/
        	Node node = Node.getInstance();
        	
			// Declaring SharedPreference as Data Storage - Babu
			SharedPreferences sharedPreference = getContext().getSharedPreferences("SimpleDhtSP", Context.MODE_PRIVATE);
			String keyToInsert = values.getAsString("key");
			String valueToInsert = values.getAsString("value");
			String target = "";
			
			// Forming MessagePacket incase of forwarding
			MessagePacket msgPacket = new MessagePacket(keyToInsert, valueToInsert);
			msgPacket.setMsgType(MSG_TYPE.REQUEST);
			msgPacket.setMsgOperation(MSG_OPER.INSERT);
			msgPacket.setMsgInitiator(node.getDeviceID());
			
			/* 
			 * If key to be inserted lies between the previous node id and the current node id 
			 * then insert into the local storage
			 */
			switch(Node.nodeLookup(keyToInsert))
			{
				/*
				 * As we are using shared preference which replace the existing value, 
				 * we check for key existence before insert operation - Babu 
				 */
				case CURRENT:		
				{
					Log.v(TAG,"Inserting key/value pair...");
					Log.v(TAG,"key : "+keyToInsert);
					Log.v(TAG,"value : "+valueToInsert);
					Editor editor = sharedPreference.edit();
					editor.putString(keyToInsert, valueToInsert);
					editor.commit();					
					isProcessComplete = true;
					break;
				}
				/* 
				 * Forward the insert request to next ID if the node id is greater than current node id
				 * or lesser the previous node id
				 */
				case NEXT:			
				{
					target = node.getNextDeviceID();					
					MessagePacket.sendMessage(target, msgPacket);
					isProcessComplete = false;
					break;
				}	
				case PREVIOUS:
				{
					target = node.getPrevDeviceID();					
					MessagePacket.sendMessage(target, msgPacket);
					isProcessComplete = false;
					break;
				}
				default:
				{
					Log.e(TAG,"Invalid Lookup node");
				}
			}
		} catch (Exception e) {
			Log.e(TAG, "Exception occured. ");
			e.printStackTrace();
		}
        
        // Dont return until the initiator node gets value from all the nodes		
		while(!isProcessComplete);
        
        Log.v(TAG,"insert Content Provider - end");
        return uri;        
    }

    @Override
    public boolean onCreate() {
    
		  try {
		      /*
		       * Create a server socket as well as a thread (AsyncTask) that listens on the server
		       * port. - Babu
		       * */
			  Log.v(TAG, "Server Socket Start ...");
			  isProcessComplete = false;					  
		      ServerSocket serverSocket = new ServerSocket(DeviceInfo.SERVER_PORT);
		      new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
		      Log.v(TAG, "Server Socket End ...");
		      return true;
		  } catch (IOException e) {
		    
		      Log.e(TAG, "Can't create a ServerSocket");		      
		  }
		  return false;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
            String sortOrder) {

    	try {
			/*
			 * Addded By Babu - Code to query key and values from the storage
			 * from the shared preference used during insert function
			 */
			Log.v(TAG,"query content provider - start");
			Log.v(TAG,"uri :" + uri);
			Log.v(TAG,"selection :" + selection);        
			
			Node node = Node.getInstance();
			// Declaring SharedPreference as Data Storage - Babu
			SharedPreferences sharedPreference = getContext().getSharedPreferences("SimpleDhtSP", Context.MODE_PRIVATE);
			MatrixCursor cursor = null;
			String target = "";
			
			// Forming MessagePacket incase of forwarding
			MessagePacket msgPacket = new MessagePacket();
			msgPacket.setMsgId(selection);
			msgPacket.setMsgType(MSG_TYPE.REQUEST);
			msgPacket.setMsgOperation(MSG_OPER.QUERY);
			msgPacket.setMsgInitiator(node.getDeviceID());
			
			/* 
			 * If key to be inserted lies between the previous node id and the current node id 
			 * then insert into the local storage
			 */
			switch(Node.nodeLookup(selection))
			{
				/*
				 * As we are using shared preference which replace the existing value, 
				 * we check for key existence before insert operation - Babu 
				 */
				case CURRENT:		
				{
					/*
					 * Build cursor from the data retrieved - Babu
					 */
					cursor = new MatrixCursor(new String[]{"key","value"});
					if(selection.compareTo("*") == 0 || selection.compareTo("@") == 0)			
					{	
						Log.v(TAG,"Querying all rows...");
						for (Entry<String, ?> entry : sharedPreference.getAll().entrySet()) {
							cursor.addRow(new String[]{entry.getKey().toString(), entry.getValue().toString()});
						}
						
						Log.v(TAG,"All rows added to cursor");
						
						// Forward the request if the selection is '*' and current node is the initiator
						if(selection.compareTo("*") == 0 && node.getNodeID().compareTo(node.getNextNodeID()) != 0)
						{
							target = node.getNextDeviceID();						
							MessagePacket.sendMessage(target, msgPacket);
							isProcessComplete = false;
						}
						else
							msgPacket = null;
					}
					else
					{
						String value = sharedPreference.getString(selection, "null");
						cursor.addRow(new String[]{selection,value});
						isProcessComplete = true;
						if(value.equals("null"))        
							Log.w(TAG, "Key does not exist");
						else
							Log.v(TAG,"Adding row to cursor");						
					}				
					break;
				}
				/* 
				 * Forward the insert request to next ID if the node id is greater than current node id
				 * or lesser the previous node id
				 */
				case NEXT:			
				{
					target = node.getNextDeviceID();					
					MessagePacket.sendMessage(target, msgPacket);
					isProcessComplete = false;
					break;
				}	
				case PREVIOUS:
				{
					target = node.getPrevDeviceID();					
					MessagePacket.sendMessage(target, msgPacket);					
					isProcessComplete = false;
					break;
				}
				default:
				{
					Log.e(TAG,"Invalid Lookup node");
				}
			}
			

			if(selection.compareTo("@") != 0)
			{
				// Dont return until the initiator node gets value from all the nodes
				while(!isProcessComplete);
				//Thread.currentThread().
				// Appending all the result cursors to form one cursor object 
				if(!resultCursorString.isEmpty())
					resultCursorString += MessagePacket.ROW_DELIMITER;
				
				if(cursor != null)
					resultCursorString += MessagePacket.serializeCursor(cursor);
				if(!resultCursorString.isEmpty())
					cursor = MessagePacket.deSerializeCursor(resultCursorString);
				resultCursorString = "";
			}			
			Log.v(TAG,"query content provider - end ");
			return cursor;
		} catch (Exception e) {
			Log.e(TAG, "Exception occured. ");
			e.printStackTrace();
		}
        
        return null;      

        
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {

    	
        return 0;
    }
	  
    public AsyncTask<ServerSocket, String, Void> getServerTask()
    {
    	return new ServerTask();
    }
		  
	  /*** 
	   * Server Task in each device to receive the message from the socket
	   * and deliver based on the sequence algorithm 
	   * 
	   * @author Babu
	   */
	   public class ServerTask extends AsyncTask<ServerSocket, String, Void> {
		   int msgCount = 0;
	  	 
	      @Override
	      protected Void doInBackground(ServerSocket... sockets) {
	          ServerSocket serverSocket = sockets[0];
	  		String msgRecieved = "";
	                      
	          /*
	           * TODO: Fill in your server code that receives messages and passes them
	           * to onProgressUpdate().
	           */
	  		
	  		/*
	           * Code to accept the client socket connection and read message from the socket. Message read  
	           * from the socket is updated in the Server UI through ProgressUpdate Call.
	           * @author - Babu
	           */  
	          try {            	
	  			while (true) {   // Infinite while loop in order to enable continuous two-way communication 
	  	            // Default Buffer Size is assigned as 128 as PA1 specifications
	  	            byte buffer[] = new byte[128];
	  				Socket socket = serverSocket.accept();					
	  				InputStream in = socket.getInputStream();
	  				if (in.read(buffer) != -1) {
	  					msgRecieved = new String(buffer);
	  					Log.d(TAG, "Message Recieved - " + msgRecieved); 
	  				}
	  				else
	  					Log.e(TAG, "Unable to read buffer data from Socket");
	  				in.close();
	  				msgRecieved = msgRecieved.trim();

	  				// Send to publish progress for further message processing
	  				publishProgress(msgRecieved);
	  				
	  			}
	  			
	  			
	  		} catch (IOException e) {				
	  			Log.e(TAG, "Error in socket connection.");
	  			e.printStackTrace();				
	  		}
	          
	          
	          return null;
	      }

	      protected void onProgressUpdate(String...strings) {
	             	
	      	
	    	  try {
				/* Fetch the device node and then insert the data*/
				  Node node = Node.getInstance();
				  
				  MessagePacket msgPacket = MessagePacket.deSerializeMessage(strings[0].trim());
				  String target = "";
				  isProcessComplete = false; 
				  
				  if(msgPacket.getMsgType() == MSG_TYPE.REQUEST)
				  {
					  					  
					  switch(Node.nodeLookup(msgPacket.getMsgId()))
					  {
						  case CURRENT:
						  {	
							  if(msgPacket.getMsgInitiator().compareTo(node.getDeviceID()) != 0)
							  {
								  							  						
								  Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.simpledht.provider");
								  target = msgPacket.getMsgInitiator();
								  					  
								  MessagePacket responseMsgPacket = new MessagePacket();							  
								  responseMsgPacket.setMsgType(MSG_TYPE.RESPONSE);
								  responseMsgPacket.setMsgId(msgPacket.getMsgId());
								  responseMsgPacket.setMsgOperation(msgPacket.getMsgOperation());
								  responseMsgPacket.setMsgInitiator(node.getDeviceID());
								  
								  if(msgPacket.getMsgOperation() == MSG_OPER.INSERT)
								  {
									  ContentValues contentValue = new ContentValues();         
									  contentValue.put("key", msgPacket.getMsgId());
									  contentValue.put("value", msgPacket.getMsgContent());
									  Uri newUri =  getContext().getContentResolver().insert(uri,contentValue);								
									  responseMsgPacket.setMsgContent(newUri.toString());								
								  }
								  else if(msgPacket.getMsgOperation() == MSG_OPER.QUERY)
								  {
									  Cursor resultCursor = getContext().getContentResolver().query(uri, null, 
											  				(msgPacket.getMsgId().compareTo("*")==0)?"@":msgPacket.getMsgId(), null, null);
									  responseMsgPacket.setMsgContent(MessagePacket.serializeCursor(resultCursor));
									  if(msgPacket.getMsgId().compareTo("*") == 0)
										  MessagePacket.sendMessage(node.getNextDeviceID(), msgPacket);									  
									  
								  }
								  else if(msgPacket.getMsgOperation() == MSG_OPER.DELETE)
								  {
									  int noRowsDeleted = getContext().getContentResolver().delete(uri, 
											  				(msgPacket.getMsgId().compareTo("*")==0)?"@":msgPacket.getMsgId(), null);								  
									  responseMsgPacket.setMsgContent(String.valueOf(noRowsDeleted));
									  if(msgPacket.getMsgId().compareTo("*") == 0)
									  {
										  target = node.getNextDeviceID();								
										  MessagePacket.sendMessage(target, msgPacket);
									  }

								  }
								  else if(msgPacket.getMsgOperation() == MSG_OPER.NODEJOIN)
								  {									
									  responseMsgPacket.setMsgContent(node.getDeviceID() + ":::" + node.getPrevDeviceID());
									  node.setPrevDeviceID(msgPacket.getMsgId());
									  node.setPrevNodeID(Node.genHash(msgPacket.getMsgId()));
								  }
								 
								  // Send Response Message to initiator
								  MessagePacket.sendMessage(target, responseMsgPacket);
								  
							  }
							  break;
						  }
						  case NEXT:
						  {
							  target = node.getNextDeviceID();								
							  MessagePacket.sendMessage(target, msgPacket);
							  break;
						  }						  
						  case PREVIOUS:
						  {
							  target = node.getPrevDeviceID();								
							  MessagePacket.sendMessage(target, msgPacket);
							  break;
						  }							  						  
					  }					  
				      
				  }
				  // only initiator gets Message Type Response, and in that case announce that process is complete
				  else if(msgPacket.getMsgType() == MSG_TYPE.RESPONSE)
				  {		
					  // Synchronized to make sure that only one thread access at one time
					  synchronized (lock) 
					  {		
						  switch(msgPacket.getMsgOperation())
						  {
						  	case INSERT:		
						  	{
						  		isProcessComplete = true;
						  		break;
						  	}
						  	case DELETE:
						  	{
						  		if(msgPacket.getMsgId().compareTo("*") == 0)
						  		{
						  			msgCount++;
						  			resultDeletedRows += Integer.parseInt(msgPacket.getMsgContent());
						  			if(msgCount == DeviceInfo.REMOTE_PORTS.length - 1)
						  			{
						  				msgCount = 0;
							  			isProcessComplete = true;
						  			}
						  		}
						  		else
						  		{
						  			resultDeletedRows = Integer.parseInt(msgPacket.getMsgContent());
						  			isProcessComplete = true;
						  		}							  			
						  		break;
						  	}
						  	case QUERY:
						  	{
						  		if(msgPacket.getMsgId().compareTo("*") == 0)
						  		{
						  			msgCount++;
						  			if(msgCount != 1)
						  				resultCursorString += MessagePacket.ROW_DELIMITER;
						  			resultCursorString += msgPacket.getMsgContent();
						  			if(msgCount == 2)//DeviceInfo.REMOTE_PORTS.length - 1) TODO : change it back
						  			{
						  				msgCount = 0;
							  			isProcessComplete = true;
						  			}
						  		}
						  		else
						  		{
						  			resultCursorString = msgPacket.getMsgContent();
						  			isProcessComplete = true;
						  		}
						  		break;
						  	}						
						  	case NODEJOIN:
						  	{
						  		String[] placeHolderNodes = msgPacket.getMsgContent().split(":::");
						  		node.setNextDeviceID(placeHolderNodes[0]);
						  		node.setNextNodeID(Node.genHash(placeHolderNodes[0]));
						  		node.setPrevDeviceID(placeHolderNodes[1]);
						  		node.setPrevNodeID(Node.genHash(placeHolderNodes[1]));
						  		
						  		target = placeHolderNodes[1];
						  		MessagePacket nodeUpdateMsg = new MessagePacket();
						  		nodeUpdateMsg.setMsgId(node.getDeviceID());						  		
						  		nodeUpdateMsg.setMsgInitiator(node.getDeviceID());						  		
						  		nodeUpdateMsg.setMsgType(MSG_TYPE.RESPONSE);
						  		nodeUpdateMsg.setMsgOperation(MSG_OPER.NODEUPDATE);
						  		MessagePacket.sendMessage(target, nodeUpdateMsg);
						  		break;
						  	}
						  	case NODEUPDATE:
						  	{
						  		node.setNextDeviceID(msgPacket.getMsgId());
						  		node.setNextNodeID(Node.genHash(msgPacket.getMsgId()));	
						  		break;
						  	}
						default:
							Log.e(TAG, "Invalid Message RESPONSE Operation :"+ msgPacket.getMsgOperation());
							break;
						  }
					  }
					
				  }
				  else
				  {
					  Log.e(TAG,"Invalid Message Type");
				  }
				  
				  //TextView textView = (TextView) findViewById(R.id.textView1);
				  //textView.append(msgPacket + "\t\n");          	          
				  
				  /*
				   * The following code creates a file in the AVD's internal storage and stores a file.
				   * 
				   * For more information on file I/O on Android, please take a look at
				   * http://developer.android.com/training/basics/data-storage/files.html
				   */
				  
				  String filename = "SimpleDhtProvider";
				  String string = MessagePacket.serializeMessage(msgPacket) + "\n";
				  FileOutputStream outputStream;

				  try {
				      outputStream =  getContext().openFileOutput(filename, Context.MODE_PRIVATE);
				      outputStream.write(string.getBytes());
				      outputStream.close();
				  } catch (Exception e) {
				      Log.e(TAG, "File write failed");
				  }
				  
				  
			} catch (Exception e) {
				Log.e(TAG, e.getMessage());
				e.printStackTrace();
			}

	          return;
	      }
	  }
	  
}

