package com.yom.btserver;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.UUID;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;

public class BTSPPService {
	
    private static final String SERIAL_PORT_SERVICE_ID = "00001101-0000-1000-8000-00805F9B34FB";
    private static final UUID SERVICE_ID = UUID.fromString(SERIAL_PORT_SERVICE_ID);
    
    /** データ */
    static final public int MESSAGE_DATA = 0;
    /** ログ */
    static final public int MESSAGE_LOG  = 1;
    /** エラー */
    static final public int MESSAGE_ERROR = 2;

    /** サービス名(acceptする時のみ必要) */
    public String serviceName = "BTSPPService";
    
    public Handler readHandler;

    
    private BluetoothServerSocket	serverSocket;
    private SocketThread	clientThread;
    
    // これがnullだとDeviceがBluetoothに対応していない
    public BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
	
    
    /***
     * サーバーを起動する
     * @throws IOException
     */
    public void accept() throws IOException {
    	if( serverSocket==null ) {
    		
    		// ソケットの作成
           	serverSocket = mBluetoothAdapter.listenUsingRfcommWithServiceRecord(serviceName, SERVICE_ID);
           	
    		// サーバースレッド
    		Thread	th = new Thread( new Runnable() {
    	        public void run() {
    	            sendLog("Accept Thread is here.");
    	            
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
    	                SocketThread	thread = new SocketThread();
    	                thread.start( socket );
    	                
    	                // 返事を送信する
    	                try {
    	                    Thread.sleep(1000);
    	                	thread.send("Happy Connections!");
    	                } catch( IOException e ) {
    	                	sendError( "send error.", e );
    	                } catch( InterruptedException e ) {}
    	            }
    	            
    	            sendLog("Accept Thread has gone away.");
    	        }
    		} );
    		th.start();
    	}
    }
    
    /***
     * サーバーを停止する
     */
    public void cancelAccept() {
    	if( serverSocket!=null ) {
            try {
                serverSocket.close();
                serverSocket = null;
            } catch (IOException e) { }
    	}
    }
    
    /***
     * ペア済みのデバイスを取得する
     * @return
     */
    public BluetoothDevice[] getBondedDevices() {
        Set<BluetoothDevice> pairedDevices = mBluetoothAdapter.getBondedDevices();
        return pairedDevices.toArray( new BluetoothDevice[pairedDevices.size()]);
    }
    
    /**
     * デバイスに接続する
     */
    public void connectDevice( final BluetoothDevice device ) {
    	if( clientThread==null ) {
    		clientThread = new SocketThread();
    		
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
	    	    	clientThread.startWithConnection(socket);
	    		}
			} );
	    	th.start();
    	}
    }
    
    /**
     * デバイスとの接続を切断する
     */
    public void disconnect() {
    	if( clientThread!=null ) {
    		clientThread.cancel();
    		clientThread = null;
    	}

    }
    
    /**
     * 全体の終了処理を行う
     */
    public void dispose() {
    	disconnect();
    	cancelAccept();
    }
    
	/***
	 * デバイスにClientとして接続してデータを送りつけてみる
	 */
    public void pingToDevice( final BluetoothDevice device ) {
    	
    	Thread	th = new Thread( new Runnable() {
    		@Override
    		public void run() {
    			sendLog("Ping just has started. Target device is "+device.getName());
    			
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
    		    	SocketThread	thread = new SocketThread();
    		    	thread.startWithConnection(socket);

    		    	sendLog("now connecting...");
    		    	thread.waitConnection(20*1000);
    		    	
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
    		    	sendLog("Ping has gone away.");
    		    }
    		}
    	} );
    	th.start();
    }
    
    
    private void sendString( String mes ) {
    	readHandler.obtainMessage( MESSAGE_DATA, mes ).sendToTarget();
    }
    private void sendLog( String mes ) {
    	readHandler.obtainMessage( MESSAGE_LOG, mes ).sendToTarget();
    }
    private void sendError( String mes, Exception e ) {
    	readHandler.obtainMessage( MESSAGE_ERROR, mes ).sendToTarget();
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
		boolean	isBinaryMode = false;
		
    	public void start( BluetoothSocket socket ) {
    		this.socket = socket;
    		start();
    	}
    	
    	public void startWithConnection( BluetoothSocket socket ) {
    		this.socket = socket;
    		this.isLazyConnect = true;
    		start();
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
	    					if( isBinaryMode ) {
	    						StringBuilder	buf = new StringBuilder();
	    						buf.append( Integer.toString( (int)buffer[0], 16 ) );
	    						for( int i=1; i<len; i++ ) {
	    							buf.append(",");
	    							buf.append( Integer.toString( (int)buffer[i], 16 ) );
	    						}
	    						sendString( buf.toString() );
	    					} else {
	    						sendString( new String(buffer,0,len) );
	    					}
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
    
}
