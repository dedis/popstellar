package com.github.dedis.student20_pop.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Class modeling a Person
 */
public final class Person {

    private final String name;
    private final String id;
    private final String authentication;
    private final List<String> laos; // list of LAOs ids for now

    /**
     * Constructor for a Person
     *
     * @param name the name of the person, can be empty
     * @throws IllegalArgumentException if the name is null
     */
    public Person(String name) {
        if(name == null) {
            throw new IllegalArgumentException("Trying to create a person with a null name");
        }
        this.name = name;
        // Generate the public and private keys
        Keys keys = new Keys();
        this.id = keys.getPublicKey();
        this.authentication = keys.getPrivateKey();
        this.laos = new ArrayList<>();
    }

    /**
     * Private constructor to maintain immutability, only used when want to modify the list of LAOs.
     *
     * @param name the name of the person
     * @param id the public key of the person
     * @param authentication the private key of the person
     * @param laos the new list of LAOs
     */
    private Person(String name, String id, String authentication, List<String> laos) {
        this.name = name;
        this.id = id;
        this.authentication = authentication;
        this.laos = laos;
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
     * @return a new Person with the same name, public and private key, but new list of laos
     * @throws IllegalArgumentException if the list is null or at least one lao value is null
     */
    public Person setLaos(List<String> laos) throws IllegalArgumentException {
        if(laos == null || laos.contains(null)) {
            throw new IllegalArgumentException("Trying to add a null lao to the Person " + name);
        }
        Person person = new Person(name, id, authentication, laos);
        // Overwrite information about this person in the storage
        return person;
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