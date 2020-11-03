package com.github.dedis.student20_pop.utility.pollUI;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListAdapter;
import android.widget.TextView;

import com.github.dedis.student20_pop.R;

import java.util.LinkedList;
import java.util.List;

public class ChoicesListViewAdapter extends BaseAdapter {
    private Context context;
    private LinkedList<Integer> choices;


    public ChoicesListViewAdapter(Context context, LinkedList<Integer> choices) {
        this.context = context;
        this.choices = choices;
    }

    @Override
    public int getCount() {
        return choices.size();
    }

    @Override
    public Object getItem(int position) {
        return choices.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater)this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.poll_choices_layout, null);
        }

        ((TextView) convertView.findViewById(R.id.choice_number))
                .setText(getItem(position).toString());

        return convertView;
    }
}
