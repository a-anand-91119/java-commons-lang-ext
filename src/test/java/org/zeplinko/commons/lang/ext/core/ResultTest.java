package org.zeplinko.commons.lang.ext.core;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

class ResultTest {

    private static Stream<Arguments> provideHashcodeCoverageCases() {
        return Stream.of(
                Arguments.of(Result.success(2), Objects.hash(2, null)),
                Arguments.of(Result.failure(5), Objects.hash(null, 5))
        );
    }

    private static Stream<Arguments> provideEqualityCases() {
        Result<Integer, String> successResult = Result.success(5);
        Result<Integer, String> failureResult = Result.failure("abc");
        return Stream.of(
                Arguments.of(successResult, successResult, true),
                Arguments.of(successResult, Result.success(5), true),
                Arguments.of(successResult, Result.success(6), false),
                Arguments.of(successResult, Optional.of(6), false),
                Arguments.of(successResult, null, false),
                Arguments.of(failureResult, failureResult, true),
                Arguments.of(failureResult, Result.failure("abc"), true),
                Arguments.of(failureResult, Result.failure("def"), false),
                Arguments.of(failureResult, Optional.of("def"), false),
                Arguments.of(failureResult, null, false),
                Arguments.of(failureResult, successResult, false)
        );
    }

    private static Stream<Arguments> provideToStringCases() {
        return Stream.of(
                Arguments.of(Result.success(2), "Result.Success[2]"),
                Arguments.of(Result.success(null), "Result.Success[null]"),
                Arguments.of(Result.failure("abc"), "Result.Failure[abc]")
        );
    }

    @Test
    void testOkResultShouldBeSuccess() {
        Result<Integer, String> result = Result.ok(10);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertFalse(result.isFailure());
        Assertions.assertEquals(10, result.getData());
    }

    @Test
    void test_whenSuccessResultCreated_thenResultShouldBeSuccess() {
        Result<Integer, String> result = Result.success(10);
        Assertions.assertTrue(result.isSuccess());
        Assertions.assertFalse(result.isFailure());
        Assertions.assertEquals(10, result.getData());
    }

    @Test
    void testErrorResultShouldBeFailure() {
        Result<Integer, String> result = Result.error("Error occurred");
        Assertions.assertTrue(result.isFailure());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals("Error occurred", result.getError());
    }

    @Test
    void test_whenFailureResultCreate_thenShouldBeFailure() {
        Result<Integer, String> result = Result.failure("Error occurred");
        Assertions.assertTrue(result.isFailure());
        Assertions.assertFalse(result.isSuccess());
        Assertions.assertEquals("Error occurred", result.getError());
    }

    @Test
    void test_givenSuccessResult_whenOrElse_thenOriginalValueShouldBeReturned() {
        Result<Integer, String> result = Result.success(10);
        int value = result.orElse(5);
        Assertions.assertEquals(10, value);
    }

    @Test
    void test_givenFailureResult_whenOrElse_thenNewValueShouldBeReturned() {
        Result<Integer, String> result = Result.failure("Error occurred");
        int value = result.orElse(5);
        Assertions.assertEquals(5, value);
    }

    @Test
    void test_whenOrElseGetSupplierCalledOnSuccessResult_thenSupplierIsNotCalled() {
        Result<Integer, String> result = Result.success(10);
        int value = result.orElseGet(() -> 5);
        Assertions.assertEquals(10, value);
    }

    @Test
    void test_whenOrElseGetSupplierCalledOnFailureResult_thenSupplierIsNotCalled() {
        Result<Integer, String> result = Result.failure("Error occurred");
        int value = result.orElseGet(() -> 5);
        Assertions.assertEquals(5, value);
    }

    @Test
    void testOrElseGetWithSuccessResult() {
        Result<Integer, String> result = Result.success(10);
        int value = result.orElseGet(error -> 5);
        Assertions.assertEquals(10, value);
    }

    @Test
    void testOrElseGetWithFailureResult() {
        Result<Integer, String> result = Result.failure("Error occurred");
        int value = result.orElseGet(error -> 5);
        Assertions.assertEquals(5, value);
    }

    @Test
    void testOrElseThrowWithSuccessResult() throws Exception {
        Result<Integer, String> result = Result.success(10);
        int value = result.orElseThrow(Exception::new);
        Assertions.assertEquals(10, value);
    }

    @Test
    void testOrElseThrowWithFailureResult() {
        Result<Integer, String> result = Result.failure("Error occurred");
        Exception exception = Assertions.assertThrows(
                Exception.class,
                () -> result.orElseThrow(Exception::new)
        );
        Assertions.assertEquals("Error occurred", exception.getMessage());
    }

    @Test
    void test_whenSuccessResultIsMappedToValue_thenMappedResultIsReturned() {
        Result<Integer, String> result = Result.success(10);
        Result<Float, String> newResult = result.map(5.0f);

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isSuccess());
        Assertions.assertEquals(5.0f, newResult.getData());
    }

    @Test
    void test_whenFailureResultIsMappedToValue_thenMappingIsSkippedAndFailedResultIsReturned() {
        Result<Integer, String> result = Result.failure("Error occurred");
        Result<Float, String> newResult = result.map(5.0f);

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isFailure());
        Assertions.assertEquals("Error occurred", newResult.getError());
    }

    @Test
    void test_whenSuccessResultIsMapped_thenMappedResultIsReturned() {
        Result<Integer, String> result = Result.success(10);
        Result<Float, String> newResult = result.map(it -> (float) it * 2);

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isSuccess());
        Assertions.assertEquals(20.0f, newResult.getData());
    }

    @Test
    void test_whenFailureResultIsMapped_thenMappingIsSkippedAndFailedResultIsReturned() {
        Result<Integer, String> result = Result.failure("Error occurred");
        Result<Float, String> newResult = result.map(it -> (float) it * 2);

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isFailure());
        Assertions.assertEquals("Error occurred", newResult.getError());
    }

    @Test
    void test_whenOtherwiseValueCalledOnSuccessResult_thenOriginalValueIsReturned() {
        Result<Integer, String> result = Result.success(10);
        Result<Integer, String> newResult = result.otherwise(20);

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isSuccess());
        Assertions.assertEquals(10, newResult.getData());
    }

    @Test
    void test_whenOtherwiseValueCalledOnFailureResult_thenProvidedValueIsReturned() {
        Result<Integer, String> result = Result.failure("Error occurred");
        Result<Integer, String> newResult = result.otherwise(20);

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isSuccess());
        Assertions.assertEquals(20, newResult.getData());
    }

    @Test
    void test_whenOtherwiseCalledOnSuccessResult_thenOriginalValueIsReturned() {
        Result<Integer, String> result = Result.success(10);
        Result<Integer, String> newResult = result.otherwise(str -> 20);

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isSuccess());
        Assertions.assertEquals(10, newResult.getData());
    }

    @Test
    void test_whenOtherwiseCalledOnFailureResult_thenProvidedValueIsReturned() {
        Result<Integer, String> result = Result.failure("Error occurred");
        Result<Integer, String> newResult = result.otherwise(str -> 20);

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isSuccess());
        Assertions.assertEquals(20, newResult.getData());
    }

    @Test
    void test_givenSuccessResultMapper_whenComposeCalledOnSuccessResult_thenSuccessMapperIsCalled() {
        Result<Integer, String> result = Result.success(10);
        Result<Float, Double> newResult = result.compose(num -> Result.success(20.f), str -> Result.failure(50d));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isSuccess());
        Assertions.assertEquals(20.0f, newResult.getData());
    }

    @Test
    void test_givenFailureResultMapper_whenComposeCalledOnSuccessResult_thenSuccessMapperIsCalled() {
        Result<Integer, String> result = Result.success(10);
        Result<Float, Double> newResult = result.compose(num -> Result.failure(30d), str -> Result.success(20.f));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isFailure());
        Assertions.assertEquals(30d, newResult.getError());
    }

    @Test
    void test_givenSuccessResultMapper_whenComposeCalledOnFailureResult_thenFailureMapperIsCalled() {
        Result<Integer, String> result = Result.failure("Error occurred");
        Result<Float, Double> newResult = result.compose(num -> Result.failure(50d), str -> Result.success(20.f));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isSuccess());
        Assertions.assertEquals(20.0f, newResult.getData());
    }

    @Test
    void test_givenFailureResultMapper_whenComposeCalledOnFailureResult_thenFailureMapperIsCalled() {
        Result<Integer, String> result = Result.failure("Error occurred");
        Result<Float, Double> newResult = result.compose(num -> Result.success(20.f), str -> Result.failure(30d));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isFailure());
        Assertions.assertEquals(30d, newResult.getError());
    }

    @Test
    void test_givenSuccessMapper_whenFlatMapCalledOnSuccessResult_thenMapperIsInvoked() {
        Result<Integer, String> result = Result.success(10);
        Result<Float, String> newResult = result.flatMap(num -> Result.success(20.0f));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isSuccess());
        Assertions.assertEquals(20.0f, newResult.getData());
    }

    @Test
    void test_givenFailureMapper_whenFlatMapCalledOnSuccessResult_thenMapperIsInvoked() {
        Result<Integer, String> result = Result.success(10);
        Result<Float, String> newResult = result.flatMap(num -> Result.failure("Error occurred"));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isFailure());
        Assertions.assertEquals("Error occurred", newResult.getError());
    }

    @Test
    void test_givenSuccessMapper_whenFlatMapCalledOnFailureResult_thenMapperIsSkipped() {
        Result<Integer, String> result = Result.failure("Error occurred");
        Result<Float, String> newResult = result.flatMap(num -> Result.success(20.0f));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isFailure());
        Assertions.assertEquals("Error occurred", newResult.getError());
    }

    @Test
    void test_givenFailureMapper_whenFlatMapCalledOnFailureResult_thenMapperIsSkipped() {
        Result<Integer, String> result = Result.failure("Error occurred");
        Result<Float, String> newResult = result.flatMap(num -> Result.failure("Error Occurred Again"));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isFailure());
        Assertions.assertEquals("Error occurred", newResult.getError());
    }

    @Test
    void test_givenSuccessMapper_whenRecoverCalledOnSuccessResult_thenMapperIsSkipped() {
        Result<Integer, String> result = Result.success(10);
        Result<Integer, Double> newResult = result.recover(str -> Result.success(20));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isSuccess());
        Assertions.assertEquals(10, newResult.getData());
    }

    @Test
    void test_givenFailureMapper_whenRecoverCalledOnSuccessResult_thenMapperIsSkipped() {
        Result<Integer, String> result = Result.success(10);
        Result<Integer, Double> newResult = result.recover(str -> Result.failure(30d));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isSuccess());
        Assertions.assertEquals(10, newResult.getData());
    }

    @Test
    void test_givenSuccessMapper_whenRecoverCalledOnFailureResult_thenMapperIsInvoked() {
        Result<Integer, String> result = Result.failure("Error occurred");
        Result<Integer, Double> newResult = result.recover(str -> Result.success(20));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isSuccess());
        Assertions.assertEquals(20, newResult.getData());
    }

    @Test
    void test_givenFailureMapper_whenRecoverCalledOnFailureResult_thenMapperIsInvoked() {
        Result<Integer, String> result = Result.failure("Error occurred");
        Result<Integer, Double> newResult = result.recover(str -> Result.failure(50d));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isFailure());
        Assertions.assertEquals(50d, newResult.getError());
    }

    @Test
    void test_givenSuccessTransformer_whenTransformIsCalledOnSuccessResult_thenTransformedResultIsReturned() {
        Result<Integer, String> result = Result.success(10);
        Result<Float, Double> newResult = result.transform(r -> Result.success(20.f));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isSuccess());
        Assertions.assertEquals(20.0f, newResult.getData());
    }

    @Test
    void test_givenSuccessTransformer_whenTransformIsCalledOnFailureResult_thenTransformedResultIsReturned() {
        Result<Integer, String> result = Result.failure("Error occurred");
        Result<Float, Double> newResult = result.transform(r -> Result.success(20.f));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isSuccess());
        Assertions.assertEquals(20.0f, newResult.getData());
    }

    @Test
    void test_givenFailureTransformer_whenTransformIsCalledOnSuccessResult_thenTransformedResultIsReturned() {
        Result<Integer, String> result = Result.success(10);
        Result<Float, Double> newResult = result.transform(r -> Result.failure(50d));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isFailure());
        Assertions.assertEquals(50d, newResult.getError());
    }

    @Test
    void test_givenFailureTransformer_whenTransformIsCalledOnFailureResult_thenTransformedResultIsReturned() {
        Result<Integer, String> result = Result.failure("Error occurred");
        Result<Float, Double> newResult = result.transform(r -> Result.failure(50d));

        Assertions.assertNotNull(newResult);
        Assertions.assertTrue(newResult.isFailure());
        Assertions.assertEquals(50d, newResult.getError());
    }

    @Test
    void test_whenHandleCalledOnSuccessResult_thenSuccessHandlerIsInvoked() {
        boolean[] successHandlerCalled = { false };
        boolean[] failureHandlerCalled = { false };
        Result<Integer, String> result = Result.success(10);

        Result<Integer, String> returnedResult = result
                .handle(num -> successHandlerCalled[0] = true, str -> failureHandlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertTrue(successHandlerCalled[0]);
        Assertions.assertFalse(failureHandlerCalled[0]);
    }

    @Test
    void test_whenOnResultCalledOnSuccessResult_thenSuccessHandlerIsInvoked() {
        boolean[] successHandlerCalled = { false };
        boolean[] failureHandlerCalled = { false };
        Result<Integer, String> result = Result.success(10);

        Result<Integer, String> returnedResult = result
                .onResult(num -> successHandlerCalled[0] = true, str -> failureHandlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertTrue(successHandlerCalled[0]);
        Assertions.assertFalse(failureHandlerCalled[0]);
    }

    @Test
    void test_whenHandleCalledOnFailedResult_thenFailureHandlerIsInvoked() {
        boolean[] successHandlerCalled = { false };
        boolean[] failureHandlerCalled = { false };
        Result<Integer, String> result = Result.failure("Error occurred");

        Result<Integer, String> returnedResult = result
                .handle(num -> successHandlerCalled[0] = true, str -> failureHandlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertFalse(successHandlerCalled[0]);
        Assertions.assertTrue(failureHandlerCalled[0]);
    }

    @Test
    void test_whenOnResultCalledOnFailedResult_thenFailureHandlerIsInvoked() {
        boolean[] successHandlerCalled = { false };
        boolean[] failureHandlerCalled = { false };
        Result<Integer, String> result = Result.failure("Error occurred");

        Result<Integer, String> returnedResult = result
                .onResult(num -> successHandlerCalled[0] = true, str -> failureHandlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertFalse(successHandlerCalled[0]);
        Assertions.assertTrue(failureHandlerCalled[0]);
    }

    @Test
    void test_whenHandleResultCalledOnSuccessResult_thenHandlerIsInvoked() {
        boolean[] handlerCalled = { false };
        Result<Integer, String> result = Result.success(10);

        Result<Integer, String> returnedResult = result.handleResult(r -> handlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertTrue(handlerCalled[0]);
    }

    @Test
    void test_whenOnResultCalledOnSuccessResult_thenHandlerIsInvoked() {
        boolean[] handlerCalled = { false };
        Result<Integer, String> result = Result.success(10);

        Result<Integer, String> returnedResult = result.onResult(r -> handlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertTrue(handlerCalled[0]);
    }

    @Test
    void test_whenHandleResultCalledOnFailedResult_thenHandlerIsInvoked() {
        boolean[] handlerCalled = { false };
        Result<Integer, String> result = Result.failure("Error occurred");

        Result<Integer, String> returnedResult = result.handleResult(r -> handlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertTrue(handlerCalled[0]);
    }

    @Test
    void test_whenOnResultCalledOnFailedResult_thenHandlerIsInvoked() {
        boolean[] handlerCalled = { false };
        Result<Integer, String> result = Result.failure("Error occurred");

        Result<Integer, String> returnedResult = result.onResult(r -> handlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertTrue(handlerCalled[0]);
    }

    @Test
    void test_whenHandleSuccessCalledOnSuccessResult_thenHandlerIsInvoked() {
        boolean[] handlerCalled = { false };
        Result<Integer, String> result = Result.success(10);

        Result<Integer, String> returnedResult = result.handleSuccess(r -> handlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertTrue(handlerCalled[0]);
    }

    @Test
    void test_whenOnSuccessCalledOnSuccessResult_thenHandlerIsInvoked() {
        boolean[] handlerCalled = { false };
        Result<Integer, String> result = Result.success(10);

        Result<Integer, String> returnedResult = result.onSuccess(r -> handlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertTrue(handlerCalled[0]);
    }

    @Test
    void test_whenHandleSuccessCalledOnFailedResult_thenHandlerIsSkipped() {
        boolean[] handlerCalled = { false };
        Result<Integer, String> result = Result.failure("Error occurred");

        Result<Integer, String> returnedResult = result.handleSuccess(r -> handlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertFalse(handlerCalled[0]);
    }

    @Test
    void test_whenOnSuccessCalledOnFailedResult_thenHandlerIsSkipped() {
        boolean[] handlerCalled = { false };
        Result<Integer, String> result = Result.failure("Error occurred");

        Result<Integer, String> returnedResult = result.onSuccess(r -> handlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertFalse(handlerCalled[0]);
    }

    @Test
    void test_whenHandleFailureCalledOnSuccessResult_thenHandlerIsSkipped() {
        boolean[] handlerCalled = { false };
        Result<Integer, String> result = Result.success(10);

        Result<Integer, String> returnedResult = result.handleFailure(r -> handlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertFalse(handlerCalled[0]);
    }

    @Test
    void test_whenOnFailureCalledOnSuccessResult_thenHandlerIsSkipped() {
        boolean[] handlerCalled = { false };
        Result<Integer, String> result = Result.success(10);

        Result<Integer, String> returnedResult = result.onFailure(r -> handlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertFalse(handlerCalled[0]);
    }

    @Test
    void test_whenHandleFailureCalledOnFailedResult_thenHandlerIsInvoked() {
        boolean[] handlerCalled = { false };
        Result<Integer, String> result = Result.failure("Error occurred");

        Result<Integer, String> returnedResult = result.handleFailure(r -> handlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertTrue(handlerCalled[0]);
    }

    @Test
    void test_whenOnFailureCalledOnFailedResult_thenHandlerIsInvoked() {
        boolean[] handlerCalled = { false };
        Result<Integer, String> result = Result.failure("Error occurred");

        Result<Integer, String> returnedResult = result.onFailure(r -> handlerCalled[0] = true);

        Assertions.assertNotNull(returnedResult);
        Assertions.assertSame(result, returnedResult);
        Assertions.assertTrue(handlerCalled[0]);
    }

    @Test
    void test_givenSuccess_whenToOptionalCalled_thenReturnsOptionalWithSameValue() {
        Result<Integer, String> success = Result.success(10);

        Optional<Integer> optional = success.toOptional();
        Assertions.assertNotNull(optional);
        Assertions.assertTrue(optional.isPresent());
        Assertions.assertSame(success.getData(), optional.get());
    }

    @Test
    void test_givenFailure_whenToOptionalCalled_thenReturnsEmptyOptional() {
        Result<Integer, String> failure = Result.failure("error");

        Optional<Integer> optional = failure.toOptional();
        Assertions.assertNotNull(optional);
        Assertions.assertFalse(optional.isPresent());
    }

    @MethodSource("provideHashcodeCoverageCases")
    @ParameterizedTest
    void test_givenResult_whenHashcodeIsInvoked_thenRespectiveOutcomeIsReturned(
            Result<?, ?> result,
            int expectedHashcode
    ) {
        int actualHashcode = result.hashCode();
        Assertions.assertEquals(expectedHashcode, actualHashcode);
    }

    @MethodSource("provideEqualityCases")
    @ParameterizedTest
    void test_givenResult_whenEqualsIsInvoked_thenRespectiveOutcomeIsReturned(
            Result<?, ?> result,
            Object object,
            boolean expectedIsEqual
    ) {
        boolean actualIsEquals = result.equals(object);
        Assertions.assertEquals(expectedIsEqual, actualIsEquals);
    }

    @MethodSource("provideToStringCases")
    @ParameterizedTest
    void test_givenResult_whenToStringIsInvoked_thenRespectiveOutcomeIsReturned(
            Result<?, ?> result,
            String expectedToString
    ) {
        String actualToString = result.toString();
        Assertions.assertEquals(expectedToString, actualToString);
    }
}
