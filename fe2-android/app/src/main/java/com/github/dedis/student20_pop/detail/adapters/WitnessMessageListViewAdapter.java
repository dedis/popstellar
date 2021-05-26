package com.github.dedis.student20_pop.detail.adapters;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;

import androidx.lifecycle.LifecycleOwner;

import com.github.dedis.student20_pop.detail.LaoDetailViewModel;
import com.github.dedis.student20_pop.model.WitnessMessage;

import java.util.List;

/** Adapter to show the messages that have to be signed by the witnesses  */
public class WitnessMessageListViewAdapter extends BaseAdapter {

    private final LaoDetailViewModel viewModel;

    private List<WitnessMessage> messages;

    private LifecycleOwner lifecycleOwner;

    public WitnessMessageListViewAdapter(
            List<WitnessMessage> messages, LaoDetailViewModel viewModel, LifecycleOwner activity) {
        this.viewModel = viewModel;
        setList(messages);
        lifecycleOwner = activity;
    }

    public void replaceList(List<WitnessMessage> messages) {
        setList(messages);
    }

    private void setList(List<WitnessMessage> messages) {
        this.messages = messages;
        notifyDataSetChanged();
    }

    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    @Override
    public int getCount() {
        return messages != null ? messages.size() : 0;
    }


    @Override
    public Object getItem(int position) {
        return null;
    }

    @Override
    public long getItemId(int position) {
        return 0;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        return null;
    }
}
