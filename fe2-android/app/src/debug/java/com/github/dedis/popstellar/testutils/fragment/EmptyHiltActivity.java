package com.github.dedis.popstellar.testutils.fragment;

import androidx.appcompat.app.AppCompatActivity;

import dagger.hilt.android.AndroidEntryPoint;

/**
 * An empty activity annotated with {@link AndroidEntryPoint}
 *
 * <p>This is used to test Fragments isolated from any activity.
 *
 * <p>The Hilt annotation allows Fragments using Hilt injection to be tested with the activity.
 */
@AndroidEntryPoint(AppCompatActivity.class)
public class EmptyHiltActivity extends Hilt_EmptyHiltActivity {}
