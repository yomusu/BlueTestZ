package com.yom.btserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
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
    PingerClientThread	pingClientThread;
    ReadingClientKicker	clientThread;
    
    // これがnullだとDeviceがBluetoothに対応していない
    BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    
    ArrayAdapter<String>	adapter;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);
		
		// ListView用のアダプタを作成
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		
        ListView	listView = (ListView) findViewById(R.id.listView1);
        listView.setAdapter(adapter);
	}

	/**
	 * OptionMenuを作成する
	 */
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.server, menu);
		return true;
	}

    @Override
    protected void onDestroy () {
    	// Accept Threadの削除
    	if (thread != null) {
    		thread.cancel();
    	}
    	super.onDestroy();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent serverIntent = null;
        
        switch (item.getItemId()) {
    	//-------
        // AcceptThreadを開始する
        case R.id.run_accept_thread:
        	if( thread==null ) {
        		thread = new AcceptThread();
        		try {
        			thread.init();
        			thread.start();
        		} catch( IOException e ) {
        			e.printStackTrace();
        			thread = null;
        		}
        	} else {
        		// 既にThreadが存在する場合
        		sendLog("Accept thread has been already.");
        	}
        	return true;
    	//-------
        // AcceptThreadを停止する
        case R.id.stop_accept_thread:
        	if( thread!=null ) {
        		thread.cancel();
        		thread = null;
        	}
        	return true;
    	//-------
        // Clientとして接続する
        case R.id.ping_as_client:
        	if( pingClientThread==null ) {
        		// ペア済みのデバイスを取得する
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                Iterator<BluetoothDevice> it = pairedDevices.iterator();
                BluetoothDevice	device = it.next();
                
                pingClientThread = new PingerClientThread(device);
                pingClientThread.start();
        	}
        	return true;
    	//-------
        // Clientとして接続する
        case R.id.start_reading_as_client:
        	if( clientThread==null ) {
        		// ペア済みのデバイスを取得する
                Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
                Iterator<BluetoothDevice> it = pairedDevices.iterator();
                BluetoothDevice	device = it.next();
                
        		clientThread = new ReadingClientKicker(device);
        		clientThread.kick();
        	}
        	return true;
        // Clientとして接続する
        case R.id.stop_reading_as_client:
        	if( clientThread!=null ) {
        		clientThread.stop();
        		clientThread = null;
        	}
        	return true;

        }
        
        return false;
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
    	
        private BluetoothServerSocket serverSocket;
     
        public void init() throws IOException {
        	serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_ID);
        }
     
        public void run() {
        	
            sendLog("Accept Thread now has been ready.");
            
            while (serverSocket!=null) {
            	
                BluetoothSocket socket = null;
                
                // 接続待ち
                try {
                    socket = serverSocket.accept();
                    sendLog("connection was accepted.");
                } catch (IOException e) {
                	sendError( "Fail to accept.", e);
                    break;
                }
                
                // 通信Threadを起動する
                SocketThread	thread = new SocketThread(socket);
                thread.start();
                
                try {
                    Thread.sleep(1000);
                	thread.send("Happy Connections!");
                } catch( IOException e ) {
                	sendError( "send error.", e );
                } catch( InterruptedException e ) {}
            }
            sendLog("Accept Thread has gone away.");
        }
        
        /** Will cancel the listening socket, and cause the thread to finish */
        public void cancel() {
            try {
                serverSocket.close();
                serverSocket = null;
                sendLog( "The server socket is closed." );
            } catch (IOException e) { }
        }
    }
    
    private void sendString( String mes ) {
    	readHandler.obtainMessage( MESSAGE_READ, mes ).sendToTarget();
    }
    private void sendLog( String mes ) {
    	readHandler.obtainMessage( MESSAGE_READ, mes ).sendToTarget();
    }
    private void sendError( String mes, Exception e ) {
    	readHandler.obtainMessage( MESSAGE_READ, mes ).sendToTarget();
    }
    
    
    /***
     * BluetoothのSocketを管理するスレッド
     * ServerでもClientでも動く
     * 読み込んだ文字列はHandleに送信する
     * @author matsumoto
     *
     */
    private class SocketThread extends Thread {
    	
    	private BluetoothSocket socket;
	    private OutputStream	out;
		private InputStream		in;
		boolean	isLazyConnect = false;
		
    	SocketThread( BluetoothSocket socket ) {
    		this.socket = socket;
    	}
    	SocketThread( BluetoothSocket socket, boolean lazyConnect ) {
    		this.socket = socket;
    		this.isLazyConnect = lazyConnect;
    	}
    	
    	/** connectionが完了するのを待つ。処理をブロックします */
    	synchronized public void waitConnection( long milli ) {
    		if( socket.isConnected() == false ) {
    			try {
    				wait( milli );
    			} catch( InterruptedException e ) {}
    		}
    	}
    	
		@Override
		public void run() {
	    	sendLog("SocketThread just has started.");
    		
	    	try {
	    		// 必要であればSocketを接続
	    		if( isLazyConnect ) {
	    			try {
	    				synchronized (this) {
		    				socket.connect();
		    				BluetoothDevice	dv = socket.getRemoteDevice();
		    				sendLog("socket just has connected to "+dv.getName()+"("+dv.getAddress()+")");
		    				notifyAll();	// for waitConnection
						}
	    			} catch( IOException e ) {
		    			sendError("could not connect socket",e);
		    			return;
	    			}
	    		}
	    		// Stream取得
	    		try {
	    			in = socket.getInputStream();
	    			out = socket.getOutputStream();
	    		} catch( IOException e ) {
	    			sendError("could not open stream",e);
	    			return;
	    		}

	    		// 読み取り
	    		byte[] buffer = new byte[1024];
	    		try {
	    			while (socket!=null) {
	    				int len = in.read(buffer);
	    				if( len>0 ) {
	    					StringBuilder	buf = new StringBuilder();
	    					buf.append( Integer.toString( (int)buffer[0], 16 ) );
	    					for( int i=1; i<len; i++ ) {
	    						buf.append(",");
	    						buf.append( Integer.toString( (int)buffer[i], 16 ) );
	    					}
	    					sendString( buf.toString() );
	    				}
	    				if( len<0 )
	    					break;
	    			}
	    		} catch (IOException e) {
	    			if( socket!=null )
	    				sendError( "connection is closed", e );
	    		}
	    	} finally {
    			cancel();
	    		sendLog("SocketThread has gone away.");
	    	}
		}
		
		synchronized public void cancel() {
			try {
				if( in!=null )	in.close();
				if( out!=null )	out.close();
				if( socket!=null )	socket.close();
			} catch( IOException e ) {}
			socket = null;
			in = null;
			out = null;
		}
    	
		synchronized public void send( String mes ) throws IOException {
			if( out==null )
				throw new IOException("already closed");
			byte[]	data = mes.getBytes();
			out.write( data );
    	}
		synchronized public void send( int data ) throws IOException {
			if( out==null )
				throw new IOException("already closed");
			out.write( data );
    	}
    }
    
    /*********
     * Clientとして文字を読み込むスレッド
     * @author matsumoto
     *
     */
	private class ReadingClientKicker {
		
		SocketThread	thread;
	    BluetoothDevice device;
	    
	    ReadingClientKicker( BluetoothDevice device ) {
			this.device = device;
		}
		
	    public void kick() {
	    	Thread	th = new Thread( new Runnable() {
	    		@Override
	    		public void run() {
	    		    BluetoothSocket	socket = null;
	    	    	// Socketを作成
	    	    	try {
	    	    		socket = device.createRfcommSocketToServiceRecord(SERVICE_ID);
	    	    	} catch( IOException e ) {
	    	    		sendError("could not create socket."+device.getName(),e);
	    	    		return;
	    	    	}

	    	    	// SocketThreadを開く（以降のSocketハンドルはThreadが行う）
	    	    	thread = new SocketThread(socket, true);
	    	    	thread.start();
	    		}
			} );
	    	th.start();
	    }
	    
		public void stop() {
			if( thread!=null ) {
				thread.cancel();
				thread = null;
			}
		}
	}

    /*********
     * Clientとして文字を出力するスレッド
     * @author matsumoto
     *
     */
	private class PingerClientThread extends Thread {
		
	    BluetoothDevice device;
	    
		PingerClientThread( BluetoothDevice device ) {
			this.device = device;
		}
		
		@Override
		public void run() {
			sendLog("ClientThread just has started. Target device is "+device.getName());
			
		    try {
			    BluetoothSocket	socket = null;
		    	// Socketを作成
		    	try {
		    		socket = device.createRfcommSocketToServiceRecord(SERVICE_ID);
		    	} catch( IOException e ) {
		    		sendError("could not create socket."+device.getName(),e);
		    		return;
		    	}

		    	// SocketThreadを開く（以降のSocketハンドルはThreadが行う）
		    	SocketThread	thread = new SocketThread(socket, true);
		    	thread.start();

		    	thread.waitConnection(10*1000);
		    	
		    	// SocketThreadに出力を指示
		    	try {
		    		String	message = "Hello!";
	    			thread.send( message.charAt(0) );
		    		for( int i=1; i<message.length(); i++ ) {
		    			try {
		    				Thread.sleep(1000);
		    			} catch( InterruptedException e ){}
		    			thread.send( message.charAt(i) );
		    		}
	    			thread.send( "world!" );
	    			
		    	} catch( IOException e ) {
		    		sendError("send error",e);
		    	} finally {
		    		thread.cancel();
		    	}
		    } finally {
		    	sendLog("ClientThread has gone away.");
		    }
		}
	}
	
}
