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

class EitherTest {

    private static Stream<Arguments> provideHashcodeCoverageCases() {
        return Stream.of(
                Arguments.of(Either.left(2), Objects.hash(true, 2)),
                Arguments.of(Either.right(2), Objects.hash(false, 2))
        );
    }

    private static Stream<Arguments> provideEqualityCases() {
        Either<Integer, String> leftEither = Either.left(2);
        Either<String, Integer> rightEither = Either.right(2);
        return Stream.of(
                Arguments.of(leftEither, leftEither, true),
                Arguments.of(leftEither, Either.left(2), true),
                Arguments.of(leftEither, Either.left(3), false),
                Arguments.of(leftEither, null, false),
                Arguments.of(rightEither, rightEither, true),
                Arguments.of(rightEither, Either.right(2), true),
                Arguments.of(rightEither, Either.right(3), false),
                Arguments.of(rightEither, null, false),
                Arguments.of(leftEither, rightEither, false),
                Arguments.of(rightEither, leftEither, false),
                Arguments.of(leftEither, Optional.of(2), false)
        );
    }

    private static Stream<Arguments> provideToStringCases() {
        return Stream.of(
                Arguments.of(Either.left(2), "Either.Left[2]"),
                Arguments.of(Either.left(null), "Either.Left[null]"),
                Arguments.of(Either.right(2), "Either.Right[2]"),
                Arguments.of(Either.right(null), "Either.Right[null]")
        );
    }

    @Test
    void test_givenNonNullData_whenLeftIsCalled_thenLeftEitherIsCreated() {
        String data = "left";
        Either<String, Integer> result = Either.left(data);

        assertTrue(result.isLeft());
        assertFalse(result.isRight());
        assertSame(data, result.getLeft());
        assertNull(result.getRight());
    }

    @Test
    void test_givenNullData_whenLeftIsCalled_thenLeftEitherIsCreatedWithNull() {
        Either<String, Integer> result = Either.left(null);

        assertTrue(result.isLeft());
        assertFalse(result.isRight());
        assertNull(result.getLeft());
        assertNull(result.getRight());
    }

    @Test
    void test_givenNonNullData_whenRightIsCalled_thenRightEitherIsCreated() {
        Integer data = 42;
        Either<String, Integer> result = Either.right(data);

        assertTrue(result.isRight());
        assertFalse(result.isLeft());
        assertSame(data, result.getRight());
        assertNull(result.getLeft());
    }

    @Test
    void test_givenNullData_whenRightIsCalled_thenRightEitherIsCreatedWithNull() {
        Either<String, Integer> result = Either.right(null);

        assertTrue(result.isRight());
        assertFalse(result.isLeft());
        assertNull(result.getRight());
        assertNull(result.getLeft());
    }

    @Test
    void test_givenLeftEither_whenSwapped_thenRightEitherIsCreated() {
        String data = "left";
        Either<String, Integer> result = Either.left(data);
        Either<Integer, String> swapped = result.swap();

        assertTrue(swapped.isRight());
        assertFalse(swapped.isLeft());
        assertSame(data, swapped.getRight());
        assertNull(swapped.getLeft());
    }

    @Test
    void test_givenRightEither_whenSwapped_thenLeftEitherIsCreated() {
        Integer data = 42;
        Either<String, Integer> result = Either.right(data);
        Either<Integer, String> swapped = result.swap();

        assertTrue(swapped.isLeft());
        assertFalse(swapped.isRight());
        assertSame(data, swapped.getLeft());
        assertNull(swapped.getRight());
    }

    @Test
    void test_givenLeftEither_whenMapIsCalled_thenMappedLeftEitherIsReturned() {
        Either<String, Integer> leftEither = Either.left("left");
        Either<String, String> result = leftEither.map(String::toUpperCase, Object::toString);

        assertTrue(result.isLeft());
        assertEquals("LEFT", result.getLeft());
        assertNull(result.getRight());
    }

    @Test
    void test_givenRightEither_whenMapIsCalled_thenMappedRightEitherIsReturned() {
        Either<String, Integer> rightEither = Either.right(42);
        Either<String, String> result = rightEither.map(Object::toString, r -> "Value: " + r);

        assertTrue(result.isRight());
        assertEquals("Value: 42", result.getRight());
        assertNull(result.getLeft());
    }

    @Test
    void test_givenLeftEither_whenFlatMapIsCalled_thenMappedLeftEitherIsReturned() {
        Either<String, Integer> leftEither = Either.left("left");
        Either<String, String> result = leftEither.flatMap(
                left -> Either.left(left.toUpperCase()),
                right -> Either.right("Unexpected")
        );

        assertTrue(result.isLeft());
        assertEquals("LEFT", result.getLeft());
        assertNull(result.getRight());
    }

    @Test
    void test_givenRightEither_whenFlatMapIsCalled_thenMappedRightEitherIsReturned() {
        Either<String, Integer> rightEither = Either.right(42);
        Either<String, String> result = rightEither.flatMap(
                left -> Either.left("Unexpected"),
                right -> Either.right("Value: " + right)
        );

        assertTrue(result.isRight());
        assertEquals("Value: 42", result.getRight());
        assertNull(result.getLeft());
    }

    @Test
    void test_givenLeftEither_whenFlatMapReturnsRight_thenMappedRightEitherIsReturned() {
        Either<String, Integer> leftEither = Either.left("left");
        Either<String, String> result = leftEither.flatMap(
                left -> Either.right("Mapped from left"),
                right -> Either.right("Unexpected")
        );

        assertTrue(result.isRight());
        assertEquals("Mapped from left", result.getRight());
        assertNull(result.getLeft());
    }

    @Test
    void test_givenRightEither_whenFlatMapReturnsLeft_thenMappedLeftEitherIsReturned() {
        Either<String, Integer> rightEither = Either.right(42);
        Either<String, String> result = rightEither.flatMap(
                left -> Either.left("Unexpected"),
                right -> Either.left("Mapped from right")
        );

        assertTrue(result.isLeft());
        assertEquals("Mapped from right", result.getLeft());
        assertNull(result.getRight());
    }

    @MethodSource("provideHashcodeCoverageCases")
    @ParameterizedTest
    void test_givenEither_whenHashcodeIsInvoked_thenRespectiveOutcomeIsReturned(
            Either<?, ?> either,
            int expectedHashcode
    ) {
        int actualHashcode = either.hashCode();
        Assertions.assertEquals(expectedHashcode, actualHashcode);
    }

    @MethodSource("provideEqualityCases")
    @ParameterizedTest
    void test_givenEither_whenEqualsIsInvoked_thenRespectiveOutcomeIsReturned(
            Either<?, ?> either,
            Object object,
            boolean expectedIsEqual
    ) {
        boolean actualIsEquals = either.equals(object);
        Assertions.assertEquals(expectedIsEqual, actualIsEquals);
    }

    @MethodSource("provideToStringCases")
    @ParameterizedTest
    void test_givenEither_whenToStringIsInvoked_thenRespectiveOutcomeIsReturned(
            Either<?, ?> either,
            String expectedToString
    ) {
        String actualToString = either.toString();
        Assertions.assertEquals(expectedToString, actualToString);
    }
}
