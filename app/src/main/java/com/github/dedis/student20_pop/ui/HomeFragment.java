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
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Keys;
import com.github.dedis.student20_pop.model.Lao;

import java.util.ArrayList;
import java.util.Date;

/**
 * Fragment used to display the Home UI
**/
public final class HomeFragment extends Fragment {

    public static final String TAG = HomeFragment.class.getSimpleName();

    //TODO: retrieve from the backend, more specifically from the Person's list of Lao's
    private ArrayList<Lao> laos;
    //Now: for testing, later: retrieve from Person
    private Keys keys = new Keys();
    private String myPublicKey = keys.getPublicKey();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // TODO: retrieve list of LAOs from backend and display them

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        laos = getLaos();

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
            ((TextView) convertView.findViewById(R.id.date)).setText("last interacted with ...");
            boolean isOrganizer = lao.getOrganizer().equals(myPublicKey);
            if (isOrganizer){
                ((TextView) convertView.findViewById(R.id.role)).setText(R.string.organizer);
            }else {
                ((TextView) convertView.findViewById(R.id.role)).setText(R.string.attendee);
            }
            convertView.setOnClickListener(clicked -> {
               if (isOrganizer){
                   startOrganizerUI(lao);
               }else {
                   startAttendeeUI(lao);
               }
            });
            return convertView;
        }
    }

    private ArrayList<Lao> getLaos(){
        //Now: for testing, later: TODO: retrieve from Person's list of Lao
        String notMyPublicKey = new Keys().getPublicKey();
        ArrayList<Lao> result = new ArrayList<>();
        result.add(new Lao("LAO 1", new Date(), notMyPublicKey));
        result.add(new Lao("LAO 2", new Date(), myPublicKey));
        result.add(new Lao("LAO 3", new Date(), notMyPublicKey));
        result.add(new Lao("LAO 4", new Date(), notMyPublicKey));
        result.add(new Lao("LAO 5", new Date(), myPublicKey));
        result.add(new Lao("LAO 6", new Date(), notMyPublicKey));
        return result;
    }


    /**
     * Method to start the organizer UI in the case when the user is the organizer of
     * the selected Lao
     * @param lao
     */
    private void startOrganizerUI(Lao lao){
        Intent intent = new Intent(this.getActivity(), OrganizerActivity.class);
        //TODO: put lao in intent (or PopContext)
        startActivity(intent);
    }

    /**
     * Method to start the attendee UI in the case when the user is not the organizer of
     * the selected Lao
     * @param lao
     */
    private void startAttendeeUI(Lao lao){
        Intent intent = new Intent(this.getActivity(), AttendeeActivity.class);
        //TODO: put lao in intent (or PopContext)
        startActivity(intent);
    }
}
