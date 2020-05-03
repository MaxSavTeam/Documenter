package com.maxsavitsky.documenter.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.maxsavitsky.documenter.MainActivity;
import com.maxsavitsky.documenter.R;
import com.maxsavitsky.documenter.data.types.Category;
import com.maxsavitsky.documenter.data.types.Document;
import com.maxsavitsky.documenter.data.types.Entry;
import com.maxsavitsky.documenter.data.types.Type;
import com.maxsavitsky.documenter.utils.Utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class DefaultChooseAdapter extends RecyclerView.Adapter<DefaultChooseAdapter.ViewHolder>{
	private ArrayList<? extends Type> mElements;
	private ArrayList<? extends Type> mStartElements = new ArrayList<>();
	private LayoutInflater mLayoutInflater;
	private View.OnClickListener mOnClickListener;
	private Context mContext;
	private static final String TAG = MainActivity.TAG + " DCAdapter";

	public DefaultChooseAdapter(ArrayList<? extends Type> elements, @Nullable View.OnClickListener onClickListener, Context context) {
		mElements = elements;
		mLayoutInflater = LayoutInflater.from(context);
		mContext = context;
		mOnClickListener = onClickListener;
		Collections.sort( mElements, Utils.getSortByNamesComparator() );
	}

	public void setStartElements(ArrayList<? extends Type> startElements) {
		mStartElements = startElements;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = mLayoutInflater.inflate( R.layout.check_box_list_item, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull final ViewHolder holder, int position) {
		Type element = mElements.get( position );
		holder.mName.setText( element.getName() );
		holder.mId.setText( element.getId() );

		boolean isInStart = false;
		for(Type type : mStartElements){
			if(type.getId().equals( element.getId() )){
				isInStart = true;
				break;
			}
		}
		holder.mCheckBox.setChecked( isInStart );

		ArrayList<String> includedIn = new ArrayList<>();

		if(element.getType().equals( "Entry" )){
			Entry entry = (Entry) element;
			ArrayList<Document> documents = entry.getParentDocuments();
			if(documents != null){
				for(Document document : documents){
					ArrayList<Category> categories = document.getParentCategories();
					if(categories != null){
						for(Category category : categories){
							includedIn.add( category.getName() + " > " + document.getName() );
						}
					}else{
						includedIn.add( "> " + document.getName() );
					}
				}
			}
		}else if(element.getType().equals( "Document" )){
			Document document = (Document) element;
			ArrayList<Category> categories = document.getParentCategories();
			if(categories != null){
				for(Category category : categories){
					includedIn.add( category.getName() );
				}
			}
		}
		Collections.sort( includedIn, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				if(o1.startsWith( ">" ) != o1.startsWith( ">" )){
					if(o1.startsWith( ">" ))
						return -1;
					else
						return 1;
				}
				return o1.compareToIgnoreCase( o2 );
			}
		} );
		Log.i(TAG, "includedIn size = " + includedIn.size());
		if(includedIn.size() > 0) {
			String end = mContext.getString( R.string.included_in ), trimmed = end;
			for (int i = 0; i < includedIn.size(); i++) {
				String path = includedIn.get( i );
				end = String.format( "%s\n\t%s", end, path );
				if((i + 1) < 10)
					trimmed = String.format( "%s\n\t%s", trimmed, path );
				else if(i + 1 == 10)
					trimmed = String.format( "%s\n\t...", trimmed );
			}
			holder.mAdditional.setText( trimmed );
			if(!trimmed.equals( end )){
				final String finalEnd = end;
				holder.mAdditional.setOnClickListener( new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						holder.mAdditional.setMaxLines( 100000 );
						holder.mAdditional.setText( finalEnd );
						holder.mAdditional.setOnClickListener( null );
					}
				} );
			}
			holder.mAdditional.setVisibility( View.VISIBLE );
		}
	}

	@Override
	public int getItemCount() {
		return mElements.size();
	}

	public class ViewHolder extends RecyclerView.ViewHolder {
		public final TextView mName;
		public final TextView mId;
		public final CheckBox mCheckBox;
		public final TextView mAdditional;

		ViewHolder(@NonNull View itemView) {
			super(itemView);
			mId = itemView.findViewById(R.id.checkbox_item_hidden_id);
			mName = itemView.findViewById(R.id.lblNameInCheckbox );
			mCheckBox = itemView.findViewById( R.id.checkBoxInCheckboxItem );
			mAdditional = itemView.findViewById( R.id.lblAdditionalInfo );
			itemView.setOnClickListener(mOnClickListener);
		}
	}
}
