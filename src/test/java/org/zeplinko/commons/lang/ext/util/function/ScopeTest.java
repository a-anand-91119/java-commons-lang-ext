package org.zeplinko.commons.lang.ext.util.function;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

class ScopeTest {

    @Test
    void test_whenMutateIsInvokedOnArray_thenMutatorExecutesAndReturnTheSameInstance() {
        Integer[] array = new Integer[] { 1 };
        Integer[] returnedArray = Scope.mutate(array, a -> a[0] = 2);
        assertSame(array, returnedArray);
        assertEquals(2, returnedArray[0]);
    }

    @Test
    void test_whenMutateIsInvokedOnArrayList_thenMutatorExecutesAndReturnTheSameInstance() {
        ArrayList<Integer> list = new ArrayList<>();
        list.add(1);
        ArrayList<Integer> returnedList = Scope.mutate(list, a -> a.add(2));
        assertSame(list, returnedList);
        assertEquals(2, returnedList.size());
        assertEquals(1, returnedList.get(0));
        assertEquals(2, returnedList.get(1));
    }

    @Test
    void test_whenMutateIsInvokedWithNullMutator_thenThrowsNullPointerException() {
        assertThrowsExactly(NullPointerException.class, () -> Scope.mutate(new Object(), null));
    }

    @Test
    void test_whenTransformIsInvokedOnArray_thenTransformerExecutesAndReturnTheTransformedObject() {
        Integer[] array = new Integer[] { 1, 2, 3, 4, 5 };
        ArrayList<Integer> returnedList = Scope.transform(array, a -> new ArrayList<>(Arrays.asList(a)));
        assertNotNull(returnedList);
        assertEquals(array.length, returnedList.size());
        IntStream.range(0, array.length).forEach(i -> assertEquals(array[i], returnedList.get(i)));
    }

    @Test
    void test_whenTransformIsInvokedWithNullTransformer_thenThrowsNullPointerException() {
        assertThrowsExactly(NullPointerException.class, () -> Scope.transform(new Object(), null));
    }

    @Test
    void test_whenExecuteIsInvoked_thenExecutorExecutesAndReturnTheValue() {
        ArrayList<Integer> returnedList = Scope.execute(() -> {
            ArrayList<Integer> list = new ArrayList<>();
            list.add(1);
            list.add(2);
            list.add(3);
            list.add(4);
            list.add(5);
            return list;
        });
        assertNotNull(returnedList);
        assertEquals(5, returnedList.size());
    }

    @Test
    void test_whenExecuteIsInvokedWithNullExecutable_thenThrowsNullPointerException() {
        assertThrowsExactly(NullPointerException.class, () -> Scope.execute(null));
    }
}
