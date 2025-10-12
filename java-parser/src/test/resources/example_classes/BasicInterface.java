package com.example.interfaces;

/**
 * A basic Java interface for testing purposes.
 */
public interface BasicInterface {

    String DEFAULT_MESSAGE = "Hello from BasicInterface";

    String getMessage();

    default void printMessage() {
        System.out.println(getMessage());
    }
}
