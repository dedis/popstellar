package com.github.dedis.student20_pop.ui;

import android.content.Context;
import android.content.Intent;
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

import com.github.dedis.student20_pop.AttendeeActivity;
import com.github.dedis.student20_pop.OrganizerActivity;
import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Lao;

import java.util.List;

/**
 * Fragment used to display the Home UI
 **/
public final class HomeFragment extends Fragment {

    public static final String TAG = HomeFragment.class.getSimpleName();

    private List<Lao> laos;
    private String id;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);
        PoPApplication app = (PoPApplication) (getActivity().getApplication());
        id = app.getPerson().getId();
        laos = app.getLaos();
        LinearLayout welcome = view.findViewById(R.id.welcome_screen);
        LinearLayout list = view.findViewById(R.id.list_screen);

        if (laos.isEmpty()) {
            welcome.setVisibility(View.VISIBLE);
            list.setVisibility(View.GONE);
        } else {
            welcome.setVisibility(View.GONE);
            list.setVisibility(View.VISIBLE);
        }
        ListView laosListView = view.findViewById(R.id.lao_list);
        laosListView.setAdapter(new LaoListAdapter(this.getContext()));
        return view;
    }

    /**
     * Adapter class required to display the list of LAOs in a ListView
     */
    private class LaoListAdapter extends BaseAdapter {
        private final Context context;

        public LaoListAdapter(Context context) {
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
            PoPApplication app = (PoPApplication) (getActivity().getApplication());
            if (convertView == null) {
                LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                convertView = inflater.inflate(R.layout.layout_lao_home, null);
            }
            Lao lao = laos.get(position);
            ((TextView) convertView.findViewById(R.id.lao_name)).setText(lao.getName());
            ((TextView) convertView.findViewById(R.id.date)).setText("last interacted with ...");
            boolean isOrganizer = (lao.getOrganizer()).equals(id);
            ((TextView) convertView.findViewById(R.id.role)).setText(isOrganizer ? R.string.organizer : R.string.attendee);
            convertView.setOnClickListener(clicked -> {
                app.setCurrentLao(lao);
                Intent intent = new Intent(getActivity(), isOrganizer ? OrganizerActivity.class : AttendeeActivity.class);
                startActivity(intent);
            });
            return convertView;
        }
    }
}
