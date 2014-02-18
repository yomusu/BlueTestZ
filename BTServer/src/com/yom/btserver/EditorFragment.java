package com.yom.btserver;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

public class EditorFragment extends Fragment {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View v = inflater.inflate(R.layout.editor, container, false);
		return v;
	}
	
	/**
	 * デバイスから受信したコードをセットする
	 * @param code
	 */
	public void setCodeData( String code ) {
		
		// 会員コードであれば入力フォームをクリアし、
		EditText	memberCode = (EditText)getView().findViewById(R.id.member_code);
		memberCode.setText( code );
		
		// 会員コードに値をセットする
		
		// イベントコードであれば入力フォームはクリアせずに
		
		// 送信するとクリアするのかな？
		// であれば、コードセットでクリアは必要ないかも？
		
		// 会員コード、イベントコードの順番を守ってもらう仕様にするか
	}

}
