package com.github.dedis.popstellar.ui.transaction;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.github.dedis.popstellar.R;

import dagger.hilt.android.AndroidEntryPoint;

@AndroidEntryPoint
public class TransactionMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.transaction_main);

        Toolbar toolbar = findViewById(R.id.toolbar_digitalcash);
        setSupportActionBar(toolbar);
    }
}