package org.openscience.jmolandroid.search;

import java.util.ArrayList;

import org.openscience.jmolandroid.R;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class PDBResultAdapter extends ArrayAdapter<PDBResult> {

    private ArrayList<PDBResult> items;

    public PDBResultAdapter(Context context, int textViewResourceId, ArrayList<PDBResult> items) {
            super(context, textViewResourceId, items);
            this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater vi = (LayoutInflater)this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = vi.inflate(R.layout.row, null);
            }
            
            PDBResult o = items.get(position);
            ((TextView)convertView.findViewById(R.id.id)).setText(o.getId());
            ((TextView)convertView.findViewById(R.id.description)).setText(o.getDescription());

            return convertView;
    }

}
