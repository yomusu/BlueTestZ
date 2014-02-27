package com.yom.btserver;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;

import android.app.Fragment;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

public class EditorFragment extends Fragment implements View.OnClickListener, TextWatcher {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.editor, container, false);
		
		((Button)v.findViewById(R.id.btn_bunrui_1)).setOnClickListener( this );
		((Button)v.findViewById(R.id.btn_bunrui_2)).setOnClickListener( this );
		((Button)v.findViewById(R.id.btn_bunrui_3)).setOnClickListener( this );
		((Button)v.findViewById(R.id.btn_bunrui_4)).setOnClickListener( this );
		((Button)v.findViewById(R.id.btn_bunrui_other)).setOnClickListener( this );
		
		((Button)v.findViewById(R.id.btn_card)).setOnClickListener( this );
		((Button)v.findViewById(R.id.btn_genkin)).setOnClickListener( this );
		
		((Button)v.findViewById(R.id.btn_man)).setOnClickListener( this );
		((Button)v.findViewById(R.id.btn_woman)).setOnClickListener( this );
		
		((EditText)v.findViewById(R.id.edit_membercode)).addTextChangedListener( this );
		((EditText)v.findViewById(R.id.edit_amount)).addTextChangedListener( this );
		((EditText)v.findViewById(R.id.edit_number)).addTextChangedListener( this );

		// 送信ボタン
		((Button)v.findViewById(R.id.btn_send)).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				sendToServer();
			}
		} );
		
		// クリアボタン
		((Button)v.findViewById(R.id.btn_clear)).setOnClickListener( new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				clearForm();
			}
		} );
		
		return v;
	}
	
	@Override
	public void onClick(View v) {
		switch( v.getId() ) {
		case R.id.btn_bunrui_1:
			setDM("1");
			refreshSenable();
			break;
		case R.id.btn_bunrui_2:
			setDM("2");
			refreshSenable();
			break;
		case R.id.btn_bunrui_3:
			setDM("3");
			refreshSenable();
			break;
		case R.id.btn_bunrui_4:
			setDM("4");
			refreshSenable();
			break;
		case R.id.btn_bunrui_other:
			refreshSenable();
			break;
		case R.id.btn_man:
			setSex("m");
			refreshSenable();
			break;
		case R.id.btn_woman:
			setSex("f");
			refreshSenable();
			break;
		case R.id.btn_genkin:
			setPayment("g");
			refreshSenable();
			break;
		case R.id.btn_card:
			setPayment("c");
			refreshSenable();
			break;
		}
	}
	
	@Override
	public void onTextChanged(CharSequence s, int start, int before, int count) {
	}

	@Override
	public void beforeTextChanged(CharSequence s, int start, int count,	int after) {
	}

	@Override
	public void afterTextChanged(Editable s) {
		refreshSenable();
	}
	
	private void clearForm() {
		
		((EditText)getView().findViewById(R.id.edit_membercode)).setText("");
		((EditText)getView().findViewById(R.id.edit_amount)).setText("");
		((EditText)getView().findViewById(R.id.edit_number)).setText("");
		
		setDM(null);
		setSex(null);
		setPayment(null);
		
		refreshSenable();
	}
	
	private void refreshSenable() {
		MemberAct	act = getAsMemberAct();
		if( act.canSend() ) {
			((Button)getView().findViewById(R.id.btn_send)).setEnabled(true);
		} else {
			((Button)getView().findViewById(R.id.btn_send)).setEnabled(false);
		}
	}
	
	private MemberAct getAsMemberAct() {
		
		MemberAct	a = new MemberAct();
		a.amount = ((EditText)getView().findViewById(R.id.edit_amount)).getText().toString();
		a.number = ((EditText)getView().findViewById(R.id.edit_number)).getText().toString();
		a.memberCode = ((EditText)getView().findViewById(R.id.edit_membercode)).getText().toString();
		
		a.dm = selectedDm;
		a.sex = selectedSex;
		a.payment = selectedPayment;
		
		return a;
	}
	
	String	selectedDm;
	String	selectedPayment;
	String	selectedSex;
	
	private void setDM( String dm ) {
		
		selectedDm = dm;
		
		int	select = Color.RED;
		int	unselect = Color.GRAY;
		
		if( dm!=null ) {
			((Button)getView().findViewById(R.id.btn_bunrui_1)).setBackgroundColor( dm.equals("1") ? select : unselect );
			((Button)getView().findViewById(R.id.btn_bunrui_2)).setBackgroundColor( dm.equals("2") ? select : unselect );
			((Button)getView().findViewById(R.id.btn_bunrui_3)).setBackgroundColor( dm.equals("3") ? select : unselect );
			((Button)getView().findViewById(R.id.btn_bunrui_4)).setBackgroundColor( dm.equals("4") ? select : unselect );
		} else {
			((Button)getView().findViewById(R.id.btn_bunrui_1)).setBackgroundColor( unselect );
			((Button)getView().findViewById(R.id.btn_bunrui_2)).setBackgroundColor( unselect );
			((Button)getView().findViewById(R.id.btn_bunrui_3)).setBackgroundColor( unselect );
			((Button)getView().findViewById(R.id.btn_bunrui_4)).setBackgroundColor( unselect );
		}
	}
	
	private void setSex( String sex ) {
		
		selectedSex = sex;
		
		int	select = Color.RED;
		int	unselect = Color.GRAY;
		
		if( sex!=null ) {
			((Button)getView().findViewById(R.id.btn_man)).setBackgroundColor( sex.equals("m") ? select : unselect );
			((Button)getView().findViewById(R.id.btn_woman)).setBackgroundColor( sex.equals("f") ? select : unselect );
		} else {
			((Button)getView().findViewById(R.id.btn_man)).setBackgroundColor( unselect );
			((Button)getView().findViewById(R.id.btn_woman)).setBackgroundColor( unselect );
		}
		
	}
	
	private void setPayment( String payment ) {
		
		selectedPayment = payment;
		
		int	select = Color.RED;
		int	unselect = Color.GRAY;
		
		if( payment!=null ) {
			((Button)getView().findViewById(R.id.btn_genkin)).setBackgroundColor( payment.equals("g") ? select : unselect );
			((Button)getView().findViewById(R.id.btn_card)).setBackgroundColor( payment.equals("c") ? select : unselect );
		} else {
			((Button)getView().findViewById(R.id.btn_genkin)).setBackgroundColor( unselect );
			((Button)getView().findViewById(R.id.btn_card)).setBackgroundColor( unselect );
		}
	}
	
	/**
	 * デバイスから受信したコードをセットする
	 * @param code
	 */
	public void setCodeData( String code ) {
		
		// 会員コードに値をセットする
		EditText	memberCode = (EditText)getView().findViewById(R.id.edit_membercode);
		memberCode.setText( code );
		
		
		// イベントコードであれば入力フォームはクリアせずに
		
		// 送信するとクリアするのかな？
		// であれば、コードセットでクリアは必要ないかも？
		
		// 会員コード、イベントコードの順番を守ってもらう仕様にするか
	}

	/**
	 * サーバーにデータを送信する
	 */
	private void sendToServer() {
		
		MemberAct	act = getAsMemberAct();
		
		// パラメーターの作成
		final List<NameValuePair> params = new ArrayList<NameValuePair>();
		params.add(new BasicNameValuePair("memberCode", act.memberCode ));
		params.add(new BasicNameValuePair("dm", act.dm ));
		params.add(new BasicNameValuePair("sex", act.sex ));
		params.add(new BasicNameValuePair("payment", act.payment ));
		params.add(new BasicNameValuePair("amount", act.amount ));
		params.add(new BasicNameValuePair("number", act.number ));
		
		// URLの作成
		final Uri.Builder	uri = new Uri.Builder();
		uri.scheme("http");
		uri.encodedAuthority("172.16.69.76:8080");
		uri.path("/post");

		Log.i("rr",uri.build().toString());
		
		Thread	th = new Thread( new Runnable() {
			@Override
			public void run() {
				HttpURLConnection	conn = null;
				try {
					// 送信した後フォームをクリアするよ
					HttpPost	req = new HttpPost(uri.build().toString());
					req.setEntity(new UrlEncodedFormEntity(params));

					HttpClient	client = new DefaultHttpClient();
					String	result = client.execute(req, new ResponseHandler<String>() {
						@Override
						public String handleResponse(HttpResponse response) throws ClientProtocolException, IOException {
							switch (response.getStatusLine().getStatusCode()) {
							case HttpStatus.SC_OK:
								return EntityUtils.toString(response.getEntity(), "UTF-8");
							case HttpStatus.SC_NOT_FOUND:
								throw new RuntimeException("404 dayo");
							default:
								throw new RuntimeException("Unknown Error dayo");
							}
						}
					});
					
					handler.obtainMessage(POST_SUCCESS, result).sendToTarget();

				} catch( Exception e ) {
					handler.obtainMessage(POST_ERROR, e).sendToTarget();
				} finally {
					if( conn!=null )
						conn.disconnect();
				}
			}
		});
		th.start();
		
	}
	
	
	static final int	POST_SUCCESS = 0;
	static final int	POST_ERROR = 1;
	
	Handler	handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
        	switch(msg.what) {
        	case POST_SUCCESS:
        		String	mes = (String)msg.obj;
        		Toast.makeText(getActivity(), "POST完了"+mes, 3*1000 ).show();
        		break;
        	case POST_ERROR:
        		Exception	e = (Exception)msg.obj;
        		e.printStackTrace();
        		Toast.makeText(getActivity(), "POSTエラー"+e.getMessage(), 5*1000 ).show();
        		break;
        	}
        }
	};
	
}

class MemberAct {
	
	String	memberCode;
	/** 利用金額 */
	String	amount;
	/** 人数 */
	String	number;
	/** 性別 */
	String	sex;
	/** 支払い方法 */
	String	payment;
	/** DM分類 */
	String	dm;
	
	public boolean canSend() {
		int	check = 0;
		
		if( memberCode!=null && memberCode.length()>0 )
			check++;
		if( amount!=null && amount.length()>0 )
			check++;
		if( number!=null && number.length()>0 )
			check++;
		if( sex!=null && sex.length()>0 )
			check++;
		if( payment!=null && payment.length()>0 )
			check++;
		if( dm!=null && dm.length()>0 )
			check++;
		
		return check==6;
	}
}
