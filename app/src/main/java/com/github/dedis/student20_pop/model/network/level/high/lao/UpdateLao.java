package com.github.dedis.student20_pop.model.network.level.high.lao;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.github.dedis.student20_pop.model.network.level.high.Action;
import com.github.dedis.student20_pop.model.network.level.high.Message;
import com.github.dedis.student20_pop.model.network.level.high.Objects;

import java.util.ArrayList;
import java.util.List;

public class UpdateLao extends Message {

    private final String name;
    private final long last_modified;
    private final List<String> witnesses;

    @RequiresApi(api = Build.VERSION_CODES.O)
    public UpdateLao(String name, long last_modified, List<String> witnesses) {
        this.name = name;
        this.last_modified = last_modified;
        this.witnesses = witnesses;
    }

    @Override
    public String getObject() {
        return Objects.LAO.getObject();
    }

    @Override
    public String getAction() {
        return Action.UPDATE.getAction();
    }

    public String getName() {
        return name;
    }

    public long getLast_modified() {
        return last_modified;
    }

    public List<String> getWitnesses() {
        return new ArrayList<>(witnesses);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UpdateLao updateLao = (UpdateLao) o;
        return getLast_modified() == updateLao.getLast_modified() &&
                java.util.Objects.equals(getName(), updateLao.getName()) &&
                java.util.Objects.equals(getWitnesses(), updateLao.getWitnesses());
    }

    @Override
    public int hashCode() {
        return java.util.Objects.hash(getName(), getLast_modified(), getWitnesses());
    }
}
