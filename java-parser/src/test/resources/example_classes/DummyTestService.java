package com.example.service;

import java.util.List;
import org.springframework.stereotype.Service;

/**
 * A dummy test service for validating Java parsing functionality.
 * This class serves as a simple example for testing the parser with
 * basic service annotations and simple methods.
 */
@Service
public class DummyTestService {

    /**
     * Processes a list of test items.
     * @param items the list of items to process
     * @return the count of processed items
     */
    public int processItems(List<String> items) {
        // Simple processing logic for testing
        return items.size();
    }

    /**
     * Validates the test configuration.
     * @return true if configuration is valid
     */
    public boolean validateConfig() {
        // Dummy validation logic
        return true;
    }
}