package io.smallrye.common.function;

import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.DoubleFunction;
import java.util.function.Function;
import java.util.function.IntFunction;
import java.util.function.LongFunction;
import java.util.function.ObjDoubleConsumer;
import java.util.function.ObjIntConsumer;
import java.util.function.ObjLongConsumer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToDoubleBiFunction;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import java.util.function.ToLongBiFunction;
import java.util.function.ToLongFunction;

import io.smallrye.common.constraint.Assert;

/**
 * A set of utility methods which return common functions.
 */
public final class Functions {
    private Functions() {
    }

    /**
     * Get the singleton consumer which accepts and runs runnable instances.
     *
     * @return the runnable consumer
     */
    public static Consumer<Runnable> runnableConsumer() {
        return RunnableConsumer.INSTANCE;
    }

    /**
     * Get the singleton exception consumer which accepts and runs exception runnable instances.
     *
     * @param <E> the exception type
     * @return the runnable consumer
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <E extends Exception> ExceptionConsumer<ExceptionRunnable<E>, E> exceptionRunnableConsumer() {
        return ExceptionRunnableConsumer.INSTANCE;
    }

    /**
     * Get the singleton exception consumer which accepts and runs runnable instances.
     *
     * @return the runnable consumer
     */
    public static ExceptionConsumer<Runnable, RuntimeException> runnableExceptionConsumer() {
        return RunnableExceptionConsumer.INSTANCE;
    }

    /**
     * Get the singleton consumer which accepts a consumer and an argument to hand to it.
     *
     * @param <T> the argument type
     * @return the consumer
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> BiConsumer<Consumer<T>, T> consumerBiConsumer() {
        return ConsumerBiConsumer.INSTANCE;
    }

    /**
     * Get the singleton consumer which accepts a consumer and an argument to hand to it.
     *
     * @param <T> the argument type
     * @param <E> the exception type
     * @return the consumer
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T, E extends Exception> ExceptionBiConsumer<ExceptionConsumer<T, E>, T, E> exceptionConsumerBiConsumer() {
        return ExceptionConsumerBiConsumer.INSTANCE;
    }

    /**
     * Get the singleton consumer which accepts a consumer and an argument to hand to it.
     *
     * @param <T> the argument type
     * @return the consumer
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T> ExceptionBiConsumer<Consumer<T>, T, RuntimeException> consumerExceptionBiConsumer() {
        return ConsumerExceptionBiConsumer.INSTANCE;
    }

    /**
     * Get the singleton function which accepts a supplier and returns the result of the supplier.
     *
     * @param <R> the result type
     * @return the function
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <R> Function<Supplier<R>, R> supplierFunction() {
        return SupplierFunction.INSTANCE;
    }

    /**
     * Get the singleton function which accepts a supplier and returns the result of the supplier.
     *
     * @param <R> the result type
     * @param <E> the exception type
     * @return the function
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <R, E extends Exception> ExceptionFunction<ExceptionSupplier<R, E>, R, E> exceptionSupplierFunction() {
        return ExceptionSupplierFunction.INSTANCE;
    }

    /**
     * Get the singleton function which accepts a supplier and returns the result of the supplier.
     *
     * @param <R> the result type
     * @return the function
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <R> ExceptionFunction<Supplier<R>, R, RuntimeException> supplierExceptionFunction() {
        return SupplierExceptionFunction.INSTANCE;
    }

    /**
     * Get the singleton function which accepts a function which accepts a supplier, all of which return the result
     * of the supplier.
     *
     * @param <R> the result type
     * @return the function
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <R> BiFunction<Function<Supplier<R>, R>, Supplier<R>, R> supplierFunctionBiFunction() {
        return FunctionSupplierBiFunction.INSTANCE;
    }

    /**
     * Get the singleton function which accepts a function which accepts a supplier, all of which return the result
     * of the supplier.
     *
     * @param <R> the result type
     * @param <E> the exception type
     * @return the function
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <R, E extends Exception> ExceptionBiFunction<ExceptionFunction<ExceptionSupplier<R, E>, R, E>, ExceptionSupplier<R, E>, R, E> exceptionSupplierFunctionBiFunction() {
        return ExceptionFunctionSupplierBiFunction.INSTANCE;
    }

    /**
     * Get the singleton function which accepts a function and a parameter to pass to the function, and returns the
     * result of the function.
     *
     * @param <T> the argument type
     * @param <R> the result type
     * @return the function
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T, R> BiFunction<Function<T, R>, T, R> functionBiFunction() {
        return FunctionBiFunction.INSTANCE;
    }

    /**
     * Get the singleton function which accepts a function and a parameter to pass to the function, and returns the
     * result of the function.
     *
     * @param <T> the argument type
     * @param <R> the result type
     * @param <E> the exception type
     * @return the function
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T, R, E extends Exception> ExceptionBiFunction<ExceptionFunction<T, R, E>, T, R, E> exceptionFunctionBiFunction() {
        return ExceptionFunctionBiFunction.INSTANCE;
    }

    /**
     * Get the singleton function which accepts a function and a parameter to pass to the function, and returns the
     * result of the function.
     *
     * @param <T> the argument type
     * @param <R> the result type
     * @return the function
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T, R> ExceptionBiFunction<Function<T, R>, T, R, RuntimeException> functionExceptionBiFunction() {
        return FunctionExceptionBiFunction.INSTANCE;
    }

    /**
     * Get a supplier which always returns the same value.
     *
     * @param value the value to return
     * @param <T> the value type
     * @return the value supplier
     */
    @SuppressWarnings("unchecked")
    public static <T> Supplier<T> constantSupplier(T value) {
        return value == null ? ConstantSupplier.NULL : new ConstantSupplier<>(value);
    }

    /**
     * Get a supplier which always returns the same value.
     *
     * @param value the value to return
     * @param <T> the value type
     * @param <E> the exception type
     * @return the value supplier
     */
    @SuppressWarnings({ "unchecked", "rawtypes" })
    public static <T, E extends Exception> ExceptionSupplier<T, E> constantExceptionSupplier(T value) {
        return value == null ? ConstantSupplier.NULL : new ConstantSupplier(value);
    }

    /**
     * Get a runnable which executes the given consumer with captured values.
     *
     * @param consumer the consumer to run (must not be {@code null})
     * @param param1 the first parameter to pass
     * @param param2 the second parameter to pass
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @return the capturing runnable
     */
    public static <T, U> Runnable capturingRunnable(BiConsumer<T, U> consumer, T param1, U param2) {
        Assert.checkNotNullParam("consumer", consumer);
        return new BiConsumerRunnable<T, U>(consumer, param1, param2);
    }

    /**
     * Get a runnable which executes the given consumer with captured values.
     *
     * @param consumer the consumer to run (must not be {@code null})
     * @param param the parameter to pass
     * @param <T> the parameter type
     * @return the capturing runnable
     */
    public static <T> Runnable capturingRunnable(Consumer<T> consumer, T param) {
        Assert.checkNotNullParam("consumer", consumer);
        return new ConsumerRunnable<T>(consumer, param);
    }

    /**
     * Get a runnable which executes the given consumer with captured values.
     *
     * @param consumer the consumer to run (must not be {@code null})
     * @param param1 the first parameter to pass
     * @param param2 the second parameter to pass
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <E> the exception type
     * @return the capturing runnable
     */
    public static <T, U, E extends Exception> ExceptionRunnable<E> exceptionCapturingRunnable(
            ExceptionBiConsumer<T, U, E> consumer, T param1, U param2) {
        Assert.checkNotNullParam("consumer", consumer);
        return new ExceptionBiConsumerRunnable<T, U, E>(consumer, param1, param2);
    }

    /**
     * Get a runnable which executes the given consumer with captured values.
     *
     * @param consumer the consumer to run (must not be {@code null})
     * @param param the parameter to pass
     * @param <T> the parameter type
     * @param <E> the exception type
     * @return the capturing runnable
     */
    public static <T, E extends Exception> ExceptionRunnable<E> exceptionCapturingRunnable(ExceptionConsumer<T, E> consumer,
            T param) {
        Assert.checkNotNullParam("consumer", consumer);
        return new ExceptionConsumerRunnable<T, E>(consumer, param);
    }

    /**
     * Get a consumer which discards the values it is given.
     *
     * @param <T> the parameter type
     * @return the discarding consumer
     */
    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> discardingConsumer() {
        return DiscardingConsumer.INSTANCE;
    }

    /**
     * Get a consumer which discards the values it is given.
     *
     * @param <T> the parameter type
     * @param <E> the exception type
     * @return the discarding consumer
     */
    @SuppressWarnings("unchecked")
    public static <T, E extends Exception> ExceptionConsumer<T, E> discardingExceptionConsumer() {
        return DiscardingConsumer.INSTANCE;
    }

    /**
     * Get a consumer which discards the values it is given.
     *
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @return the discarding consumer
     */
    @SuppressWarnings("unchecked")
    public static <T, U> BiConsumer<T, U> discardingBiConsumer() {
        return DiscardingBiConsumer.INSTANCE;
    }

    /**
     * Get a consumer which discards the values it is given.
     *
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <E> the exception type
     * @return the discarding consumer
     */
    @SuppressWarnings("unchecked")
    public static <T, U, E extends Exception> ExceptionBiConsumer<T, U, E> discardingExceptionBiConsumer() {
        return DiscardingBiConsumer.INSTANCE;
    }

    private static final Consumer<AutoCloseable> CLOSING_CONSUMER = quiet(AutoCloseable::close, exceptionLoggingConsumer());

    /**
     * Returns a consumer that quietly closes its argument, logging any exceptions.
     *
     * @return a closing consumer
     */
    @SuppressWarnings("unchecked")
    public static <T extends AutoCloseable> Consumer<T> closingConsumer() {
        return (Consumer<T>) CLOSING_CONSUMER;
    }

    private static final Consumer<Exception> EXCEPTION_LOGGER = new Consumer<>() {
        @Override
        public void accept(Exception e) {
            FunctionsLogging.log.exception(e);
        }
    };

    /**
     * Returns a consumer that logs its exception parameter as a warning.
     *
     * @param <E> the exception type
     * @return an exception consumer
     */
    @SuppressWarnings("unchecked")
    public static <E extends Exception> Consumer<E> exceptionLoggingConsumer() {
        return (Consumer<E>) EXCEPTION_LOGGER;
    }

    /**
     * Returns a consumer that wraps and throws its exception parameter as a {@link RuntimeException}.
     *
     * @param <E> the exception type
     * @param wrapper a runtime exception wrapper
     * @return an exception consumer
     */
    public static <E extends Exception, RE extends RuntimeException> Consumer<E> runtimeExceptionThrowingConsumer(
            Function<E, RE> wrapper) {
        return new Consumer<>() {
            @Override
            public void accept(E exception) {
                throw wrapper.apply(exception);
            }
        };
    }

    /**
     * Converts an {@link ExceptionConsumer} to a standard {@link Consumer} using the specified exception handler.
     *
     * @param <T> the parameter type of the consumer
     * @param <E> the exception type
     * @param consumer an exception consumer
     * @param handler an exception handler
     * @return a standard consumer
     */
    public static <T, E extends Exception> Consumer<T> quiet(ExceptionConsumer<T, E> consumer, Consumer<E> handler) {
        return new Consumer<>() {
            @SuppressWarnings("unchecked")
            @Override
            public void accept(T value) {
                try {
                    consumer.accept(value);
                } catch (Exception e) {
                    handler.accept((E) e);
                }
            }
        };
    }

    /**
     * Converts an {@link ExceptionBiConsumer} to a standard {@link BiConsumer} using the specified exception handler.
     *
     * @param <T> the first parameter type of the consumer
     * @param <U> the second parameter type of the consumer
     * @param <E> the exception type
     * @param consumer a binary exception consumer
     * @param handler an exception handler
     * @return a standard binary consumer
     */
    public static <T, U, E extends Exception> BiConsumer<T, U> quiet(ExceptionBiConsumer<T, U, E> consumer,
            Consumer<E> handler) {
        return new BiConsumer<>() {
            @SuppressWarnings("unchecked")
            @Override
            public void accept(T value1, U value2) {
                try {
                    consumer.accept(value1, value2);
                } catch (Exception e) {
                    handler.accept((E) e);
                }
            }
        };
    }

    /**
     * Converts an {@link ExceptionObjIntConsumer} to a standard {@link ObjIntConsumer} using the specified exception handler.
     *
     * @param <T> the first parameter type of the consumer
     * @param <E> the exception type
     * @param consumer an object/int exception consumer
     * @param handler an exception handler
     * @return a standard object/int consumer
     */
    public static <T, E extends Exception> ObjIntConsumer<T> quiet(ExceptionObjIntConsumer<T, E> consumer,
            Consumer<E> handler) {
        return new ObjIntConsumer<>() {
            @SuppressWarnings("unchecked")
            @Override
            public void accept(T object, int i) {
                try {
                    consumer.accept(object, i);
                } catch (Exception e) {
                    handler.accept((E) e);
                }
            }
        };
    }

    /**
     * Converts an {@link ExceptionObjLongConsumer} to a standard {@link ObjLongConsumer} using the specified exception handler.
     *
     * @param <T> the first parameter type of the consumer
     * @param <E> the exception type
     * @param consumer an object/long exception consumer
     * @param handler an exception handler
     * @return a standard object/long consumer
     */
    public static <T, E extends Exception> ObjLongConsumer<T> quiet(ExceptionObjLongConsumer<T, E> consumer,
            Consumer<E> handler) {
        return new ObjLongConsumer<>() {
            @SuppressWarnings("unchecked")
            @Override
            public void accept(T object, long i) {
                try {
                    consumer.accept(object, i);
                } catch (Exception e) {
                    handler.accept((E) e);
                }
            }
        };
    }

    /**
     * Returns a {@link Consumer} with identical behavior to the specified {@link Consumer}
     * but with restricted parameter type.
     *
     * @param <T> the parameter type
     * @param <TT> the restricted parameter type
     * @param consumer a consumer
     * @return a functionally equivalent consumer
     */
    @SuppressWarnings("unchecked")
    public static <T, TT extends T> Consumer<TT> cast(Consumer<T> consumer) {
        return (Consumer<TT>) consumer;
    }

    /**
     * Returns a {@link Predicate} with identical behavior to the specified {@link Predicate}
     * but with restricted parameter type.
     *
     * @param <T> the parameter type
     * @param <TT> the restricted parameter type
     * @param predicate a predicate
     * @return a functionally equivalent predicate
     */
    @SuppressWarnings("unchecked")
    public static <T, TT extends T> Predicate<TT> cast(Predicate<T> predicate) {
        return (Predicate<TT>) predicate;
    }

    /**
     * Returns a {@link Supplier} with identical behavior to the specified {@link Supplier}
     * but with relaxed return type.
     *
     * @param <T> the return type
     * @param <TT> the relaxed return type
     * @param supplier a supplier
     * @return a functionally equivalent supplier
     */
    @SuppressWarnings("unchecked")
    public static <T extends TT, TT> Supplier<TT> cast(Supplier<T> supplier) {
        return (Supplier<TT>) supplier;
    }

    /**
     * Returns a {@link Function} with identical behavior to the specified {@link Function}
     * but with restricted parameter type and relaxed return type.
     *
     * @param <T> the parameter type
     * @param <R> the return type
     * @param <TT> the restricted parameter type
     * @param <RR> the relaxed return type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <T, R extends RR, TT extends T, RR> Function<TT, RR> cast(Function<T, R> function) {
        return (Function<TT, RR>) function;
    }

    /**
     * Returns a {@link DoubleFunction} with identical behavior to the specified {@link DoubleFunction}
     * but with relaxed return type.
     *
     * @param <R> the return type
     * @param <RR> the relaxed return type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <R extends RR, RR> DoubleFunction<RR> cast(DoubleFunction<R> function) {
        return (DoubleFunction<RR>) function;
    }

    /**
     * Returns a {@link IntFunction} with identical behavior to the specified {@link IntFunction}
     * but with relaxed return type.
     *
     * @param <R> the return type
     * @param <RR> the relaxed return type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <R extends RR, RR> IntFunction<RR> cast(IntFunction<R> function) {
        return (IntFunction<RR>) function;
    }

    /**
     * Returns a {@link LongFunction} with identical behavior to the specified {@link LongFunction}
     * but with relaxed return type.
     *
     * @param <R> the return type
     * @param <RR> the relaxed return type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <R extends RR, RR> LongFunction<RR> cast(LongFunction<R> function) {
        return (LongFunction<RR>) function;
    }

    /**
     * Returns a {@link ToDoubleFunction} with identical behavior to the specified {@link ToDoubleFunction}
     * but with restricted parameter type.
     *
     * @param <T> the parameter type
     * @param <TT> the restricted parameter type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <T, TT extends T> ToDoubleFunction<TT> cast(ToDoubleFunction<T> function) {
        return (ToDoubleFunction<TT>) function;
    }

    /**
     * Returns a {@link ToIntFunction} with identical behavior to the specified {@link ToIntFunction}
     * but with restricted parameter type.
     *
     * @param <T> the parameter type
     * @param <TT> the restricted parameter type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <T, TT extends T> ToIntFunction<TT> cast(ToIntFunction<T> function) {
        return (ToIntFunction<TT>) function;
    }

    /**
     * Returns a {@link ToLongFunction} with identical behavior to the specified {@link ToLongFunction}
     * but with restricted parameter type.
     *
     * @param <T> the parameter type
     * @param <TT> the restricted parameter type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <T, TT extends T> ToLongFunction<TT> cast(ToLongFunction<T> function) {
        return (ToLongFunction<TT>) function;
    }

    /**
     * Returns a {@link BiConsumer} with identical behavior to the specified {@link BiConsumer}
     * but with restricted parameter types.
     *
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <TT> the restricted first parameter type
     * @param <UU> the restricted second parameter type
     * @param consumer a consumer
     * @return a functionally equivalent consumer
     */
    @SuppressWarnings("unchecked")
    public static <T, U, TT extends T, UU extends U> BiConsumer<TT, UU> cast(BiConsumer<T, U> consumer) {
        return (BiConsumer<TT, UU>) consumer;
    }

    /**
     * Returns a {@link ObjDoubleConsumer} with identical behavior to the specified {@link ObjDoubleConsumer}
     * but with restricted parameter type.
     *
     * @param <T> the parameter type
     * @param <TT> the restricted parameter type
     * @param consumer a consumer
     * @return a functionally equivalent consumer
     */
    @SuppressWarnings("unchecked")
    public static <T, TT extends T> ObjDoubleConsumer<TT> cast(ObjDoubleConsumer<T> consumer) {
        return (ObjDoubleConsumer<TT>) consumer;
    }

    /**
     * Returns a {@link ObjIntConsumer} with identical behavior to the specified {@link ObjIntConsumer}
     * but with restricted parameter type.
     *
     * @param <T> the parameter type
     * @param <TT> the restricted parameter type
     * @param consumer a consumer
     * @return a functionally equivalent consumer
     */
    @SuppressWarnings("unchecked")
    public static <T, TT extends T> ObjIntConsumer<TT> cast(ObjIntConsumer<T> consumer) {
        return (ObjIntConsumer<TT>) consumer;
    }

    /**
     * Returns a {@link ObjLongConsumer} with identical behavior to the specified {@link ObjLongConsumer}
     * but with restricted parameter type.
     *
     * @param <T> the parameter type
     * @param <TT> the restricted parameter type
     * @param consumer a consumer
     * @return a functionally equivalent consumer
     */
    @SuppressWarnings("unchecked")
    public static <T, TT extends T> ObjLongConsumer<TT> cast(ObjLongConsumer<T> consumer) {
        return (ObjLongConsumer<TT>) consumer;
    }

    /**
     * Returns a {@link BiPredicate} with identical behavior to the specified {@link BiPredicate}
     * but with restricted parameter types.
     *
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <TT> the restricted first parameter type
     * @param <UU> the restricted second parameter type
     * @param predicate a predicate
     * @return a functionally equivalent predicate
     */
    @SuppressWarnings("unchecked")
    public static <T, U, TT extends T, UU extends U> BiPredicate<TT, UU> cast(BiPredicate<T, U> predicate) {
        return (BiPredicate<TT, UU>) predicate;
    }

    /**
     * Returns a {@link BiFunction} with identical behavior to the specified {@link BiFunction}
     * but with restricted parameter types and relaxed return type.
     *
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <R> the return type
     * @param <TT> the restricted first parameter type
     * @param <UU> the restricted second parameter type
     * @param <RR> the relaxed return type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <T, U, R extends RR, TT extends T, UU extends U, RR> BiFunction<TT, UU, RR> cast(
            BiFunction<T, U, R> function) {
        return (BiFunction<TT, UU, RR>) function;
    }

    /**
     * Returns a {@link ToDoubleBiFunction} with identical behavior to the specified {@link ToDoubleBiFunction}
     * but with restricted parameter types.
     *
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <TT> the restricted first parameter type
     * @param <UU> the restricted second parameter type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <T, U, TT extends T, UU extends U> ToDoubleBiFunction<TT, UU> cast(ToDoubleBiFunction<T, U> function) {
        return (ToDoubleBiFunction<TT, UU>) function;
    }

    /**
     * Returns a {@link ToIntBiFunction} with identical behavior to the specified {@link ToIntBiFunction}
     * but with restricted parameter types.
     *
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <TT> the restricted first parameter type
     * @param <UU> the restricted second parameter type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <T, U, TT extends T, UU extends U> ToIntBiFunction<TT, UU> cast(ToIntBiFunction<T, U> function) {
        return (ToIntBiFunction<TT, UU>) function;
    }

    /**
     * Returns a {@link ToLongBiFunction} with identical behavior to the specified {@link ToLongBiFunction}
     * but with restricted parameter types.
     *
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <TT> the restricted first parameter type
     * @param <UU> the restricted second parameter type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <T, U, TT extends T, UU extends U> ToLongBiFunction<TT, UU> cast(ToLongBiFunction<T, U> function) {
        return (ToLongBiFunction<TT, UU>) function;
    }

    /**
     * Returns a {@link ExceptionConsumer} with identical behavior to the specified {@link ExceptionConsumer}
     * but with restricted parameter type and relaxed exception type.
     *
     * @param <T> the parameter type
     * @param <E> the exception type
     * @param <TT> the restricted parameter type
     * @param <EE> the relaxed exception type
     * @param consumer a consumer
     * @return a functionally equivalent consumer
     */
    @SuppressWarnings("unchecked")
    public static <T, E extends EE, TT extends T, EE extends Exception> ExceptionConsumer<TT, EE> cast(
            ExceptionConsumer<T, E> consumer) {
        return (ExceptionConsumer<TT, EE>) consumer;
    }

    /**
     * Returns a {@link ExceptionPredicate} with identical behavior to the specified {@link ExceptionPredicate}
     * but with restricted parameter type and relaxed exception type.
     *
     * @param <T> the parameter type
     * @param <E> the exception type
     * @param <TT> the restricted parameter type
     * @param <EE> the relaxed exception type
     * @param consumer a consumer
     * @return a functionally equivalent predicate
     */
    @SuppressWarnings("unchecked")
    public static <T, E extends EE, TT extends T, EE extends Exception> ExceptionPredicate<TT, EE> cast(
            ExceptionPredicate<T, E> consumer) {
        return (ExceptionPredicate<TT, EE>) consumer;
    }

    /**
     * Returns a {@link ExceptionSupplier} with identical behavior to the specified {@link ExceptionSupplier}
     * but with relaxed return type and relaxed exception type.
     *
     * @param <T> the return type
     * @param <E> the exception type
     * @param <TT> the relaxed return type
     * @param <EE> the relaxed exception type
     * @param supplier a supplier
     * @return a functionally equivalent supplier
     */
    @SuppressWarnings("unchecked")
    public static <T extends TT, E extends EE, TT, EE extends Exception> ExceptionSupplier<TT, EE> cast(
            ExceptionSupplier<T, E> supplier) {
        return (ExceptionSupplier<TT, EE>) supplier;
    }

    /**
     * Returns a {@link ExceptionFunction} with identical behavior to the specified {@link ExceptionFunction}
     * but with restricted parameter type, relaxed return type, and relaxed exception type.
     *
     * @param <T> the parameter type
     * @param <R> the return type
     * @param <E> the exception type
     * @param <TT> the restricted parameter type
     * @param <RR> the relaxed return type
     * @param <EE> the relaxed exception type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <T, R extends RR, E extends EE, TT extends T, RR, EE extends Exception> ExceptionFunction<TT, RR, EE> cast(
            ExceptionFunction<T, R, E> function) {
        return (ExceptionFunction<TT, RR, EE>) function;
    }

    /**
     * Returns a {@link ExceptionIntFunction} with identical behavior to the specified {@link ExceptionFunction}
     * but with relaxed return type and relaxed exception type.
     *
     * @param <R> the return type
     * @param <E> the exception type
     * @param <RR> the relaxed return type
     * @param <EE> the relaxed exception type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <R extends RR, E extends EE, RR, EE extends Exception> ExceptionIntFunction<RR, EE> cast(
            ExceptionIntFunction<R, E> function) {
        return (ExceptionIntFunction<RR, EE>) function;
    }

    /**
     * Returns a {@link ExceptionLongFunction} with identical behavior to the specified {@link ExceptionLongFunction}
     * but with relaxed return type and relaxed exception type.
     *
     * @param <R> the return type
     * @param <E> the exception type
     * @param <RR> the relaxed return type
     * @param <EE> the relaxed exception type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <R extends RR, E extends EE, RR, EE extends Exception> ExceptionLongFunction<RR, EE> cast(
            ExceptionLongFunction<R, E> function) {
        return (ExceptionLongFunction<RR, EE>) function;
    }

    /**
     * Returns a {@link ExceptionToIntFunction} with identical behavior to the specified {@link ExceptionToIntFunction}
     * but with restricted parameter type and relaxed exception type.
     *
     * @param <T> the parameter type
     * @param <E> the exception type
     * @param <TT> the restricted parameter type
     * @param <EE> the relaxed exception type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <T, E extends EE, TT extends T, EE extends Exception> ExceptionToIntFunction<TT, EE> cast(
            ExceptionToIntFunction<T, E> function) {
        return (ExceptionToIntFunction<TT, EE>) function;
    }

    /**
     * Returns a {@link ExceptionToLongFunction} with identical behavior to the specified {@link ExceptionToLongFunction}
     * but with restricted parameter type and relaxed exception type.
     *
     * @param <T> the parameter type
     * @param <E> the exception type
     * @param <TT> the restricted parameter type
     * @param <EE> the relaxed exception type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <T, E extends EE, TT extends T, EE extends Exception> ExceptionToLongFunction<TT, EE> cast(
            ExceptionToLongFunction<T, E> function) {
        return (ExceptionToLongFunction<TT, EE>) function;
    }

    /**
     * Returns a {@link ExceptionBiConsumer} with identical behavior to the specified {@link ExceptionBiConsumer}
     * but with restricted parameter types and relaxed exception type.
     *
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <E> the exception type
     * @param <TT> the restricted first parameter type
     * @param <UU> the restricted second parameter type
     * @param <EE> the relaxed exception type
     * @param consumer a consumer
     * @return a functionally equivalent consumer
     */
    @SuppressWarnings("unchecked")
    public static <T, E extends EE, TT extends T, U, UU extends U, EE extends Exception> ExceptionBiConsumer<TT, UU, EE> cast(
            ExceptionBiConsumer<T, U, E> consumer) {
        return (ExceptionBiConsumer<TT, UU, EE>) consumer;
    }

    /**
     * Returns a {@link ExceptionObjIntConsumer} with identical behavior to the specified {@link ExceptionObjIntConsumer}
     * but with restricted parameter type and relaxed exception type.
     *
     * @param <T> the parameter type
     * @param <E> the exception type
     * @param <TT> the restricted parameter type
     * @param <EE> the relaxed exception type
     * @param consumer a consumer
     * @return a functionally equivalent consumer
     */
    @SuppressWarnings("unchecked")
    public static <T, E extends EE, TT extends T, EE extends Exception> ExceptionObjIntConsumer<TT, EE> cast(
            ExceptionObjIntConsumer<T, E> consumer) {
        return (ExceptionObjIntConsumer<TT, EE>) consumer;
    }

    /**
     * Returns a {@link ExceptionObjLongConsumer} with identical behavior to the specified {@link ExceptionObjLongConsumer}
     * but with restricted parameter type and relaxed exception type.
     *
     * @param <T> the parameter type
     * @param <E> the exception type
     * @param <TT> the restricted parameter type
     * @param <EE> the relaxed exception type
     * @param consumer a consumer
     * @return a functionally equivalent consumer
     */
    @SuppressWarnings("unchecked")
    public static <T, E extends EE, TT extends T, EE extends Exception> ExceptionObjLongConsumer<TT, EE> cast(
            ExceptionObjLongConsumer<T, E> consumer) {
        return (ExceptionObjLongConsumer<TT, EE>) consumer;
    }

    /**
     * Returns a {@link ExceptionBiPredicate} with identical behavior to the specified {@link ExceptionBiPredicate}
     * but with restricted parameter types and relaxed exception type.
     *
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <E> the exception type
     * @param <TT> the restricted first parameter type
     * @param <UU> the restricted second parameter type
     * @param <EE> the relaxed exception type
     * @param predicate a predicate
     * @return a functionally equivalent predicate
     */
    @SuppressWarnings("unchecked")
    public static <T, U, E extends EE, TT extends T, UU extends U, EE extends Exception> ExceptionBiPredicate<TT, UU, EE> cast(
            ExceptionBiPredicate<T, U, E> predicate) {
        return (ExceptionBiPredicate<TT, UU, EE>) predicate;
    }

    /**
     * Returns a {@link ExceptionBiFunction} with identical behavior to the specified {@link ExceptionBiFunction}
     * but with restricted parameter types, relaxed return type, and relaxed exception type.
     *
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <R> the return type
     * @param <E> the exception type
     * @param <TT> the restricted first parameter type
     * @param <UU> the restricted second parameter type
     * @param <RR> the relaxed return type
     * @param <EE> the relaxed exception type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <T, U, R extends RR, E extends EE, TT extends T, UU extends U, RR, EE extends Exception> ExceptionBiFunction<TT, UU, RR, EE> cast(
            ExceptionBiFunction<T, U, R, E> function) {
        return (ExceptionBiFunction<TT, UU, RR, EE>) function;
    }

    /**
     * Returns a {@link ExceptionToIntBiFunction} with identical behavior to the specified {@link ExceptionToIntBiFunction}
     * but with restricted parameter types and relaxed exception type.
     *
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <E> the exception type
     * @param <TT> the restricted first parameter type
     * @param <UU> the restricted second parameter type
     * @param <EE> the relaxed exception type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <T, U, E extends EE, TT extends T, UU extends U, EE extends Exception> ExceptionToIntBiFunction<TT, UU, EE> cast(
            ExceptionToIntBiFunction<T, U, E> function) {
        return (ExceptionToIntBiFunction<TT, UU, EE>) function;
    }

    /**
     * Returns a {@link ExceptionToLongBiFunction} with identical behavior to the specified {@link ExceptionToLongBiFunction}
     * but with restricted parameter types and relaxed exception type.
     *
     * @param <T> the first parameter type
     * @param <U> the second parameter type
     * @param <E> the exception type
     * @param <TT> the restricted first parameter type
     * @param <UU> the restricted second parameter type
     * @param <EE> the relaxed exception type
     * @param function a function
     * @return a functionally equivalent function
     */
    @SuppressWarnings("unchecked")
    public static <T, U, E extends EE, TT extends T, UU extends U, EE extends Exception> ExceptionToLongBiFunction<TT, UU, EE> cast(
            ExceptionToLongBiFunction<T, U, E> function) {
        return (ExceptionToLongBiFunction<TT, UU, EE>) function;
    }

    static class RunnableConsumer implements Consumer<Runnable> {
        static final Consumer<Runnable> INSTANCE = new RunnableConsumer();

        private RunnableConsumer() {
        }

        public void accept(final Runnable runnable) {
            runnable.run();
        }
    }

    static class ExceptionRunnableConsumer<E extends Exception> implements ExceptionConsumer<ExceptionRunnable<E>, E> {
        static final ExceptionConsumer INSTANCE = new ExceptionRunnableConsumer<>();

        private ExceptionRunnableConsumer() {
        }

        public void accept(final ExceptionRunnable<E> ExceptionRunnable) throws E {
            ExceptionRunnable.run();
        }
    }

    static class RunnableExceptionConsumer implements ExceptionConsumer<Runnable, RuntimeException> {
        static final RunnableExceptionConsumer INSTANCE = new RunnableExceptionConsumer();

        private RunnableExceptionConsumer() {
        }

        public void accept(final Runnable runnable) throws RuntimeException {
            runnable.run();
        }
    }

    static class ConsumerBiConsumer implements BiConsumer<Consumer<Object>, Object> {
        static final BiConsumer INSTANCE = new ConsumerBiConsumer();

        private ConsumerBiConsumer() {
        }

        public void accept(final Consumer<Object> consumer, final Object o) {
            consumer.accept(o);
        }
    }

    static class ExceptionConsumerBiConsumer<E extends Exception>
            implements ExceptionBiConsumer<ExceptionConsumer<Object, E>, Object, E> {
        static final ExceptionBiConsumer INSTANCE = new ExceptionConsumerBiConsumer<>();

        private ExceptionConsumerBiConsumer() {
        }

        public void accept(final ExceptionConsumer<Object, E> consumer, final Object o) throws E {
            consumer.accept(o);
        }
    }

    static class ConsumerExceptionBiConsumer<T> implements ExceptionBiConsumer<Consumer<T>, T, RuntimeException> {
        static final ExceptionBiConsumer INSTANCE = new ConsumerExceptionBiConsumer();

        private ConsumerExceptionBiConsumer() {
        }

        public void accept(final Consumer<T> consumer, final T t) throws RuntimeException {
            consumer.accept(t);
        }
    }

    static class SupplierFunction implements Function<Supplier<Object>, Object> {
        static final Function INSTANCE = new SupplierFunction();

        private SupplierFunction() {
        }

        public Object apply(final Supplier<Object> supplier) {
            return supplier.get();
        }
    }

    static class ExceptionSupplierFunction<E extends Exception>
            implements ExceptionFunction<ExceptionSupplier<Object, E>, Object, E> {
        static final ExceptionFunction INSTANCE = new ExceptionSupplierFunction<>();

        private ExceptionSupplierFunction() {
        }

        public Object apply(final ExceptionSupplier<Object, E> supplier) throws E {
            return supplier.get();
        }
    }

    static class SupplierExceptionFunction<R> implements ExceptionFunction<Supplier<R>, R, RuntimeException> {
        static final SupplierExceptionFunction INSTANCE = new SupplierExceptionFunction();

        private SupplierExceptionFunction() {
        }

        public R apply(final Supplier<R> supplier) throws RuntimeException {
            return supplier.get();
        }
    }

    static class FunctionSupplierBiFunction
            implements BiFunction<Function<Supplier<Object>, Object>, Supplier<Object>, Object> {
        static final BiFunction INSTANCE = new FunctionSupplierBiFunction();

        private FunctionSupplierBiFunction() {
        }

        public Object apply(final Function<Supplier<Object>, Object> function, final Supplier<Object> supplier) {
            return function.apply(supplier);
        }
    }

    static class ExceptionFunctionSupplierBiFunction<E extends Exception> implements
            ExceptionBiFunction<ExceptionFunction<ExceptionSupplier<Object, E>, Object, E>, ExceptionSupplier<Object, E>, Object, E> {
        static final ExceptionBiFunction INSTANCE = new ExceptionFunctionSupplierBiFunction();

        private ExceptionFunctionSupplierBiFunction() {
        }

        public Object apply(final ExceptionFunction<ExceptionSupplier<Object, E>, Object, E> function,
                final ExceptionSupplier<Object, E> supplier) throws E {
            return function.apply(supplier);
        }
    }

    static class FunctionExceptionBiFunction<T, R> implements ExceptionBiFunction<Function<T, R>, T, R, RuntimeException> {
        static final FunctionExceptionBiFunction INSTANCE = new FunctionExceptionBiFunction();

        private FunctionExceptionBiFunction() {
        }

        public R apply(final Function<T, R> function, final T t) throws RuntimeException {
            return function.apply(t);
        }
    }

    static class FunctionBiFunction<T, R> implements BiFunction<Function<T, R>, T, R> {
        static final BiFunction INSTANCE = new FunctionBiFunction();

        private FunctionBiFunction() {
        }

        public R apply(final Function<T, R> function, final T t) {
            return function.apply(t);
        }
    }

    static class ExceptionFunctionBiFunction<T, R, E extends Exception>
            implements ExceptionBiFunction<ExceptionFunction<T, R, E>, T, R, E> {
        static final ExceptionBiFunction INSTANCE = new ExceptionFunctionBiFunction();

        private ExceptionFunctionBiFunction() {
        }

        public R apply(final ExceptionFunction<T, R, E> function, final T t) throws E {
            return function.apply(t);
        }
    }

    static class ConstantSupplier<T> implements Supplier<T>, ExceptionSupplier<T, RuntimeException> {
        static final ConstantSupplier NULL = new ConstantSupplier<>(null);

        private final T arg1;

        ConstantSupplier(final T arg1) {
            this.arg1 = arg1;
        }

        public T get() {
            return arg1;
        }

        public String toString() {
            return String.format("supplier(%s)", arg1);
        }
    }

    static class BiConsumerRunnable<T, U> implements Runnable {
        private final BiConsumer<T, U> consumer;
        private final T param1;
        private final U param2;

        BiConsumerRunnable(final BiConsumer<T, U> consumer, final T param1, final U param2) {
            this.consumer = consumer;
            this.param1 = param1;
            this.param2 = param2;
        }

        public void run() {
            consumer.accept(param1, param2);
        }

        public String toString() {
            return String.format("%s(%s,%s)", consumer, param1, param2);
        }
    }

    static class ConsumerRunnable<T> implements Runnable {
        private final Consumer<T> consumer;
        private final T param;

        ConsumerRunnable(final Consumer<T> consumer, final T param) {
            this.consumer = consumer;
            this.param = param;
        }

        public void run() {
            consumer.accept(param);
        }

        public String toString() {
            return String.format("%s(%s)", consumer, param);
        }
    }

    static class ExceptionBiConsumerRunnable<T, U, E extends Exception> implements ExceptionRunnable<E> {
        private final ExceptionBiConsumer<T, U, E> consumer;
        private final T param1;
        private final U param2;

        ExceptionBiConsumerRunnable(final ExceptionBiConsumer<T, U, E> consumer, final T param1, final U param2) {
            this.consumer = consumer;
            this.param1 = param1;
            this.param2 = param2;
        }

        public void run() throws E {
            consumer.accept(param1, param2);
        }

        public String toString() {
            return String.format("%s(%s,%s)", consumer, param1, param2);
        }
    }

    static class ExceptionConsumerRunnable<T, E extends Exception> implements ExceptionRunnable<E> {
        private final ExceptionConsumer<T, E> consumer;
        private final T param;

        ExceptionConsumerRunnable(final ExceptionConsumer<T, E> consumer, final T param) {
            this.consumer = consumer;
            this.param = param;
        }

        public void run() throws E {
            consumer.accept(param);
        }

        public String toString() {
            return String.format("%s(%s)", consumer, param);
        }
    }

    static class DiscardingConsumer<T, E extends Exception> implements Consumer<T>, ExceptionConsumer<T, E> {
        static final DiscardingConsumer INSTANCE = new DiscardingConsumer();

        private DiscardingConsumer() {
        }

        public void accept(final T t) {
        }
    }

    static class DiscardingBiConsumer<T, U, E extends Exception> implements BiConsumer<T, U>, ExceptionBiConsumer<T, U, E> {
        static final DiscardingBiConsumer INSTANCE = new DiscardingBiConsumer();

        private DiscardingBiConsumer() {
        }

        public void accept(final T t, final U u) {
        }
    }
}
