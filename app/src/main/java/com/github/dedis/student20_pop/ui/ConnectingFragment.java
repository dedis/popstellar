package com.github.dedis.student20_pop.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.fragment.app.Fragment;

import com.github.dedis.student20_pop.MainActivity;
import com.github.dedis.student20_pop.PoPApplication;
import com.github.dedis.student20_pop.R;
import com.github.dedis.student20_pop.utility.protocol.HighLevelProxy;

import java.net.URI;
import java.util.concurrent.CompletableFuture;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ConnectingFragment#newInstance} factory method to
 * create an instance of this fragment.
 */
public final class ConnectingFragment extends Fragment {

    public static final String TAG = ConnectingFragment.class.getSimpleName();
    private static final String URL_EXTRA = "url";
    private static final String LAO_EXTRA = "lao";

    private String url;
    private String lao;
    private CompletableFuture<Integer> connexion;

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param url to connect to
     * @return A new instance of fragment ConnectingFragment.
     */
    public static ConnectingFragment newInstance(String url, String lao) {
        ConnectingFragment fragment = new ConnectingFragment();
        Bundle args = new Bundle();
        args.putString(URL_EXTRA, url);
        args.putString(LAO_EXTRA, lao);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            url = getArguments().getString(URL_EXTRA);
            lao = getArguments().getString(LAO_EXTRA);

            HighLevelProxy proxy = ((PoPApplication) getActivity().getApplication())
                    .getProxy(URI.create(url));

            String channel = HighLevelProxy.ROOT + "/" + lao;

            connexion = proxy.lowLevel().subscribe(channel);
            connexion.thenCompose(i -> proxy.lowLevel().catchup(channel))
                .thenAccept(msgs -> getActivity().runOnUiThread(() -> {
                    ((PoPApplication) getActivity().getApplication()).handleDataMessages(msgs);
                    Intent intent = new Intent(getActivity(), MainActivity.class);
                    startActivity(intent);
                })).exceptionally(t -> {
                    Throwable real = t.getCause() == null ? t : t.getCause();
                    if(!(real instanceof ManualCancel))
                        Toast.makeText(getContext(), real.getMessage(), Toast.LENGTH_LONG).show();

                    if(!(real instanceof ChangeViewCancel))
                        getActivity().runOnUiThread(() -> {
                                    Intent intent = new Intent(getActivity(), MainActivity.class);
                                    startActivity(intent);
                    });
                    return null;
                });
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        final View view = inflater.inflate(R.layout.fragment_connecting, container, false);

        TextView url_view = view.findViewById(R.id.connecting_url);
        TextView lao_view = view.findViewById(R.id.connecting_lao);

        url_view.setText(url);
        lao_view.setText(lao);

        Button cancelButton = view.findViewById(R.id.button_cancel_connecting);
        cancelButton.setOnClickListener(v -> connexion.completeExceptionally(new ManualCancel()));

        return view;
    }

    @Override
    public void onStop() {
        connexion.completeExceptionally(new ChangeViewCancel());
        super.onStop();
    }

    /**
     * Dummy exception to avoid showing toast when the user cancel the connection
     */
    private static final class ManualCancel extends Throwable {}

    /**
     * Dummy exception to avoid showing main activity when the user cancel the connection by changing the view
     */
    private static final class ChangeViewCancel extends Exception {

        private ChangeViewCancel() {
            super("Connection to lao cancelled");
        }
    }
}