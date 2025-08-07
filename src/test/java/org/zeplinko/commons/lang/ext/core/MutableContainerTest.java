package org.zeplinko.commons.lang.ext.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class MutableContainerTest {

    private static Stream<Arguments> provideHashcodeCoverageCases() {
        return Stream.of(
                Arguments.of(MutableContainer.of(2), Objects.hash(2)),
                Arguments.of(MutableContainer.of(3), Objects.hash(3))
        );
    }

    private static Stream<Arguments> provideEqualityCases() {
        MutableContainer<Integer> mutableContainer = MutableContainer.of(2);
        return Stream.of(
                Arguments.of(mutableContainer, mutableContainer, true),
                Arguments.of(mutableContainer, MutableContainer.of(2), true),
                Arguments.of(mutableContainer, Container.of(2), true),
                Arguments.of(mutableContainer, MutableContainer.of(3), false),
                Arguments.of(mutableContainer, Optional.of(3), false),
                Arguments.of(mutableContainer, null, false)
        );
    }

    private static Stream<Arguments> provideToStringCases() {
        return Stream.of(
                Arguments.of(MutableContainer.of(2), "MutableContainer[2]"),
                Arguments.of(MutableContainer.of(null), "MutableContainer[null]")
        );
    }

    @Test
    void test_givenNonNullInitialValue_whenGetValueIsCalled_thenCorrectValueIsReturned() {
        String data = "initial";
        MutableContainer<String> container = new MutableContainer<>(data);

        assertSame(data, container.getValue());
    }

    @Test
    void test_givenNullInitialValue_whenGetValueIsCalled_thenNullIsReturned() {
        MutableContainer<String> container = new MutableContainer<>(null);

        assertNull(container.getValue());
    }

    @Test
    void test_givenExistingValue_whenSetValueWithNewNonNullValue_thenValueIsUpdated() {
        MutableContainer<String> container = new MutableContainer<>("old");
        String newValue = "new";

        container.setValue(newValue);

        assertSame(newValue, container.getValue());
    }

    @Test
    void test_givenExistingValue_whenSetValueWithNull_thenValueIsCleared() {
        MutableContainer<String> container = new MutableContainer<>("data");

        container.setValue(null);

        assertNull(container.getValue());
    }

    @Test
    void test_givenNonNullValue_whenConstructedUsingFactoryMethodOf_thenCorrectValueIsStored() {
        MutableContainer<String> container = MutableContainer.of("test-value");

        assertEquals("test-value", container.getValue());
    }

    @Test
    void test_givenNullValue_whenConstructedUsingFactoryMethodOf_thenNullIsStored() {
        MutableContainer<String> container = MutableContainer.of(null);

        assertNull(container.getValue());
    }

    @MethodSource("provideHashcodeCoverageCases")
    @ParameterizedTest
    void test_givenMutableContainer_whenHashcodeIsInvoked_thenRespectiveOutcomeIsReturned(
            MutableContainer<?> mutableContainer,
            int expectedHashcode
    ) {
        int actualHashcode = mutableContainer.hashCode();
        Assertions.assertEquals(expectedHashcode, actualHashcode);
    }

    @MethodSource("provideEqualityCases")
    @ParameterizedTest
    void test_givenContainer_whenEqualsIsInvoked_thenRespectiveOutcomeIsReturned(
            MutableContainer<?> mutableContainer,
            Object object,
            boolean expectedIsEqual
    ) {
        boolean actualIsEquals = mutableContainer.equals(object);
        Assertions.assertEquals(expectedIsEqual, actualIsEquals);
    }

    @MethodSource("provideToStringCases")
    @ParameterizedTest
    void test_givenMutableContainer_whenToStringIsInvoked_thenRespectiveOutcomeIsReturned(
            MutableContainer<?> mutableContainer,
            String expectedToString
    ) {
        String actualToString = mutableContainer.toString();
        Assertions.assertEquals(expectedToString, actualToString);
    }
}
