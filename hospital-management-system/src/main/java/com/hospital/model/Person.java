package com.hospital.model;

/**
 * Abstract base class representing a person in the hospital system.
 * Demonstrates abstraction (subclasses must implement describe())
 * and encapsulation (private fields with public accessors).
 */
public abstract class Person {

    private int id;
    private String name;
    private String phone;

    public Person(int id, String name, String phone) {
        this.id = id;
        this.name = name;
        this.phone = phone;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    /**
     * Every concrete subclass must describe itself differently.
     * This is the polymorphic hook (overridden by Patient and Doctor).
     */
    public abstract String describe();

    @Override
    public String toString() {
        return describe();
    }
}
