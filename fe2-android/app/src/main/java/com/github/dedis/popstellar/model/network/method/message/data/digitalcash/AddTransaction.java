package com.github.dedis.popstellar.model.network.method.message.data.digitalcash;

import android.view.SurfaceControl;

import androidx.annotation.Nullable;

import com.github.dedis.popstellar.model.network.method.message.data.Action;
import com.github.dedis.popstellar.model.network.method.message.data.Data;
import com.github.dedis.popstellar.model.network.method.message.data.Objects;
import com.github.dedis.popstellar.model.objects.Address;
import com.google.gson.annotations.SerializedName;

public class AddTransaction extends Data {
    @SerializedName(value = "transaction")
    private final Transaction transaction;

    @Override
    public String getObject() {
        return Objects.TRANSACTION.getObject();
    }

    @Override
    public String getAction() {
        return Action.POST.getAction();
    }

}
