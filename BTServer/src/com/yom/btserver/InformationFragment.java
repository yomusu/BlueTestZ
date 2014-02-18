package com.yom.btserver;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

public class InformationFragment extends Fragment {
	
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		
		Context	con = getActivity();
		
		TableLayout	layout = new TableLayout( con );
		layout.setStretchAllColumns(true);
		
		layout.addView( createTableRow(con, "店舗", "未入力", "tenpo") );
		layout.addView( createTableRow(con, "リーダー状態", "未接続", "device") );
		
		return layout;
	}
	
	private TableRow createTableRow( Context con, String title, String value, String tag ) {
		
		TextView	t = new TextView( con );
		t.setText(title);
		
		TextView	v = new TextView( con );
		v.setText(value);
		v.setTag( tag );
		
		TableRow	row = new TableRow( con );
		row.addView( t );
		row.addView( v );
		
		return row;
	}
	
	/**
	 * 接続状態の表示を更新する
	 * @param st
	 */
	public void setConnectionStatus( String st ) {
		TextView	text = (TextView)getView().findViewWithTag("device");
		text.setText( st );
	}
}
