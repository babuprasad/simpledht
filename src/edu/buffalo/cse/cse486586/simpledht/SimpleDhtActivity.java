package edu.buffalo.cse.cse486586.simpledht;


import java.io.IOException;
import java.net.ServerSocket;

import android.net.Uri;
import android.os.Bundle;
import android.app.Activity;
import android.content.Context;
import android.database.Cursor;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

public class SimpleDhtActivity extends Activity {

	SimpleDhtProvider simpleDhtProvider = null;
	public static final String TAG = SimpleDhtActivity.class.getSimpleName();
	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	
    	try {
		        super.onCreate(savedInstanceState);
		        setContentView(R.layout.activity_simple_dht_main);
		        
		        final TextView tv = (TextView) findViewById(R.id.textView1);
		        tv.setMovementMethod(new ScrollingMovementMethod());
		        findViewById(R.id.button3).setOnClickListener(
		                new OnTestClickListener(tv, getContentResolver()));
		        
		        final Uri uri = Uri.parse("content://edu.buffalo.cse.cse486586.simpledht.provider");
		        
		        findViewById(R.id.button1).setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						// TODO Auto-generated method stub
						try {
							Node node = Node.getInstance();
							Cursor cursor = getContentResolver().query(uri, null, "@", null, null);
							String result = "Node ID : " + node.getEmulatorPort();
							while(cursor.moveToNext())
							{
								result += "\n" + cursor.getString(0) + " : " + cursor.getString(1); 
							}
							tv.setText(result);
								
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
						
					}
				});
		        
		        /*
		         * Calculate the port number that this AVD listens on.
		         * It is just a hack that I came up with to get around the networking limitations of AVDs.
		         * The explanation is provided in the PA1 spec.
		         */
		        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
		        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
		        final String emulatorPort = String.valueOf((Integer.parseInt(portStr) * 2));
		        
		        findViewById(R.id.button4).setOnClickListener(new OnClickListener() {
					
					@Override
					public void onClick(View v) {
						Node node;
						try {
							node = Node.getInstance();
							tv.setText("\nNode ID : "+node.getEmulatorPort() +
									"\nNode Prev ID : "+node.getPrevNodePort() +
									"\nNode Next ID : "+node.getNextNodePort());
						} catch (Exception e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
						
					}
				});
		        
		        // Initialize Node Instance - Babu
				Node.initNodeInstance(emulatorPort);       
				Node node = Node.getInstance();				
				
				/*
		       	* Create a server socket as well as a thread (AsyncTask) that listens on the server
		       	* port. - Babu
		       	* */								  
				//ServerSocket serverSocket = new ServerSocket(DeviceInfo.SERVER_PORT);
				//simpleDhtProvider = new SimpleDhtProvider();	
				//AsyncTask<ServerSocket, String, Void> serverTask = simpleDhtProvider.getServerTask();
				//serverTask.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
				
				
				// Checking if we are the base node
				if(emulatorPort.compareTo(DeviceInfo.BASE_PORT) == 0) 	
				{
					
				}				
				else /* Form logical ring by sending a request to the Base node */
				{
					MessagePacket msgPacket = new MessagePacket();
					msgPacket.setMsgId(node.getEmulatorPort());				
					msgPacket.setMsgType(MSG_TYPE.REQUEST);
					msgPacket.setMsgOperation(MSG_OPER.NODEJOIN);
					msgPacket.setMsgInitiator(node.getEmulatorPort());
					MessagePacket.sendMessage(DeviceInfo.BASE_PORT, msgPacket);
				}
		   
		  } catch (IOException e) {		    
		      Log.e(TAG, "Can't create a ServerSocket");		      
		  } catch (Exception e) {
		      Log.e(TAG, "Exception in oncreate - "+e.getMessage());
			e.printStackTrace();
		}
		
			 
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_simple_dht_main, menu);
        return true;
    }
}
