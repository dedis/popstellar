package com.github.dedis.student20_pop.utility.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageButton;
import android.widget.TextView;

import com.github.dedis.student20_pop.R;

import java.util.ArrayList;

/**
 * Adapter to show witnesses of an Event
 */
public class WitnessListAdapter extends BaseAdapter {
    private final Context context;
    private final ArrayList<String> witnesses;

    public WitnessListAdapter(Context context, ArrayList<String> witnesses) {
        this.context = context;
        this.witnesses = witnesses;
    }

    /**
     * How many items are in the data set represented by this Adapter.
     *
     * @return Count of items.
     */
    @Override
    public int getCount() {
        return witnesses.size();
    }

    /**
     * Get the data item associated with the specified position in the data set.
     *
     * @param position Position of the item whose data we want within the adapter's
     *                 data set.
     * @return The data at the specified position.
     */
    @Override
    public Object getItem(int position) {
        return witnesses.get(position);
    }

    /**
     * Get the row id associated with the specified position in the list.
     *
     * @param position The position of the item within the adapter's data set whose row id we want.
     * @return The id of the item at the specified position.
     */
    @Override
    public long getItemId(int position) {
        return position;
    }

    /**
     * Get a View that displays the data at the specified position in the data set. You can either
     * create a View manually or inflate it from an XML layout file. When the View is inflated, the
     * parent View (GridView, ListView...) will apply default layout parameters unless you use
     * {@link LayoutInflater#inflate(int, ViewGroup, boolean)}
     * to specify a root view and to prevent attachment to the root.
     *
     * @param position    The position of the item within the adapter's data set of the item whose view
     *                    we want.
     * @param convertView The old view to reuse, if possible. Note: You should check that this view
     *                    is non-null and of an appropriate type before using. If it is not possible to convert
     *                    this view to display the correct data, this method can create a new view.
     *                    Heterogeneous lists can specify their number of view types, so that this View is
     *                    always of the right type (see {@link #getViewTypeCount()} and
     *                    {@link #getItemViewType(int)}).
     * @param parent      The parent that this view will eventually be attached to
     * @return A View corresponding to the data at the specified position.
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            convertView = LayoutInflater.from(context).
                    inflate(R.layout.layout_witnesses_list_view, parent, false);
        }

        ((TextView) convertView.findViewById(R.id.text_view_witness_name))
                .setText(witnesses.get(position));
        ImageButton deleteButton = convertView.findViewById(R.id.image_button_delete_witness);
        deleteButton.setVisibility(parent.getId() == R.id.witness_edit_list ? View.VISIBLE : View.GONE);
        deleteButton.setOnClickListener(
                clicked -> {
                    AlertDialog.Builder adb = new AlertDialog.Builder(context);
                    //TODO : Extract stings in string resources
                    adb.setTitle("Delete ?");
                    adb.setMessage("Are you sure you want to delete Witness n°" + (position + 1));
                    adb.setNegativeButton(context.getString(R.string.button_cancel), null);
                    adb.setPositiveButton(context.getString(R.string.button_confirm), (dialog, which) -> {
                        witnesses.remove(position);
                        notifyDataSetChanged();
                    });
                    adb.show();
                }
        );
        return convertView;
    }
}
