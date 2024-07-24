package org.zeplinko.commons.lang.ext.core;

import lombok.Getter;

import java.util.concurrent.Callable;
import java.util.function.Consumer;

public class Try {

    public static <T> Try to(Callable<T> callable) {
        try {
            return new Success<>(callable.call());
        } catch (Throwable throwable) {
            return new Failure(throwable);
        }
    }

    public boolean isSuccess() {
        return this instanceof Success;
    }

    public boolean isFailure() {
        return this instanceof Failure;
    }

    public void handleError(Consumer<Throwable> errorHandler) {
        if (this instanceof Failure) {
            errorHandler.accept(((Failure) this).getThrowable());
        }
    }

    @Getter
    static final class Success<T> extends Try {
        private final T value;

        public Success(T value) {
            this.value = value;
        }
    }

    @Getter
    static final class Failure extends Try {
        private final Throwable throwable;

        public Failure(Throwable throwable) {
            this.throwable = throwable;
        }
    }
}
