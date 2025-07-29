package org.zeplinko.commons.lang.ext.util.resilience;

import jakarta.annotation.Nonnull;
import org.zeplinko.commons.lang.ext.annotations.Preview;
import org.zeplinko.commons.lang.ext.core.AbstractOutcome;
import org.zeplinko.commons.lang.ext.core.Nullable;
import org.zeplinko.commons.lang.ext.core.Result;
import org.zeplinko.commons.lang.ext.core.Try;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

import static org.zeplinko.commons.lang.ext.util.resilience.Retryable.Outcome.TerminationReason.*;

/**
 * Executes a {@link Callable} with retry semantics.
 * <p>
 * A {@code Retryable} encapsulates a task that may be re-executed in the event
 * of failure or validation rejection. Clients configure the behavior fluently
 * using:
 * </p>
 * <ul>
 * <li>{@link #retryUntilResult(BiPredicate)} – validation bi-predicate that
 * must hold for the <em>result</em> and <em>attempt number</em> to be
 * accepted</li>
 * <li>{@link #retryOnFailure(BiPredicate)} – declare <em>exceptions</em> and
 * <em>attempt numbers</em> that trigger a retry</li>
 * <li>{@link #maxRetries(int)} – maximum retry attempts</li>
 * <li>{@link #baseDelay(long, TimeUnit)} – base delay between attempts</li>
 * <li>{@link #backoff(BackoffStrategy)} – back-off strategy for calculating the
 * next delay</li>
 * </ul>
 * The task is started by calling {@link #call()} which returns an
 * {@link Outcome} extending {@link AbstractOutcome} describing the outcome.
 * Since {@code Retryable} implements {@link Callable}, it can also be used
 * directly with {@link java.util.concurrent.ExecutorService} and other
 * concurrency utilities.
 * <p>
 * <strong>Thread-safety:</strong> Instances of {@code Retryable} are
 * <em>not</em> designed for concurrent use. Configure the instance in a single
 * thread and either invoke {@link #call()} once or ensure that invocations
 * happen from the same thread. For parallel work create a separate
 * {@code Retryable} per thread.
 * </p>
 *
 * @param <R> the type of the successful result returned by the task
 * @author A&nbsp;Anand
 */
@Preview
public final class Retryable<R> implements Callable<Retryable.Outcome<R>> {

    private final Callable<R> retryableTask;

    // Validation predicate:accept any result by default
    private BiPredicate<? super RetryOnResultContext, ? super R> resultPredicate = (context, result) -> true;

    // Do not retry on exceptions by default
    private BiPredicate<? super RetryOnErrorContext, ? super Exception> exceptionPredicate = (
            context,
            exception
    ) -> false;

    private int maxRetries = 0;

    private long baseDelayMillis = 0;

    private BackoffStrategy backoffStrategy = Backoff.FIXED;

    private Retryable(Callable<R> retryableTask) {
        this.retryableTask = Objects.requireNonNull(retryableTask);
    }

    /**
     * Creates a new {@code Retryable} for the supplied task.
     *
     * @param task the {@link Callable} to execute
     * @param <D>  the result type produced by the task
     * @return a new {@code Retryable}
     * @throws NullPointerException if {@code task} is null
     */
    public static <D> Retryable<D> of(@Nonnull Callable<D> task) {
        return new Retryable<>(task);
    }

    /**
     * Sets the validation bi-predicate that must evaluate to {@code true} for the
     * result to be considered successful. The bi-predicate receives both the result
     * and the current attempt number. If the predicate returns {@code false} the
     * task will be retried (subject to {@link #maxRetries(int)} and delay/back-off
     * settings).
     * <p>
     * <strong>Note:</strong> If the bi-predicate itself throws an exception, that
     * exception will propagate immediately and will not trigger a retry. Such
     * exceptions typically indicate logic errors in the predicate implementation.
     * </p>
     *
     * @param predicate validation bi-predicate that accepts the result and attempt
     *                  number (must not be {@code null})
     * @return this instance for chaining
     * @throws NullPointerException if {@code predicate} is null
     */
    public Retryable<R> retryUntilResult(@Nonnull BiPredicate<? super RetryOnResultContext, ? super R> predicate) {
        this.resultPredicate = Objects.requireNonNull(predicate);
        return this;
    }

    /**
     * Defines which exceptions qualify for a retry by supplying a bi-predicate. The
     * bi-predicate receives both the exception and the current attempt number.
     * <p>
     * <strong>Note:</strong> If the bi-predicate itself throws an exception, that
     * exception will propagate immediately and will not trigger a retry. Such
     * exceptions typically indicate logic errors in the predicate implementation.
     * </p>
     *
     * @param predicate bi-predicate deciding whether an exception is retryable
     *                  based on the exception and attempt number
     * @return this instance for chaining
     * @throws NullPointerException if {@code predicate} is null
     */
    public Retryable<R> retryOnFailure(@Nonnull BiPredicate<? super RetryOnErrorContext, ? super Exception> predicate) {
        this.exceptionPredicate = Objects.requireNonNull(predicate);
        return this;
    }

    /**
     * Sets how many times the operation should be retried after the initial
     * attempt. A negative value is normalized to {@code 0}. No hard upper bound is
     * enforced—use care when supplying very large numbers as they may lead to
     * long-running loops.
     *
     * @param maxRetries number of retries to permit (negative values are normalized
     *                   to 0)
     * @return this instance for fluent chaining
     */
    public Retryable<R> maxRetries(int maxRetries) {
        this.maxRetries = Math.max(0, maxRetries);
        return this;
    }

    /**
     * Sets the base delay applied before scheduling the next attempt.
     * <p>
     * Any {@link TimeUnit} can be supplied. The value is internally converted to
     * milliseconds. No upper bound is enforced, so passing large values (for
     * instance {@code HOURS} or {@code DAYS}) will produce equally large sleep
     * times. Callers are responsible for choosing sensible delays.
     * </p>
     *
     * @param duration amount of time to wait between attempts
     * @param unit     unit for the duration; any unit
     * @return this instance for method chaining
     * @throws IllegalArgumentException if {@code duration} is negative
     * @throws NullPointerException     if {@code unit} is null
     */
    public Retryable<R> baseDelay(long duration, @Nonnull TimeUnit unit) {
        Objects.requireNonNull(unit);
        if (duration < 0)
            throw new IllegalArgumentException("Duration must be positive");

        this.baseDelayMillis = unit.toMillis(duration);
        return this;
    }

    /**
     * Selects the back-off strategy to compute the delay before the next retry.
     *
     * @param strategy the {@link BackoffStrategy} to apply
     * @return this instance for method chaining
     * @throws NullPointerException if {@code strategy} is null
     */
    public Retryable<R> backoff(@Nonnull BackoffStrategy strategy) {
        this.backoffStrategy = Objects.requireNonNull(strategy);
        return this;
    }

    /**
     * Executes the configured task applying all retry rules and validation. This
     * method implements the {@link Callable} interface, allowing {@code Retryable}
     * instances to be used with {@link java.util.concurrent.ExecutorService} and
     * other concurrency utilities.
     *
     * @return a {@link Outcome} that captures success/failure, number of attempts
     *         and stop reason
     */
    @Override
    public Outcome<R> call() {
        int attempt = 0;

        while (true) {
            attempt++;
            Try<R> currentAttemptTry = Try.to(retryableTask);

            Nullable<Outcome<R>> outcomeNullable = doPredicatesHaltAttempt(currentAttemptTry, attempt);
            if (outcomeNullable.isNotNull()) {
                return outcomeNullable.orElseThrow();
            }

            if (attempt > maxRetries) {
                return getOutcomeOnRetryExhaustion(currentAttemptTry, attempt);
            }

            long delay = backoffStrategy.nextDelay(baseDelayMillis, attempt);
            if (delay > 0) {
                try {
                    // noinspection BusyWait
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return Outcome.failureDueToThreadInterruptedException(ie, attempt);
                }
            }
        }
    }

    private Nullable<Outcome<R>> doPredicatesHaltAttempt(Try<R> currentAttemptTry, int attempt) {
        if (currentAttemptTry.isSuccess()) {
            if (resultPredicate.test(ContextImpl.of(attempt), currentAttemptTry.getData())) {
                return Nullable.of(Outcome.success(currentAttemptTry.getData(), attempt));
            }
        } else {
            if (!exceptionPredicate.test(ContextImpl.of(attempt), currentAttemptTry.getError()))
                return Nullable.of(Outcome.failureDueToNonRetryableException(currentAttemptTry.getError(), attempt));
        }
        return Nullable.empty();
    }

    private static <R> Outcome<R> getOutcomeOnRetryExhaustion(Try<R> currentAttemptTry, int attempt) {
        if (currentAttemptTry.isSuccess()) {
            return Outcome.failureDueToRetryExhaustion(currentAttemptTry.getData(), attempt);
        }
        return Outcome.failureDueToRetryExhaustion(currentAttemptTry.getError(), attempt);
    }

    /**
     * Built-in backoff strategies for retry delays.
     *
     * @author A&nbsp;Anand
     */
    public enum Backoff implements BackoffStrategy {
        /**
         * Fixed delay strategy that returns the same base delay for each attempt.
         */
        FIXED {
            @Override
            public long nextDelay(long base, int attempt) {
                return base;
            }
        },
        /**
         * Linear backoff strategy that multiplies the base delay by the attempt number.
         * For example, base, base*2, base*3, base*4, etc.
         */
        LINEAR {
            @Override
            public long nextDelay(long base, int attempt) {
                return base * attempt;
            }
        },
        /**
         * Exponential backoff strategy that doubles the delay with each attempt. For
         * example, base, base*2, base*4, base*8, etc. Overflow protection ensures the
         * delay never exceeds {@link Long#MAX_VALUE}.
         */
        EXPONENTIAL {
            @Override
            public long nextDelay(long base, int attempt) {
                if (attempt <= 0 || base <= 0)
                    return 0;

                int shift = attempt - 1;
                if (shift >= Long.SIZE - 1)
                    return Long.MAX_VALUE;

                long multiplier = 1L << shift;
                if (base > Long.MAX_VALUE / multiplier)
                    return Long.MAX_VALUE;
                return base * multiplier;
            }
        }
    }

    /**
     * Strategy interface to calculate delay before each retry. Custom strategies
     * can implement this interface and be supplied via
     * {@link #backoff(BackoffStrategy)}.
     *
     * @author A&nbsp;Anand
     */
    public interface BackoffStrategy {
        /**
         * Calculates the delay before the next retry attempt.
         *
         * @param base    the base delay in milliseconds
         * @param attempt the current attempt number (1-based)
         * @return the delay in milliseconds before the next attempt
         */
        long nextDelay(long base, int attempt);
    }

    /**
     * Context provided to result-based retry predicate
     *
     * @author Shivam&nbsp;Nagpal
     */
    public interface RetryOnResultContext {

        /**
         * Returns the current attempt number (1-based).
         *
         * @return the current attempt count, starting at {@code 1}
         */
        int getAttempt();
    }

    /**
     * Context provided to error-based retry predicates or callbacks.
     *
     * @author Shivam&nbsp;Nagpal
     */
    public interface RetryOnErrorContext {

        /**
         * Returns the current attempt number (1-based).
         *
         * @return the current attempt count, starting at {@code 1}
         */
        int getAttempt();
    }

    /**
     * Exception used to signal that the maximum number of retry attempts has been
     * exceeded due to result validation failures.
     *
     * <p>
     * This exception type is typically attached to a failed {@code Outcome} when
     * none of the produced results passed the result validator.
     * </p>
     *
     * @author A&nbsp;Anand
     */
    public static final class MaxRetriesExceededException extends Exception {

        /**
         * Creates the exception with a detail message.
         *
         * @param message detail message describing the exhaustion condition
         */
        public MaxRetriesExceededException(String message) {
            super(message);
        }
    }

    private static class ContextImpl implements RetryOnResultContext, RetryOnErrorContext {
        private final int attempt;

        private ContextImpl(int attempt) {
            this.attempt = attempt;
        }

        public static ContextImpl of(int attempt) {
            return new ContextImpl(attempt);
        }

        @Override
        public int getAttempt() {
            return attempt;
        }
    }

    /**
     * Immutable value object describing the outcome of a {@link Retryable#call()}
     * call. This class extends {@link AbstractOutcome} to provide common outcome
     * functionality with success/failure semantics. It is immutable and therefore
     * thread-safe.
     *
     * @param <T> the type of the successful result value
     * @author A&nbsp;Anand
     * @see AbstractOutcome
     * @see Retryable
     * @see Outcome.TerminationReason
     */
    public static class Outcome<T> extends AbstractOutcome<T, Exception> {

        private final int attempts;

        private final T invalidData;

        private final TerminationReason reason;

        private Outcome(int attempts, T data, Exception error, T invalidData, TerminationReason reason) {
            super(data, error);
            this.attempts = attempts;
            this.invalidData = invalidData;
            this.reason = reason;
        }

        /**
         * Returns a successful outcome.
         *
         * @param data     the successful result; may be {@code null} if the retryable
         *                 task legitimately returns {@code null}
         * @param attempts total number of attempts performed (including the successful
         *                 attempt)
         * @param <T>      the result type
         * @return a successful {@code Outcome}
         * @see TerminationReason#SUCCEEDED
         */
        public static <T> Outcome<T> success(T data, int attempts) {
            return new Outcome<>(attempts, data, null, null, TerminationReason.SUCCEEDED);
        }

        /**
         * Returns a failed outcome that was aborted because a non-retryable exception
         * occurred.
         *
         * @param error    the non-retryable exception that caused the abort; must not
         *                 be {@code null}
         * @param attempts total number of attempts performed up to the abort
         * @param <T>      the result type
         * @return a failed {@code Outcome}
         * @see TerminationReason#ABORTED_NON_RETRYABLE_EXCEPTION
         */
        public static <T> Outcome<T> failureDueToNonRetryableException(Exception error, int attempts) {
            return new Outcome<>(attempts, null, error, null, ABORTED_NON_RETRYABLE_EXCEPTION);
        }

        /**
         * Returns a failed outcome indicating that retries were exhausted because
         * result validation failed on each attempt.
         *
         * <p>
         * The terminal {@link Exception} stored in this outcome is a
         * {@link MaxRetriesExceededException} describing that the maximum number of
         * attempts was reached. The {@code invalidData} captures the last result
         * produced by the task that did not pass validation.
         *
         * @param invalidData the last invalid result produced; may be {@code null} if
         *                    not applicable
         * @param attempts    total number of attempts performed (typically the
         *                    configured maximum)
         * @param <T>         the result type
         * @return a failed {@code Outcome}
         * @see TerminationReason#RETRIES_EXHAUSTED_INVALID_RESULT
         * @see MaxRetriesExceededException
         */
        public static <T> Outcome<T> failureDueToRetryExhaustion(T invalidData, int attempts) {
            MaxRetriesExceededException error = new MaxRetriesExceededException(
                    "Result data validation failed after " + attempts + " attempts"
            );
            return new Outcome<>(attempts, null, error, invalidData, RETRIES_EXHAUSTED_INVALID_RESULT);
        }

        /**
         * Returns a failed outcome indicating that retries were exhausted due to
         * repeated retryable exceptions.
         *
         * @param error    the last retryable exception observed; must not be
         *                 {@code null}
         * @param attempts total number of attempts performed (typically the configured
         *                 maximum)
         * @param <T>      the result type
         * @return a failed {@code Outcome}
         * @see TerminationReason#RETRIES_EXHAUSTED_RETRYABLE_EXCEPTION
         */
        public static <T> Outcome<T> failureDueToRetryExhaustion(Exception error, int attempts) {
            return new Outcome<>(attempts, null, error, null, RETRIES_EXHAUSTED_RETRYABLE_EXCEPTION);
        }

        /**
         * Returns a failed outcome indicating that execution was interrupted while
         * waiting between attempts or during the call.
         *
         * @param error    the {@link InterruptedException} that interrupted execution;
         *                 must not be {@code null}
         * @param attempts total number of attempts performed up to the interruption
         * @param <T>      the result type
         * @return a failed {@code Outcome}
         * @see TerminationReason#INTERRUPTED
         */
        public static <T> Outcome<T> failureDueToThreadInterruptedException(InterruptedException error, int attempts) {
            return new Outcome<>(attempts, null, error, null, INTERRUPTED);
        }

        /**
         * Converts this outcome to a {@link Result} with the same success/failure
         * semantics.
         *
         * @return a {@link Result} representing the success or failure of the retry
         *         operation
         */
        public Result<T, Exception> toResult() {
            return isFailure() ? Result.failure(getError()) : Result.success(getData());
        }

        @Override
        public boolean isFailure() {
            return !Objects.equals(SUCCEEDED, this.reason);
        }

        /**
         * Returns the total number of attempts performed for this execution.
         * <p>
         * The count includes the first attempt and, if successful, the final successful
         * attempt.
         *
         * @return the number of attempts performed (never negative)
         */
        public int getAttempts() {
            return this.attempts;
        }

        /**
         * Returns the high-level reason describing why retry execution finished.
         *
         * @return the termination reason (never {@code null})
         */
        public TerminationReason getReason() {
            return this.reason;
        }

        /**
         * Returns the last result produced that failed validation when retries were
         * exhausted due to invalid results.
         *
         * @return the last invalid result, or {@code null} if not applicable
         * @see TerminationReason#RETRIES_EXHAUSTED_INVALID_RESULT
         */
        public T getInvalidData() {
            return this.invalidData;
        }

        /**
         * Enumeration of possible termination reasons for retry execution.
         */
        public enum TerminationReason {
            /**
             * The task succeeded and result validation passed.
             */
            SUCCEEDED,
            /**
             * Maximum retries were exceeded due to result validation failures.
             */
            RETRIES_EXHAUSTED_INVALID_RESULT,
            /**
             * Maximum retries were exceeded due to retryable exceptions.
             */
            RETRIES_EXHAUSTED_RETRYABLE_EXCEPTION,
            /**
             * Execution was aborted due to a non-retryable exception.
             */
            ABORTED_NON_RETRYABLE_EXCEPTION,
            /**
             * Execution was aborted due to an exception matching the no-retry predicate.
             */
            ABORTED_SKIP_RETRY_EXCEPTION,
            /**
             * Execution was interrupted while waiting between attempts.
             */
            INTERRUPTED
        }
    }
}
