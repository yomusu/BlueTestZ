package com.yom.btserver;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.widget.ArrayAdapter;

public class LogFragment extends ListFragment {

    public ArrayAdapter<String>	adapter;
    
    
    public LogFragment() {
    	
    }
    
    @Override
    public void onAttach(Activity activity) {
    	super.onAttach(activity);
    	
		adapter = new ArrayAdapter<String>( getActivity(), android.R.layout.simple_list_item_1 );
    }
    
	@Override
	public void onActivityCreated(Bundle savedInstanceState){
		super.onActivityCreated(savedInstanceState);
		
        setListAdapter(adapter);
	}
}
