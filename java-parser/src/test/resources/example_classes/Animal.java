package com.example.animals;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.validation.constraints.AssertTrue;

/**
 * Represents a generic animal entity in the system.
 * This class is intended as a base type for specific animal implementations.
 */
@Entity
@Table(name = "animals")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
public class Animal {

    /**
     * Primary key identifier for the animal.
     */
    @Id
    private long id;

    /**
     * Describes the type of animal, e.g., "Cat", "Shark", etc.
     */
    private String animalTypeName;

    /**
     * Default constructor required by JPA.
     */
    public Animal() {
        // No-arg constructor
    }

    /**
     * Gets the unique identifier of the animal.
     * @return the primary key ID
     */
    public long getId() {
        return id;
    }

    /**
     * Gets the type name of the animal.
     * @return the animal type name
     */
    public String getAnimalTypeName() {
        return animalTypeName;
    }

    /**
     * Sets the type name of the animal.
     * @param animalTypeName the new type name (e.g., "Cat", "Shark")
     */
    public void setAnimalTypeName(String animalTypeName) {
        this.animalTypeName = animalTypeName;
    }

    /**
     * Validates that the animal type name contains no digits.
     * @return true if the name has no numbers
     */
    @AssertTrue
    public boolean isAnimalTypeValid() {
        return animalTypeName != null && animalTypeName.matches("^[^\\d]*$");
    }
}