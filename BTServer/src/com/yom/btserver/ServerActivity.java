package com.yom.btserver;

import java.io.IOException;

import android.app.Activity;
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

    BTSPPService	btservice;
    
    ArrayAdapter<String>	adapter;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_server);
		
		// ListView用のアダプタを作成
		adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1);
		
        ListView	listView = (ListView) findViewById(R.id.listView1);
        listView.setAdapter(adapter);
        
        // Bluetoothサービスの作成
        btservice = new BTSPPService();
        btservice.readHandler = readHandler;
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
		btservice.dispose();
    	super.onDestroy();
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        
        switch (item.getItemId()) {
    	//-------
        // AcceptThreadを開始する
        case R.id.run_accept_thread:
    		try {
            	btservice.accept();
    		} catch( IOException e ) {
            	//	sendLog("Accept thread has been already.");
    		}
        	return true;
    	//-------
        // AcceptThreadを停止する
        case R.id.stop_accept_thread:
        	btservice.cancelAccept();
        	return true;
    	//-------
        // Clientとして接続する
        case R.id.ping_as_client:
        	btservice.pingToDevice();
        	return true;
    	//-------
        // Clientとして接続する
        case R.id.start_reading_as_client:
        	btservice.connectDevice();
        	return true;
        // Clientとして接続する
        case R.id.stop_reading_as_client:
        	btservice.disconnect();
        	return true;
        }
        
        return false;
    }
    
    private final Handler readHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case BTSPPService.MESSAGE_ERROR:
            case BTSPPService.MESSAGE_LOG:
            case BTSPPService.MESSAGE_DATA:
                String readMessage = (String) msg.obj;
                Log.d("Handler",readMessage);
                adapter.add(readMessage);
                break;
            }
        }
    };
    
}
