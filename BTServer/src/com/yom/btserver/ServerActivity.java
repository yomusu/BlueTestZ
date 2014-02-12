package com.yom.btserver;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.widget.ArrayAdapter;
import android.widget.ListView;


/***
 * 
 * BluetoothのSPPサーバー側プログラム
 * 
 * @author matsumoto
 *
 */
public class ServerActivity extends Activity {

    private static final String TAG = "BTHello";
    private static final String SERVICE_NAME = "BTHello";
    private static final String SERIAL_PORT_SERVICE_ID = "00001101-0000-1000-8000-00805F9B34FB";
    private static final UUID SERVICE_ID = UUID.fromString(SERIAL_PORT_SERVICE_ID);
    
    
    AcceptThread thread;

    // これがnullだとDeviceがBluetoothに対応していない
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    
    ArrayAdapter<String>	adapter;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);
		
		thread = new AcceptThread( readHandler );
		try {
			thread.init();
			thread.start();
		} catch( IOException e ) {
			e.printStackTrace();
			thread = null;
		}
		
		// ListView用のアダプタを作成
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		
        ListView	listView = (ListView) findViewById(R.id.listView1);
        listView.setAdapter(adapter);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.server, menu);
		return true;
	}

    @Override
    protected void onDestroy () {
    	if (thread != null) {
    		thread.cancel();
    	}
    	super.onDestroy();
    }
    
    static final private int MESSAGE_READ = 0;

    private final Handler readHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_READ:
                // construct a string from the valid bytes in the buffer
                String readMessage = (String) msg.obj;
                Log.d("Handler",readMessage);
                adapter.add(readMessage);
                break;
            }
        }
    };
    
    /**
     * Bluetoothのサーバースレッド
     * @author matsumoto
     *
     */
    private class AcceptThread extends Thread {
    	
    	private Handler	handler;
        private BluetoothServerSocket serverSocket;
     
        AcceptThread( Handler handler ) {
        	this.handler = handler; 
        }
        
        public void init() throws IOException {
        	serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_ID);
        }
     
        public void run() {
            while (serverSocket!=null) {
            	
                BluetoothSocket socket = null;
                
                // 接続待ち
                try {
                    socket = serverSocket.accept();
                } catch (IOException e) {
					Log.e(TAG, "Fail to accept.", e);
                    break;
                }
                
                // 読み取り
            	Log.d(TAG, "connection was accepted.");
            	try {
            		InputStream in = socket.getInputStream();
            		
        	    	Log.d(TAG, "Connection established.");
                	byte[] buffer = new byte[1024];
                	
        	        while (serverSocket!=null) {
                        int bytes = in.read(buffer);
                        String	m = new String(buffer, 0, bytes);
                        handler.obtainMessage( MESSAGE_READ, m ).sendToTarget();
        	        }
        	        
        		} catch (IOException e) {
        			Log.e(TAG, "connection is closed");
        		} finally {
        			try {
        				socket.close();
                    	Log.d(TAG, "The session was closed. Listen again.");
        			} catch( Exception e ) {}
        		}
            }
            Log.d(TAG,"Accept Thread has gone.");
        }
        
        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                serverSocket.close();
                serverSocket = null;
    	    	Log.d(TAG, "The server socket is closed.");
            } catch (IOException e) { }
        }
    }
}
