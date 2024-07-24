package org.zeplinko.commons.lang.ext.core;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

public class TryTest {

    @Test
    void testSuccess() {
        Try aTry = Try.to(() -> "Hello, World!");

        assertInstanceOf(Try.Success.class, aTry);
        assertEquals("Hello, World!", ((Try.Success<?>) aTry).getValue());
    }

    @Test
    void testFailure() {
        Try aTry = Try.to(() -> {
            throw new Exception("Test exception");
        });

        assertInstanceOf(Try.Failure.class, aTry);
        assertEquals("Test exception", ((Try.Failure) aTry).getThrowable().getMessage());
    }

    @Test
    void testSuccessWithInteger() {
        Try aTry = Try.to(() -> 42);

        assertInstanceOf(Try.Success.class, aTry);
        assertEquals(42, ((Try.Success<?>) aTry).getValue());
    }

    @Test
    void testFailureWithRuntimeException() {
        Try aTry = Try.to(() -> {
            throw new RuntimeException("Runtime exception");
        });

        assertInstanceOf(Try.Failure.class, aTry);
        assertEquals("Runtime exception", ((Try.Failure) aTry).getThrowable().getMessage());
    }

}
