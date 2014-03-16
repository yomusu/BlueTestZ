package com.yom.btserver;

import java.io.IOException;

import android.app.ActionBar;
import android.app.ActionBar.Tab;
import android.app.ActionBar.TabListener;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.FragmentTransaction;
import android.bluetooth.BluetoothDevice;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.widget.ArrayAdapter;
import android.widget.FrameLayout;
import android.widget.Spinner;

import com.yom.btserver.DeviceChoiceDialog.OnBluetoothDeviceSelectedListener;
import com.yom.btserver.LoginDialog.OnLoginDialogListener;


/***
 * 
 * BluetoothのSPPサーバー側プログラム
 * 
 * @author matsumoto
 *
 */
public class ServerActivity extends Activity implements OnBluetoothDeviceSelectedListener,TabListener,OnLoginDialogListener {

    BTSPPService	btservice;
    
    
    static final int	MP = LayoutParams.MATCH_PARENT;
    static final int	WC = LayoutParams.WRAP_CONTENT;
    
    EditorFragment	editor;
    LogFragment		logfragment;
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		FrameLayout	frame = new FrameLayout(this);
		frame.setLayoutParams( new LayoutParams(MP, MP) );
		frame.setId( 10 );
		
		setContentView(frame);
//		setContentView(R.layout.activity_server);
		
		// フラグメントの作成
		editor = EditorFragment.newInstance("");
		logfragment = new LogFragment();
		
		FragmentTransaction	tx = getFragmentManager().beginTransaction();
		tx.add( 10, editor);
		tx.add( 10, logfragment);
		tx.show(editor);
		tx.hide(logfragment);
		tx.commit();
		
		// タブのセットアップ　
		ActionBar	actionBar = getActionBar();
		actionBar.setNavigationMode( ActionBar.NAVIGATION_MODE_TABS );
		
		actionBar.addTab(
				actionBar.newTab()
				.setText("入力")
				.setTabListener( this )
				);
		
		actionBar.addTab(
				actionBar.newTab()
				.setText("ログ")
				.setTabListener( this )
				);
		
        // Bluetoothサービスの作成
        btservice = new BTSPPService();
        btservice.readHandler = readHandler;
        
        // いきなりダイアログを出すことは可能か
        LoginDialog	dlg = new LoginDialog();
        dlg.show(getFragmentManager(), "login");
	}
	
	//-----------------------------------
	// TabListenerの実装

	@Override
	public void onTabReselected(Tab tab, FragmentTransaction ft) {
	}

	@Override
	public void onTabSelected(Tab tab, FragmentTransaction ft) {
		int	pos = tab.getPosition();
		switch( pos ) {
		case 0:
			ft.show(editor);
			ft.hide(logfragment);
			break;
		case 1:
			ft.show(logfragment);
			ft.hide(editor);
			break;
		}
	}

	@Override
	public void onTabUnselected(Tab tab, FragmentTransaction ft) {
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
	public void onClickLoginButton( DialogFragment dialog ) {
		
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
//        	EditorFragment	f = (EditorFragment)getFragmentManager().findFragmentById(R.id.editor_fragment);
//        	f.setCodeData("0120444444");
        	editor.setCodeData("");
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
                logfragment.adapter.add(readMessage);
                break;
            case BTSPPService.MESSAGE_DATA:
            {
            	// 入力フォームにセット
            	editor.setCodeData( (String) msg.obj );
                break;
            }
            case BTSPPService.MESSAGE_CONNECTED:
            {
            	// デバイスに接続した旨を表示
            	logfragment.adapter.add( (String) msg.obj );
            	break;
            }	
            case BTSPPService.MESSAGE_DISCONNECTED:
            {
            	// デバイスから切断した旨を表示
            	logfragment.adapter.add("未接続");
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

/****
 * 
 * ログインダイアログ
 * 
 * @author matsumoto
 *
 */
class LoginDialog extends DialogFragment {
	
	static public interface OnLoginDialogListener {
		public void onClickLoginButton( DialogFragment dialog );
	}

	OnLoginDialogListener	listener;
	
	@Override  
    public void onAttach(Activity activity) {  
        super.onAttach(activity);  
        try {  
        	listener = (OnLoginDialogListener) activity;  
        } catch (ClassCastException e) {  
            throw new ClassCastException(activity.toString() + " must implement OnBluetoothDeviceSelectedListener");  
        }  
    }
	   
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
    	
    	Dialog	dialog = new Dialog(getActivity());
    	
    	dialog.setTitle("店舗ログイン");
    	dialog.setContentView(R.layout.login_dialog);
    	
    	ArrayAdapter<String>	shops = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_item);
    	shops.add("アキバ本店");
    	shops.add("鳩ヶ谷店");
    	shops.add("越谷店");
    	
    	Spinner	spin = (Spinner)dialog.findViewById(R.id.shop_spinner);
    	spin.setAdapter(shops);
    	
    	dialog.findViewById(R.id.login).setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				listener.onClickLoginButton(LoginDialog.this);
				dismiss();
			}
		});
    	dialog.findViewById(R.id.login_cancel).setOnClickListener( new OnClickListener() {
			@Override
			public void onClick(View v) {
				dismiss();
			}
		});
    	
        return dialog;
    }
}
