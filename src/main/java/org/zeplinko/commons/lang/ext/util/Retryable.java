package org.zeplinko.commons.lang.ext.util;

import jakarta.annotation.Nonnull;
import lombok.Getter;
import org.zeplinko.commons.lang.ext.annotations.Preview;
import org.zeplinko.commons.lang.ext.core.AbstractOutcome;
import org.zeplinko.commons.lang.ext.core.Result;

import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.BiPredicate;

import static org.zeplinko.commons.lang.ext.util.Retryable.Outcome.TerminationReason.*;

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
 * <li>{@link #noRetryOnFailure(BiPredicate)} – declare <em>exceptions</em> and
 * <em>attempt numbers</em> that should not trigger a retry</li>
 * <li>{@link #maxRetries(int)} – maximum retry attempts</li>
 * <li>{@link #baseDelay(long, TimeUnit)} – base delay between attempts</li>
 * <li>{@link #backoff(BackoffStrategy)} – back-off strategy for calculating the
 * next delay</li>
 * </ul>
 * The task is started by calling {@link #run()} which returns an
 * {@link Outcome} extending {@link AbstractOutcome} describing the outcome.
 * Since {@code Retryable} implements {@link Callable}, it can also be used
 * directly with {@link java.util.concurrent.ExecutorService} and other
 * concurrency utilities.
 * <p>
 * <strong>Thread-safety:</strong> Instances of {@code Retryable} are
 * <em>not</em> designed for concurrent use. Configure the instance in a single
 * thread and either invoke {@link #run()} once or ensure that invocations
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
    private BiPredicate<? super R, Integer> resultPredicate = (result, currentAttempt) -> true;

    // Do not retry on exceptions by default
    private BiPredicate<? super Exception, Integer> exceptionPredicate = (exception, currentAttempt) -> false;

    // By default, there are no no-retry exceptions
    private BiPredicate<? super Exception, Integer> noRetryOnFailure = (exception, currentAttempt) -> false;

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
    public Retryable<R> retryUntilResult(@Nonnull BiPredicate<? super R, Integer> predicate) {
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
    public Retryable<R> retryOnFailure(@Nonnull BiPredicate<? super Exception, Integer> predicate) {
        this.exceptionPredicate = Objects.requireNonNull(predicate);
        return this;
    }

    /**
     * Defines which exceptions should not trigger a retry by supplying a
     * bi-predicate. When an exception matches this bi-predicate, the retryable will
     * immediately return a failure outcome without attempting any retries. The
     * bi-predicate receives both the exception and the current attempt number.
     * <p>
     * This is the opposite of {@link #retryOnFailure(BiPredicate)}. If an exception
     * matches both predicates, {@code noRetryOnFailure} takes precedence.
     * </p>
     * <p>
     * <strong>Note:</strong> If the bi-predicate itself throws an exception, that
     * exception will propagate immediately and will not trigger a retry. Such
     * exceptions typically indicate logic errors in the predicate implementation.
     * </p>
     *
     * @param predicate bi-predicate deciding whether an exception should not be
     *                  retried based on the exception and attempt number
     * @return this instance for chaining
     * @throws NullPointerException if {@code predicate} is null
     */
    public Retryable<R> noRetryOnFailure(@Nonnull BiPredicate<? super Exception, Integer> predicate) {
        this.noRetryOnFailure = Objects.requireNonNull(predicate);
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
     * Executes the configured task applying all retry rules and validation.
     *
     * @return a {@link Outcome} that captures success/failure, number of attempts
     *         and stop reason
     */
    public Outcome<R> run() {
        return call();
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
        Exception error = null;
        Outcome.TerminationReason reason;

        while (true) {
            attempt++;
            Exception taskException = null;
            R value = null;
            try {
                value = retryableTask.call();
            } catch (Exception e) {
                taskException = e;
            }

            if (taskException == null) {
                if (resultPredicate.test(value, attempt)) {
                    return Outcome.success(value, attempt);
                }
                // Result validation failed, will retry if max retries not exceeded
            } else {
                if (noRetryOnFailure.test(taskException, attempt))
                    return Outcome.failure(taskException, attempt, ABORTED_SKIP_RETRY_EXCEPTION);
                if (!exceptionPredicate.test(taskException, attempt))
                    return Outcome.failure(taskException, attempt, ABORTED_NON_RETRYABLE_EXCEPTION);
                error = taskException;
            }

            if (attempt > maxRetries) {
                reason = taskException == null ? RETRIES_EXHAUSTED_INVALID_RESULT
                        : RETRIES_EXHAUSTED_RETRYABLE_EXCEPTION;
                break;
            }

            long delay = backoffStrategy.nextDelay(baseDelayMillis, attempt);
            if (delay > 0) {
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    error = ie;
                    reason = INTERRUPTED;
                    break;
                }
            }
        }

        if (error == null) {
            error = new MaxRetriesExceededException("Validation failed after " + attempt + " attempts");
        }
        return Outcome.failure(error, attempt, reason);
    }

    /**
     * Strategy interface to calculate delay before each retry. Custom strategies
     * can implement this interface and be supplied via
     * {@link #backoff(BackoffStrategy)}.
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
     * Built-in backoff strategies for retry delays.
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

    private static final class MaxRetriesExceededException extends Exception {
        private MaxRetriesExceededException(String message) {
            super(message);
        }
    }

    /**
     * Immutable value object describing the outcome of a {@link Retryable#run()}
     * call. This class extends {@link AbstractOutcome} to provide common outcome
     * functionality with success/failure semantics.
     */
    @Getter
    public static final class Outcome<T> extends AbstractOutcome<T, Exception> {

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

        private final int attempts;

        private final TerminationReason reason;

        Outcome(int attempts, T data, Exception error, TerminationReason reason) {
            super(data, error);
            this.attempts = attempts;
            this.reason = reason;
        }

        static <T> Outcome<T> success(T data, int attempts) {
            return new Outcome<>(attempts, data, null, TerminationReason.SUCCEEDED);
        }

        static <T> Outcome<T> failure(Exception error, int attempts, TerminationReason reason) {
            return new Outcome<>(attempts, null, error, reason);
        }

        /**
         * Converts this outcome to a {@link Result}.
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

    }
}
