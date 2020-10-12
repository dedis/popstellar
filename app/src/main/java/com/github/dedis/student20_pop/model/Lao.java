package com.github.dedis.student20_pop.model;

/**
 * Class modeling a Local Autonomous Organization (LAO)
 */
public class Lao {

    // When launching a LAO the following values are empty (they are not needed for now):
    // unique id, organization id, witness id list, member id list, event id list)
    private String name;

    /**
     * Constructor for a LAO
     *
     * @param name  the name of the LAO
     */
    public Lao(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Lao lao = (Lao) o;
        return name.equals(lao.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
