package org.zeplinko.commons.lang.ext.util.function;

import jakarta.annotation.Nonnull;
import org.zeplinko.commons.lang.ext.annotations.Preview;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Kotlin-style <em>scope functions</em> for Java objects.
 *
 * <p>
 * All helpers are <strong>static</strong> and can be imported like this:
 * </p>
 *
 * <pre>
 * {@code
 * import static org.zeplinko.commons.lang.ext.util.function.Scope.*;
 *
 * Person p = mutate(new Person(), it -> it.setName("Alice"));
 *
 * User user = transform(new Person(), p -> {
 *     p.setUserType("Basic");
 *     return User.fromPerson(p);
 * });
 *
 * RoleSet roleSet = execute(() -> {
 *     Role basic = getBasicRole();
 *     Role premium = getPremiumRole();
 *     return RoleSet.from(basic, premium);
 * });
 * }
 * </pre>
 *
 * <p>
 * The class is <strong>final</strong> and has a private constructor. Only the
 * static methods are intended to be used.
 * </p>
 *
 * @author Shivam&nbsp;Nagpal
 */
public final class Scope {
    /** Not instantiable. */
    private Scope() {
    }

    /**
     * Executes the given side effect on {@code value} and then returns that same
     * {@code value}. Useful for fluent "tap"/"apply" style chains.
     *
     * <pre>
     * {@code
     * import static org.zeplinko.commons.lang.ext.util.function.Scope.*;
     *
     * Person p = mutate(new Person(), it -> it.setName("Alice"));
     * }
     * </pre>
     *
     * @param value   the object to be mutated; may be {@code null}, the caller must
     *                take care of the null checks
     * @param mutator the side effect to run (must not be {@code null})
     * @param <T>     the type of the receiver
     * @return the original {@code value} after {@code mutator.accept(value)} has
     *         executed
     * @throws NullPointerException if {@code mutator} is {@code null}
     */
    @Preview
    public static <T> T mutate(T value, @Nonnull Consumer<? super T> mutator) {
        Objects.requireNonNull(mutator, "mutator cannot be null");
        mutator.accept(value);
        return value;
    }

    /**
     * Transforms {@code value} with the supplied {@code transformer} function and
     * returns the function’s result.
     *
     *
     * <pre>
     * {@code
     * import static org.zeplinko.commons.lang.ext.util.function.Scope.*;
     *
     * User user = transform(new Person(), p -> {
     *     p.setUserType("Basic");
     *     return User.fromPerson(p);
     * });
     * }
     * </pre>
     *
     * @param value       the input to transform; may be {@code null}, the caller
     *                    must take care of the null checks
     * @param transformer the mapping function (must not be {@code null})
     * @param <T>         input type
     * @param <R>         result type
     * @return whatever {@code transformer.apply(value)} returns
     * @throws NullPointerException if {@code transformer} is {@code null}
     */
    @Preview
    public static <T, R> R transform(T value, @Nonnull Function<? super T, ? extends R> transformer) {
        Objects.requireNonNull(transformer, "transformer cannot be null");
        return transformer.apply(value);
    }

    /**
     * Executes the given {@code executable} and returns its result. Provides a
     * concise “scoped block” for expressions that don’t depend on an external
     * receiver.
     *
     *
     * <pre>
     * {@code
     * import static org.zeplinko.commons.lang.ext.util.function.Scope.*;
     *
     * RoleSet roleSet = execute(() -> {
     *     Role basic = getBasicRole();
     *     Role premium = getPremiumRole();
     *     return RoleSet.from(basic, premium);
     * });
     * }
     * </pre>
     *
     * @param executable a supplier to call (must not be {@code null})
     * @param <R>        result type
     * @return the value returned by {@code executable.get()}
     * @throws NullPointerException if {@code executable} is {@code null}
     */
    @Preview
    public static <R> R execute(@Nonnull Supplier<? extends R> executable) {
        Objects.requireNonNull(executable, "executable cannot be null");
        return executable.get();
    }
}
