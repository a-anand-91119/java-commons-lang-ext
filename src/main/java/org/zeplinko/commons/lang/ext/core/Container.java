package org.zeplinko.commons.lang.ext.core;

import java.util.Objects;

/**
 * A simple container class that holds a single immutable value.
 *
 * @author Astha&nbsp;Singh
 *
 * @param <T> The type of the value stored in the container.
 */
@SuppressWarnings("LombokGetterMayBeUsed")
public class Container<T> {

    /**
     * The stored value of type {@code T}.
     */
    protected T value;

    /**
     * Constructs a new {@code Container} with the specified value.
     *
     * @deprecated Instead use {@link Container#of(Object)}
     *
     * @param value The value to be stored in the container. Can be {@code null}.
     */
    @Deprecated
    public Container(T value) {
        this.value = value;
    }

    /**
     * Retrieves the value stored in this container.
     *
     * @return The stored value of type {@code T}, or {@code null} if no value is
     *         present.
     */
    public T getValue() {
        return this.value;
    }

    /**
     * Static factory method to create a {@code Container} instance.
     *
     * @param value The value to be stored in the container.
     * @param <T>   The type of the value.
     * @return A new {@code Container} containing the given value.
     */
    public static <T> Container<T> of(T value) {
        return new Container<>(value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return Objects.hash(getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Container)) {
            return false;
        }
        Container<?> other = (Container<?>) o;
        return Objects.equals(this.getValue(), other.getValue());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return "Container[" + getValue() + "]";
    }
}
