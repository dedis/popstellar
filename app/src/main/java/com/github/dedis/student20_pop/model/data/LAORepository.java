package com.github.dedis.student20_pop.model.data;

import androidx.annotation.NonNull;

import com.github.dedis.student20_pop.model.network.GenericMessage;

import io.reactivex.Flowable;

public class LAORepository {
    private volatile static LAORepository INSTANCE = null;

    private final LAODataSource mRemoteDataSource;

    private LAORepository(@NonNull LAODataSource remoteDataSource) {
        mRemoteDataSource = remoteDataSource;
    }

    public static LAORepository getInstance(LAODataSource laoRemoteDataSource) {
        if (INSTANCE == null) {
            synchronized (LAORepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LAORepository(laoRemoteDataSource);
                }
            }
        }
        return INSTANCE;
    }

    public static void destroyInstance() {
        INSTANCE = null;
    }

    public Flowable<GenericMessage> observeMessage() {
        return mRemoteDataSource.observeMessage();
    }

}

