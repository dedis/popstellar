package com.github.dedis.student20_pop.ui;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.model.Lao;
import com.github.dedis.student20_pop.model.entities.LAOEntity;
import com.github.dedis.student20_pop.model.event.Event;
import com.github.dedis.student20_pop.utility.ui.adapter.OrganizerEventExpandableListViewAdapter;
import com.github.dedis.student20_pop.utility.ui.adapter.WitnessListViewAdapter;
import com.github.dedis.student20_pop.utility.ui.listener.OnAddWitnessListener;
import com.github.dedis.student20_pop.utility.ui.listener.OnEventTypeSelectedListener;
import java.util.List;

/** Fragment used to display Organizer's UI */
public class OrganizerFragment extends Fragment {
  public static final String TAG = AttendeeFragment.class.getSimpleName();

  private final LAOEntity laoEntity;


  private OnEventTypeSelectedListener onEventTypeSelectedListener;
  private OnAddWitnessListener onAddWitnessListener;

  public OrganizerFragment(LAOEntity laoEntity) {
      this.laoEntity = laoEntity;
  }

  public static OrganizerFragment newInstance(LAOEntity laoEntity) {
      return new OrganizerFragment(laoEntity);
  }

  @Override
  public void onAttach(@NonNull Context context) {
    super.onAttach(context);
    /*
    if (context instanceof OnEventCreatedListener)
      onEventTypeSelectedListener = (OnEventTypeSelectedListener) context;
    else
      throw new ClassCastException(
          context.toString() + " must implement OnEventTypeSelectedListener");

    if (context instanceof OnAddWitnessListener)
      onAddWitnessListener = (OnAddWitnessListener) context;
    else throw new ClassCastException(context.toString() + " must implement OnAddWitnessListener");*/
  }

  @Nullable
  @Override
  public View onCreateView(
      @NonNull LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {

    View rootView = inflater.inflate(R.layout.fragment_lao_detail, container, false);
/*
    PoPApplication app = (PoPApplication) (getActivity().getApplication());
    Lao lao = app.getCurrentLaoUnsafe();

    ImageButton editPropertiesButton;
    ImageButton addWitnessButton;
    EditText laoNameEditText;
    TextView laoNameTextView;

    List<Event> events = lao.getEvents();
    // Layout Properties fields && Edit Properties field
    View propertiesView = rootView.findViewById(R.id.properties_view);
    laoNameTextView = propertiesView.findViewById(R.id.organization_name);
    laoNameTextView.setText(lao.getName());

    final WitnessListViewAdapter witnessListViewAdapter =
        new WitnessListViewAdapter(getActivity(), lao.getWitnesses());
    ListView witnessesListView = propertiesView.findViewById(R.id.witness_list);
    witnessesListView.setAdapter(witnessListViewAdapter);

    ViewSwitcher viewSwitcher = rootView.findViewById(R.id.view_switcher);
    editPropertiesButton = rootView.findViewById(R.id.edit_button);
    editPropertiesButton.setVisibility(
        ((viewSwitcher.getNextView().getId() == R.id.properties_edit_view)
                && (viewSwitcher.getVisibility() == View.VISIBLE))
            ? View.VISIBLE
            : View.GONE);

    // Layout Edit Properties fields
    View propertiesEditView = rootView.findViewById(R.id.properties_edit_view);
    laoNameEditText = propertiesEditView.findViewById(R.id.organization_name);
    laoNameEditText.setText(lao.getName());
    ListView witnessesEditListView = propertiesEditView.findViewById(R.id.witness_list);
    witnessesEditListView.setAdapter(witnessListViewAdapter);
    Button propertiesButton = rootView.findViewById(R.id.tab_properties);
    propertiesButton.setOnClickListener(
        clicked -> {
          viewSwitcher.setVisibility(
              (viewSwitcher.getVisibility() == View.GONE) ? View.VISIBLE : View.GONE);
          editPropertiesButton.setVisibility(
              ((viewSwitcher.getNextView().getId() == R.id.properties_edit_view)
                      && (viewSwitcher.getVisibility() == View.VISIBLE))
                  ? View.VISIBLE
                  : View.GONE);
        });

      addWitnessButton = propertiesEditView.findViewById(R.id.add_witness_button);
      Button confirmButton = propertiesEditView.findViewById(R.id.properties_edit_confirm);

    propertiesEditView
        .findViewById(R.id.properties_edit_cancel)
        .setOnClickListener(
            c -> {
              viewSwitcher.showNext();
              editPropertiesButton.setVisibility(View.VISIBLE);
              addWitnessButton.setVisibility(View.GONE);
            });

    editPropertiesButton.setOnClickListener(
        clicked -> {
          viewSwitcher.showNext();
          editPropertiesButton.setVisibility(View.GONE);
          addWitnessButton.setVisibility(View.VISIBLE);
        });



    addWitnessButton.setOnClickListener(clicked -> onAddWitnessListener.onAddWitnessListener());

    confirmButton.setOnClickListener(
        clicked -> {
          String title = laoNameEditText.getText().toString().trim();
          if (!title.isEmpty()) {
            lao.setName(title);
            viewSwitcher.showNext();
            laoNameTextView.setText(laoNameEditText.getText());
            editPropertiesButton.setVisibility(View.VISIBLE);
            addWitnessButton.setVisibility(View.GONE);
            // TODO : If LAO's name has changed : tell backend to update it
          } else {
            Toast.makeText(
                    getContext(),
                    getString(R.string.exception_message_empty_lao_name),
                    Toast.LENGTH_SHORT)
                .show();
          }
        });







      // Display Events
      ExpandableListView expandableListView =
              rootView.findViewById(R.id.exp_list_view);
      OrganizerEventExpandableListViewAdapter listViewEventAdapter =
              new OrganizerEventExpandableListViewAdapter(
                      this.getActivity(), events, onEventTypeSelectedListener);
      expandableListView.setAdapter(listViewEventAdapter);
      expandableListView.expandGroup(0);
      expandableListView.expandGroup(1);


      SwipeRefreshLayout swipeRefreshLayout = rootView.findViewById(R.id.swipe_refresh);

    swipeRefreshLayout.setOnRefreshListener(
        () -> {
          witnessListViewAdapter.notifyDataSetChanged();
          listViewEventAdapter.notifyDataSetChanged();
          if (getFragmentManager() != null) {
            getFragmentManager().beginTransaction().detach(this).attach(this).commit();
          }
          swipeRefreshLayout.setRefreshing(false);
        });
*/
    return rootView;
  }
}
