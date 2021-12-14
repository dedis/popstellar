package com.github.dedis.popstellar;

import android.app.Application;

import dagger.hilt.android.HiltAndroidApp;

/**
 * Application object of the app
 *
 * <p>For now, it is only used by Hilt as an entry point. It extract the object's lifecycle.
 */
@HiltAndroidApp
public class PoPApplication extends Application {}
