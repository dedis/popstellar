package com.github.dedis.student20_pop.model;

import androidx.annotation.VisibleForTesting;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class modeling a Person
 */
public class Person {

    private String name;
    private String id;
    private String authentication;
    private List<String> laos; // for now list of the LAOs' id

    /**
     * Constructor for a LAO
     *
     * @param name the name of the Person, can be empty
     *
     */
    public Person(String name) {
        this.name = name;
        this.id = ""; this.authentication = ""; // need to generate public and private key
        this.laos = new ArrayList<>();
    }

    public String getName() {
        return name;
    }

    /**
     *
     * @return public key of the Person, can't be modified
     */
    public String getId() {
        return id;
    }

    /**
     *
     * @return private key of the Person
     */
    public String getAuthentication() {
        return authentication;
    }

    /**
     *
     * @return list of LAOs the Person is subscribed to and/or owns
     */
    public List<String> getLaos() {
        return laos;
    }

    /**
     *
     * @param laos the list of LAOs the Person owns/is a member to
     * @throws IllegalArgumentException if at list one lao value is null
     */
    public void setLaos(List<String> laos) {
        if(laos.contains(null)) {
            throw new IllegalArgumentException("Trying to add a null lao to the Person " + name);
        }
        this.laos = laos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Person person = (Person) o;
        return Objects.equals(name, person.name) &&
                Objects.equals(id, person.id) &&
                Objects.equals(authentication, person.authentication) &&
                Objects.equals(laos, person.laos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, id, authentication, laos);
    }
}
