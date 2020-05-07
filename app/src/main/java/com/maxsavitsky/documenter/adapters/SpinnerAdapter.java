package com.maxsavitsky.documenter.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.database.DataSetObserver;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.ThemeActivity;
import com.maxsavitsky.documenter.data.types.Type;

import java.util.ArrayList;

public class SpinnerAdapter implements android.widget.SpinnerAdapter {
	private ArrayList<? extends Type> mElements = new ArrayList<Type>();
	private Context mContext;

	public SpinnerAdapter(ArrayList<? extends Type> elements, Context context) {
		mElements = elements;
		mContext = context;
	}

	@Override
	public View getDropDownView(int position, View convertView, ViewGroup parent) {
		return getView( position, convertView, parent );
	}

	@Override
	public void registerDataSetObserver(DataSetObserver observer) {

	}

	@Override
	public void unregisterDataSetObserver(DataSetObserver observer) {

	}

	@Override
	public int getCount() {
		return mElements.size();
	}

	@Override
	public Object getItem(int position) {
		return mElements.get( position );
	}

	@Override
	public long getItemId(int position) {
		return mElements.get( position ).getId().hashCode();
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		mContext.setTheme( ThemeActivity.CURRENT_THEME );
		LayoutInflater inflater = LayoutInflater.from( mContext );
		@SuppressLint({ "ViewHolder", "InflateParams" }) View view = inflater.inflate( R.layout.spinner_item, null );
		(( TextView) view.findViewById( R.id.spinnerContent )).setText( mElements.get( position ).getName() );
		return view;
	}

	@Override
	public int getItemViewType(int position) {
		return 0;
	}

	@Override
	public int getViewTypeCount() {
		return 1;
	}

	@Override
	public boolean isEmpty() {
		return false;
	}
}
