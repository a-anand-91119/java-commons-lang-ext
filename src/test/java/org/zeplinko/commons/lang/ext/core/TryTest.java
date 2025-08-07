package org.zeplinko.commons.lang.ext.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TryTest {
    private static Stream<Arguments> provideHashcodeCoverageCases() {
        RuntimeException runtimeException = new RuntimeException();
        return Stream.of(
                Arguments.of(Try.success(2), Objects.hash(2, null)),
                Arguments.of(Try.failure(runtimeException), Objects.hash(null, runtimeException))
        );
    }

    private static Stream<Arguments> provideEqualityCases() {
        Try<Integer> successTry = Try.success(5);
        RuntimeException runtimeException = new RuntimeException("Hello");
        Try<Void> failureTry = Try.failure(runtimeException);
        return Stream.of(
                Arguments.of(successTry, successTry, true),
                Arguments.of(successTry, Try.success(5), true),
                Arguments.of(successTry, Try.success(6), false),
                Arguments.of(successTry, Optional.of(6), false),
                Arguments.of(successTry, null, false),
                Arguments.of(failureTry, failureTry, true),
                Arguments.of(failureTry, Try.<Void>failure(runtimeException), true),
                Arguments.of(failureTry, Try.<Void>failure(new IllegalArgumentException()), false),
                Arguments.of(failureTry, Optional.of(new IllegalArgumentException()), false),
                Arguments.of(failureTry, null, false),
                Arguments.of(failureTry, successTry, false)
        );
    }

    private static Stream<Arguments> provideToStringCases() {
        return Stream.of(
                Arguments.of(Try.success(2), "Try.Success[2]"),
                Arguments.of(Try.success(null), "Try.Success[null]"),
                Arguments.of(Try.failure(new RuntimeException()), "Try.Failure[java.lang.RuntimeException]")
        );
    }

    @Test
    void test_givenNonNullData_whenSuccessIsCalled_thenSuccessTryIsCreated() {
        String data = "success";
        Try<String> result = Try.success(data);

        assertTrue(result.isSuccess());
        assertSame(data, result.getData());
        assertNull(result.getError());
    }

    @Test
    void test_givenNullData_whenSuccessIsCalled_thenSuccessTryIsCreated() {
        Try<String> result = assertDoesNotThrow(() -> Try.success(null));

        assertTrue(result.isSuccess());
        assertNull(result.getData());
        assertNull(result.getError());
    }

    @Test
    void test_givenNonNullException_whenFailureIsCalled_thenFailureTryIsCreated() {
        Exception error = new RuntimeException("error");
        Try<String> result = Try.failure(error);

        assertTrue(result.isFailure());
        assertNull(result.getData());
        assertSame(error, result.getError());
    }

    @Test
    void test_givenNullException_whenFailureIsCalled_thenExceptionIsThrown() {
        @SuppressWarnings("DataFlowIssue")
        NullPointerException nullPointerException = assertThrows(NullPointerException.class, () -> Try.failure(null));

        assertNotNull(nullPointerException);
    }

    @Test
    void test_whenToIsCalledWithNullCallable_thenExceptionIsThrown() {
        @SuppressWarnings("DataFlowIssue")
        NullPointerException nullPointerException = assertThrows(NullPointerException.class, () -> Try.to(null));

        assertNotNull(nullPointerException);
    }

    @Test
    void test_whenToIsCalledWithCallableThatSucceeds_thenSuccessTryIsReturned() {
        Callable<String> callable = () -> "success";
        Try<String> result = Try.to(callable);

        assertTrue(result.isSuccess());
        assertEquals("success", result.getData());
    }

    @Test
    void test_whenToIsCalledWithCallableThatThrowsInterruptedException_thenFailureTryIsReturned() {
        Exception expectedError = new InterruptedException("error");
        Callable<String> callable = () -> {
            throw expectedError;
        };

        Try<String> result = Try.to(callable);

        assertTrue(result.isFailure());
        assertSame(expectedError, result.getError());
    }

    @Test
    void test_whenToIsCalledWithCallableThatThrowsException_thenFailureTryIsReturned() {
        Exception expectedError = new RuntimeException("error");
        Callable<String> callable = () -> {
            throw expectedError;
        };

        Try<String> result = Try.to(callable);

        assertTrue(result.isFailure());
        assertSame(expectedError, result.getError());
    }

    @Test
    void test_givenSuccessTry_whenOrElse_thenOriginalValueShouldBeReturned() {
        Try<Integer> integerTry = Try.success(10);
        int value = integerTry.orElse(5);
        Assertions.assertEquals(10, value);
    }

    @Test
    void test_givenFailureTry_whenOrElse_thenNewValueShouldBeReturned() {
        Try<Integer> failure = Try.failure(new RuntimeException("Error occurred"));
        int value = failure.orElse(5);
        Assertions.assertEquals(5, value);
    }

    @Test
    void test_whenOrElseGetSupplierCalledOnSuccessTry_thenSupplierIsNotCalled() {
        Try<Integer> success = Try.success(10);
        int value = success.orElseGet(() -> 5);
        Assertions.assertEquals(10, value);
    }

    @Test
    void test_whenOrElseGetSupplierCalledOnFailureTry_thenSupplierIsNotCalled() {
        Try<Integer> failure = Try.failure(new RuntimeException("Error occurred"));
        int value = failure.orElseGet(() -> 5);
        Assertions.assertEquals(5, value);
    }

    @Test
    void testOrElseGetWithSuccessTry() {
        Try<Integer> success = Try.success(10);
        int value = success.orElseGet(error -> 5);
        Assertions.assertEquals(10, value);
    }

    @Test
    void testOrElseGetWithFailureTry() {
        Try<Integer> failure = Try.failure(new RuntimeException("Error occurred"));
        int value = failure.orElseGet(error -> 5);
        Assertions.assertEquals(5, value);
    }

    @Test
    void testOrElseThrowWithSuccessTry() {
        Try<Integer> success = Try.success(10);
        int value = assertDoesNotThrow(() -> success.orElseThrow(Exception::new));
        Assertions.assertEquals(10, value);
    }

    @Test
    void testOrElseThrowWithFailureTry() {
        Try<Integer> failure = Try.failure(new RuntimeException("Error occurred"));
        Exception exception = Assertions.assertThrows(
                Exception.class,
                () -> failure.orElseThrow(e -> new Exception(e.getMessage()))
        );
        Assertions.assertEquals("Error occurred", exception.getMessage());
    }

    @Test
    void test_givenSuccess_whenOrElseThrowCalled_thenReturnValue() {
        Try<Integer> success = Try.success(10);
        int value = assertDoesNotThrow(() -> success.orElseThrow());
        Assertions.assertEquals(10, value);
    }

    @Test
    void test_givenFailure_whenOrElseThrowCalled_thenThrowException() {
        Try<Integer> failure = Try.failure(new RuntimeException("Error occurred"));
        Exception exception = Assertions.assertThrows(
                RuntimeException.class,
                failure::orElseThrow
        );
        Assertions.assertEquals("Error occurred", exception.getMessage());
    }

    @Test
    void test_givenSuccess_whenToOptionalCalled_thenReturnsOptionalWithSameValue() {
        Try<Integer> success = Try.success(10);

        Optional<Integer> optional = success.toOptional();
        Assertions.assertNotNull(optional);
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertSame(success.getData(), optional.get());
    }

    @Test
    void test_givenFailure_whenToOptionalCalled_thenReturnsEmptyOptional() {
        Try<Integer> failure = Try.failure(new RuntimeException("Error occurred"));

        Optional<Integer> optional = failure.toOptional();
        Assertions.assertNotNull(optional);
        Assertions.assertFalse(optional.isPresent());
    }

    @Test
    void test_givenNullMapper_whenMapIsCalled_thenExceptionIsThrown() {
        Try<Integer> success = Try.success(10);

        @SuppressWarnings("DataFlowIssue")
        NullPointerException nullPointerException = assertThrows(NullPointerException.class, () -> success.map(null));
        assertNotNull(nullPointerException);
    }

    @Test
    void test_givenSuccess_whenMapIsCalled_thenMappedSuccessTryIsReturned() {
        Try<Integer> success = Try.success(10);
        Try<String> result = success.map(Object::toString);

        assertTrue(result.isSuccess());
        assertEquals("10", result.getData());
    }

    @Test
    void test_givenFailure_whenMapIsCalled_thenSameFailureIsReturned() {
        Exception error = new RuntimeException("error");
        Try<Integer> failure = Try.failure(error);
        Try<String> result = failure.map(Object::toString);

        assertTrue(result.isFailure());
        assertSame(error, result.getError());
    }

    @Test
    void test_givenNullMapper_whenOtherwiseIsCalled_thenExceptionIsThrown() {
        Try<Integer> failure = Try.failure(new RuntimeException("error"));

        @SuppressWarnings("DataFlowIssue")
        NullPointerException nullPointerException = assertThrows(
                NullPointerException.class,
                () -> failure.otherwise(null)
        );
        assertNotNull(nullPointerException);
    }

    @Test
    void test_givenFailure_whenOtherwiseIsCalled_thenSuccessTryIsReturned() {
        Exception error = new RuntimeException("error");
        Try<String> failure = Try.failure(error);
        Try<String> result = failure.otherwise(e -> "recovered");

        assertTrue(result.isSuccess());
        assertEquals("recovered", result.getData());
    }

    @Test
    void test_givenSuccess_whenOtherwiseIsCalled_thenSameSuccessIsReturned() {
        Try<String> success = Try.success("data");
        Try<String> result = success.otherwise(e -> "ignored");

        assertTrue(result.isSuccess());
        assertSame(success, result);
        assertSame("data", result.getData());
    }

    @Test
    void test_givenNullMapper_whenFlatMapIsCalled_thenExceptionIsThrown() {
        Try<Integer> success = Try.success(10);

        @SuppressWarnings("DataFlowIssue")
        NullPointerException nullPointerException = assertThrows(
                NullPointerException.class,
                () -> success.flatMap(null)
        );
        assertNotNull(nullPointerException);
    }

    @Test
    void test_givenSuccess_whenFlatMapIsCalled_thenMappedSuccessTryIsReturned() {
        Try<Integer> success = Try.success(10);
        Try<String> result = success.flatMap(i -> Try.success(i.toString()));

        assertTrue(result.isSuccess());
        assertEquals("10", result.getData());
    }

    @Test
    void test_givenFailure_whenFlatMapIsCalled_thenSameFailureIsReturned() {
        Exception error = new RuntimeException("error");
        Try<Integer> failure = Try.failure(error);
        Try<String> result = failure.flatMap(i -> Try.success(i.toString()));

        assertTrue(result.isFailure());
        assertSame(error, result.getError());
    }

    @Test
    void test_givenNullMapper_whenRecoverIsCalled_thenExceptionIsThrown() {
        Try<Integer> failure = Try.failure(new RuntimeException("error"));

        @SuppressWarnings("DataFlowIssue")
        NullPointerException nullPointerException = assertThrows(
                NullPointerException.class,
                () -> failure.recover(null)
        );
        assertNotNull(nullPointerException);
    }

    @Test
    void test_givenFailure_whenRecoverIsCalled_thenRecoveredTryIsReturned() {
        Exception error = new RuntimeException("error");
        Try<String> failure = Try.failure(error);
        Try<String> result = failure.recover(e -> Try.success("recovered"));

        assertTrue(result.isSuccess());
        assertEquals("recovered", result.getData());
    }

    @Test
    void test_givenSuccess_whenRecoverIsCalled_thenSameSuccessIsReturned() {
        Try<String> success = Try.success("data");
        Try<String> result = success.recover(e -> Try.success("ignored"));

        assertTrue(result.isSuccess());
        assertEquals("data", result.getData());
    }

    @Test
    void test_givenNullMapper_whenTransformIsCalled_thenExceptionIsThrown() {
        Try<Integer> success = Try.success(10);

        @SuppressWarnings("DataFlowIssue")
        NullPointerException nullPointerException = assertThrows(
                NullPointerException.class,
                () -> success.transform(null)
        );
        assertNotNull(nullPointerException);
    }

    @Test
    void test_givenTry_whenTransformIsCalled_thenTransformedTryIsReturned() {
        Try<Integer> success = Try.success(10);
        Try<String> transformed = success.transform(t -> t.map(Object::toString));

        assertFalse(transformed.isFailure());
        assertEquals("10", transformed.getData());
    }

    @Test
    void test_givenNonNullConsumer_whenOnHandleIsCalled_thenConsumerIsExecuted() {
        Try<String> stringTry = Try.success("data");

        List<Try<String>> consumedTryList = new ArrayList<>();
        Try<String> returnedTry = stringTry.onHandle(consumedTryList::add);

        assertSame(stringTry, returnedTry);
        assertFalse(consumedTryList.isEmpty());
        assertEquals(1, consumedTryList.size());
        assertSame(stringTry, consumedTryList.get(0));
    }

    @Test
    void test_givenNullConsumer_whenOnHandleIsCalled_thenNoExceptionIsThrown() {
        Try<String> stringTry = Try.success("data");

        Try<String> returnedTry = assertDoesNotThrow(() -> stringTry.onHandle(null));
        assertSame(stringTry, returnedTry);
    }

    @Test
    void test_givenNullConsumer_whenOnSuccessIsCalled_thenDoesNotThrow() {
        Try<String> success = Try.success("data");

        Try<String> returnedTry = assertDoesNotThrow(() -> success.onSuccess(null));
        assertSame(success, returnedTry);
    }

    @Test
    void test_givenSuccess_whenOnSuccessIsCalled_thenConsumerIsExecuted() {
        Try<String> success = Try.success("data");
        StringBuilder consumed = new StringBuilder();
        Try<String> returnedTry = success.onSuccess(consumed::append);

        assertSame(success, returnedTry);
        assertEquals("data", consumed.toString());
    }

    @Test
    void test_givenNullConsumer_whenOnSuccessIsCalled_thenConsumerIsExecuted() {
        Exception error = new RuntimeException("error");
        Try<String> failure = Try.failure(error);

        Try<String> returnedTry = assertDoesNotThrow(() -> failure.onSuccess(null));
        assertSame(failure, returnedTry);
    }

    @Test
    void test_givenFailure_whenOnSuccessIsCalled_thenConsumerIsExecuted() {
        Exception error = new RuntimeException("error");
        Try<String> failure = Try.failure(error);
        StringBuilder consumed = new StringBuilder();
        Try<String> returnedTry = failure.onSuccess(consumed::append);

        assertSame(failure, returnedTry);
        assertEquals(0, consumed.length());
    }

    @Test
    void test_givenFailure_whenOnFailureIsCalled_thenConsumerIsExecuted() {
        Exception error = new RuntimeException("error");
        Try<String> failure = Try.failure(error);
        StringBuilder consumed = new StringBuilder();
        Try<String> returnedTry = failure.onFailure(e -> consumed.append(e.getMessage()));

        assertSame(failure, returnedTry);
        assertEquals("error", consumed.toString());
    }

    @Test
    void test_givenSuccess_whenOnFailureIsCalled_thenConsumerIsExecuted() {
        Try<String> success = Try.success("data");
        StringBuilder consumed = new StringBuilder();
        Try<String> returnedTry = success.onFailure(e -> consumed.append(e.getMessage()));

        assertSame(success, returnedTry);
        assertEquals(0, consumed.length());
    }

    @Test
    void test_givenSuccess_whenToResultIsCalled_thenResultIsReturned() {
        Try<String> success = Try.success("data");
        Result<String, Exception> result = success.toResult();

        assertTrue(result.isSuccess());
        assertEquals("data", result.getData());
    }

    @Test
    void test_givenFailure_whenToResultIsCalled_thenResultIsReturned() {
        Exception error = new RuntimeException("error");
        Try<String> failure = Try.failure(error);
        Result<String, Exception> result = failure.toResult();

        assertTrue(result.isFailure());
        assertSame(error, result.getError());
    }

    @MethodSource("provideHashcodeCoverageCases")
    @ParameterizedTest
    void test_givenTry_whenHashcodeIsInvoked_thenRespectiveOutcomeIsReturned(Try<?> tryObject, int expectedHashcode) {
        int actualHashcode = tryObject.hashCode();
        Assertions.assertEquals(expectedHashcode, actualHashcode);
    }

    @MethodSource("provideEqualityCases")
    @ParameterizedTest
    void test_givenTry_whenEqualsIsInvoked_thenRespectiveOutcomeIsReturned(
            Try<?> tryObject,
            Object object,
            boolean expectedIsEqual
    ) {
        boolean actualIsEquals = tryObject.equals(object);
        Assertions.assertEquals(expectedIsEqual, actualIsEquals);
    }

    @MethodSource("provideToStringCases")
    @ParameterizedTest
    void test_givenTry_whenToStringIsInvoked_thenRespectiveOutcomeIsReturned(
            Try<?> tryObject,
            String expectedToString
    ) {
        String actualToString = tryObject.toString();
        Assertions.assertEquals(expectedToString, actualToString);
    }
}
