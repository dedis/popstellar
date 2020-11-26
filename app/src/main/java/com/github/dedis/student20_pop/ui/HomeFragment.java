package com.github.dedis.student20_pop.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Lao;

import java.util.ArrayList;

/**
 * Fragment used to display the Home UI
**/
public final class HomeFragment extends Fragment {

    public static final String TAG = HomeFragment.class.getSimpleName();

    //retrieved from the backend, more specifically from the Person's list of Lao's
    private ArrayList<Lao> laos;
    private String myPublicKey;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // TODO: retrieve list of LAOs from backend and display them

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        LinearLayout welcome = view.findViewById(R.id.welcome_screen);
        LinearLayout list = view.findViewById(R.id.list_screen);

        if (laos.isEmpty()) {
            welcome.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
        }else{
            welcome.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
        }
        ListView laosListView = view.findViewById(R.id.lao_list);
        laosListView.setAdapter(new LaoListAdapter(this.getContext()));
        return view;
    }

    private class LaoListAdapter extends BaseAdapter {
        private Context context;

        public LaoListAdapter(Context context){
            this.context = context;
        }

        @Override
        public int getCount() {
            return laos.size();
        }

        @Override
        public Object getItem(int position) {
            return laos.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.layout_lao_home, null);
            }
            Lao lao = laos.get(position);
            ((TextView) convertView.findViewById(R.id.lao_name)).setText(lao.getName());
            ((TextView) convertView.findViewById(R.id.date)).setText("last interacted with");
            if (lao.getOrganizer().equals(myPublicKey)){
                ((TextView) convertView.findViewById(R.id.role)).setText(R.string.organizer);
            }else {
                ((TextView) convertView.findViewById(R.id.role)).setText(R.string.attendee);
            }
            return convertView;
        }
    }




}
