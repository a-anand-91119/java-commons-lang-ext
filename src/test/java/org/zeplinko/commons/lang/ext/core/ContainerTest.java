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

class ContainerTest {

    private static Stream<Arguments> provideHashcodeCoverageCases() {
        return Stream.of(
                Arguments.of(Container.of(2), Objects.hash(2)),
                Arguments.of(Container.of(3), Objects.hash(3))
        );
    }

    private static Stream<Arguments> provideEqualityCases() {
        Container<Integer> container = Container.of(2);
        return Stream.of(
                Arguments.of(container, container, true),
                Arguments.of(container, Container.of(2), true),
                Arguments.of(container, Container.of(3), false),
                Arguments.of(container, Optional.of(3), false),
                Arguments.of(container, null, false)
        );
    }

    private static Stream<Arguments> provideToStringCases() {
        return Stream.of(
                Arguments.of(Container.of(2), "Container[2]"),
                Arguments.of(Container.of(null), "Container[null]")
        );
    }

    @Test
    void test_givenNonNullValue_whenGetValueIsCalled_thenCorrectValueIsReturned() {
        String data = "test value";
        Container<String> container = Container.of(data);

        assertSame(data, container.getValue());
    }

    @Test
    void test_givenNullValue_whenGetValueIsCalled_thenNullIsReturned() {
        Container<String> container = Container.of(null);

        assertNull(container.getValue());
    }

    @Test
    void test_givenIntegerValue_whenGetValueIsCalled_thenCorrectValueIsReturned() {
        Integer data = 123;
        Container<Integer> container = Container.of(data);

        assertEquals(123, container.getValue());
    }

    @Test
    void test_givenCustomObjectValue_whenGetValueIsCalled_thenCorrectObjectIsReturned() {
        MyCustomClass customObject = new MyCustomClass("custom value");
        Container<MyCustomClass> container = Container.of(customObject);

        assertSame(customObject, container.getValue());
    }

    @Test
    void test_givenNonNullValue_whenConstructedUsingFactoryMethodOf_thenCorrectValueIsStored() {
        Container<String> container = Container.of("test-value");

        assertEquals("test-value", container.getValue());
    }

    @Test
    void test_givenNullValue_whenConstructedUsingFactoryMethodOf_thenNullIsStored() {
        Container<String> container = Container.of(null);

        assertNull(container.getValue());
    }

    @MethodSource("provideHashcodeCoverageCases")
    @ParameterizedTest
    void test_givenContainer_whenHashcodeIsInvoked_thenRespectiveOutcomeIsReturned(
            Container<?> container,
            int expectedHashcode
    ) {
        int actualHashcode = container.hashCode();
        Assertions.assertEquals(expectedHashcode, actualHashcode);
    }

    @MethodSource("provideEqualityCases")
    @ParameterizedTest
    void test_givenContainer_whenEqualsIsInvoked_thenRespectiveOutcomeIsReturned(
            Container<?> container1,
            Object object,
            boolean expectedIsEqual
    ) {
        boolean actualIsEquals = container1.equals(object);
        Assertions.assertEquals(expectedIsEqual, actualIsEquals);
    }

    @MethodSource("provideToStringCases")
    @ParameterizedTest
    void test_givenContainer_whenToStringIsInvoked_thenRespectiveOutcomeIsReturned(
            Container<?> container,
            String expectedToString
    ) {
        String actualToString = container.toString();
        Assertions.assertEquals(expectedToString, actualToString);
    }

    static class MyCustomClass {
        private final String value;

        public MyCustomClass(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }
    }
}
