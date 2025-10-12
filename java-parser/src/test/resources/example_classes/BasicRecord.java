package com.example.records;

/**
 * A basic Java record for testing purposes.
 */
public record BasicRecord(String name, int age) {

    // Compact constructor
    public BasicRecord {
        if (age < 0) {
            throw new IllegalArgumentException("Age cannot be negative");
        }
    }

    public String getInfo() {
        return "Name: " + name + ", Age: " + age;
    }
}
