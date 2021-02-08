package com.github.dedis.student20_pop.ui;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.Layout;
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
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.github.dedis.student20_pop.AttendeeActivity;
import com.github.dedis.student20_pop.OrganizerActivity;
import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.databinding.FragmentHomeBinding;
import com.github.dedis.student20_pop.home.HomeActivity;
import com.github.dedis.student20_pop.home.HomeViewModel;
import com.github.dedis.student20_pop.home.LAOListAdapter;
import com.github.dedis.student20_pop.model.Lao;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Fragment used to display the Home UI */
public final class HomeFragment extends Fragment {

  public static final String TAG = HomeFragment.class.getSimpleName();
  public static final SimpleDateFormat DATE_FORMAT =
      new SimpleDateFormat("dd/MM/yy", Locale.ENGLISH);

  private List<Lao> laos;
  private String id;

  private FragmentHomeBinding mHomeFragBinding;

  private HomeViewModel mHomeViewModel;

  private LAOListAdapter mListAdapter;

  public HomeFragment() {

  }

  public static HomeFragment newInstance() {
    return new HomeFragment();
  }

  @Nullable
  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    mHomeFragBinding = FragmentHomeBinding.inflate(inflater, container, false);

    mHomeViewModel = HomeActivity.obtainViewModel(getActivity());

    mHomeFragBinding.setViewmodel(mHomeViewModel);
    mHomeFragBinding.setLifecycleOwner(getActivity());

    return mHomeFragBinding.getRoot();
  }

  @Override
  public void onActivityCreated(@Nullable Bundle savedInstanceState) {
    super.onActivityCreated(savedInstanceState);

    setupListAdapter();
  }

  private void setupListAdapter() {
    ListView listView = mHomeFragBinding.laoList;

    mListAdapter = new LAOListAdapter(
            new ArrayList<Lao>(0),
            mHomeViewModel,
            getActivity()
    );

    listView.setAdapter(mListAdapter);
  }

  /*
  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    View view = inflater.inflate(R.layout.fragment_home, container, false);
    PoPApplication app = (PoPApplication) (getActivity().getApplication());
    id = app.getPerson().getId();
    laos = app.getLaos();
    LinearLayout welcome = view.findViewById(R.id.welcome_screen);
    LinearLayout list = view.findViewById(R.id.list_screen);
    SwipeRefreshLayout swipeRefreshLayout = view.findViewById(R.id.swipe_refresh);

    if (laos.isEmpty()) {
      welcome.setVisibility(View.VISIBLE);
      list.setVisibility(View.GONE);
    } else {
      welcome.setVisibility(View.GONE);
      list.setVisibility(View.VISIBLE);
    }
    ListView laosListView = view.findViewById(R.id.lao_list);
    LaoListAdapter adapter = new LaoListAdapter(this.getContext(), laos);
    laosListView.setAdapter(adapter);
    swipeRefreshLayout.setOnRefreshListener(
        () -> {
          adapter.notifyDataSetChanged();
          if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
          }
          swipeRefreshLayout.setRefreshing(false);
        });
    return view;
  }

  /** Adapter class required to display the list of LAOs in a ListView
  private class LaoListAdapter extends BaseAdapter {
    private final Context context;
    private List<Lao> laoList;

    public LaoListAdapter(Context context, List<Lao> laos) {
      this.context = context;
      this.laoList = laos;
    }

    @Override
    public int getCount() {
      return laoList.size();
    }

    @Override
    public Object getItem(int position) {
      return laoList.get(position);
    }

    @Override
    public long getItemId(int position) {
      return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
      PoPApplication app = (PoPApplication) (getActivity().getApplication());
      if (convertView == null) {
        LayoutInflater inflater =
            (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        convertView = inflater.inflate(R.layout.layout_lao_home, null);
      }
      Lao lao = laoList.get(position);
      ((TextView) convertView.findViewById(R.id.lao_name)).setText(lao.getName());
      ((TextView) convertView.findViewById(R.id.date))
          .setText(DATE_FORMAT.format(lao.getTime() * 1000L));
      boolean isOrganizer = (lao.getOrganizer()).equals(id);
      ((TextView) convertView.findViewById(R.id.role))
          .setText(isOrganizer ? R.string.organizer : R.string.attendee);
      convertView.setOnClickListener(
          clicked -> {
            app.setCurrentLao(lao);
            Intent intent =
                new Intent(
                    getActivity(), isOrganizer ? OrganizerActivity.class : AttendeeActivity.class);
            startActivity(intent);
          });
      return convertView;
    }
  }
  */
}
