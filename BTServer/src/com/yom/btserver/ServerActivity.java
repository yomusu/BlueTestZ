package com.yom.btserver;

import java.io.IOException;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.yom.btserver.DeviceChoiceDialog.OnBluetoothDeviceSelectedListener;


/***
 * 
 * BluetoothのSPPサーバー側プログラム
 * 
 * @author matsumoto
 *
 */
public class ServerActivity extends Activity implements OnBluetoothDeviceSelectedListener {

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
        
        getFragmentManager().findFragmentById(R.id.editor_fragment);
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
        // ClientとしてPingする
        case R.id.ping_as_client:
        {
        	DeviceChoiceDialog	dlg = new DeviceChoiceDialog();
        	dlg.devices = btservice.getBondedDevices();
        	dlg.show( getFragmentManager(), "ping_to_device" );
        	return true;
        }
    	//-------
        // Clientとして接続する
        case R.id.start_reading_as_client:
        {
        	DeviceChoiceDialog	dlg = new DeviceChoiceDialog();
        	dlg.devices = btservice.getBondedDevices();
        	dlg.show( getFragmentManager(), "connect_to_device" );
        	return true;
        }
        // Clientとして接続する
        case R.id.stop_reading_as_client:
        	btservice.disconnect();
        	// for test
        	EditorFragment	f = (EditorFragment)getFragmentManager().findFragmentById(R.id.editor_fragment);
        	f.setCodeData("0120444444");
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
                String readMessage = (String) msg.obj;
                Log.d("Handler",readMessage);
                adapter.add(readMessage);
                break;
            case BTSPPService.MESSAGE_DATA:
            {
            	// 入力フォームにセット
            	EditorFragment	editor = (EditorFragment)getFragmentManager().findFragmentById(R.id.editor_fragment);
            	editor.setCodeData( (String) msg.obj );
                break;
            }
            case BTSPPService.MESSAGE_CONNECTED:
            {
            	// デバイスに接続した旨を表示
            	InformationFragment	editor = (InformationFragment)getFragmentManager().findFragmentById(R.id.info_fragment);
            	editor.setConnectionStatus( (String) msg.obj );
            	break;
            }	
            case BTSPPService.MESSAGE_DISCONNECTED:
            {
            	// デバイスから切断した旨を表示
            	InformationFragment	editor = (InformationFragment)getFragmentManager().findFragmentById(R.id.info_fragment);
            	editor.setConnectionStatus( "未接続" );
            	break;
            }	
            }
        }
    };

	@Override
	public void onBluetoothDeviceSelected( DialogFragment dialog, BluetoothDevice device ) {
		if( dialog.getTag().equals("connect_to_device") ) {
        	btservice.connectDevice( device );
		}
		if( dialog.getTag().equals("ping_to_device") ) {
        	btservice.pingToDevice( device );
		}
	}
}

/****
 * 
 * Bluetoothのデバイスを選択するダイアログ
 * 
 * @author matsumoto
 *
 */
class DeviceChoiceDialog extends DialogFragment {
	
	static public interface OnBluetoothDeviceSelectedListener {
		public void onBluetoothDeviceSelected( DialogFragment dialog, BluetoothDevice device );
	}

//	public String tag;
	public BluetoothDevice[] devices;
	OnBluetoothDeviceSelectedListener	listener;
	
	@Override  
    public void onAttach(Activity activity) {  
        super.onAttach(activity);  
        try {  
        	listener = (OnBluetoothDeviceSelectedListener) activity;  
        } catch (ClassCastException e) {  
            throw new ClassCastException(activity.toString() + " must implement OnBluetoothDeviceSelectedListener");  
        }  
    }
	   
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	
        CharSequence[] items = new CharSequence[devices.length];
        for( int i=0; i<items.length; i++ )
        	items[i] = devices[i].getName();

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("接続デバイスの選択");
        builder.setCancelable(true);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            	if( listener!=null )
            		listener.onBluetoothDeviceSelected( DeviceChoiceDialog.this, devices[which] );
            }
        });
        return builder.create();
    }
}
