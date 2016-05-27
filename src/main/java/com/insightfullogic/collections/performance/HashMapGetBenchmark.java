package com.insightfullogic.collections.performance;

import org.openjdk.jmh.annotations.*;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;

@Fork(1)
@Threads(1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@State(Scope.Benchmark)
@BenchmarkMode(Mode.Throughput)
public class HashMapGetBenchmark
{

    @Param({"10", "10000", "1000000"})
    int size;

    @Param({"0.1", "0.5", "0.9"})
    double collisionProb;

    @Param({"JdkMap", "Koloboke" })
    String mapType;

    @Param({/*"Comparable",*/ "InComparable"})
    String keyType;

    Map<Object, String> map;

    int successIndex = 0;
    int failIndex = 0;

    // Used to simulate misses (gets that return null) with keys that aren't part of the map
    Object[] failKeys = new Object[size];
    Object[] successfulKeys = new Object[size];
    List<String> values = new ArrayList<>(size);

    @Setup
    public void setup()
    {
        final MapFactory mapFactory = MapFactory.valueOf(mapType);
        final KeyFactory keyFactory = KeyFactory.valueOf(keyType);

        map = mapFactory.make();

        final int size = this.size;
        final Random random = new Random(666);
        final int numberOfHashes = (int) (size * collisionProb);
        final int[] hashes = new int[numberOfHashes];
        for (int i = 0; i < numberOfHashes; i++)
        {
            hashes[i] = random.nextInt(size);
        }

        for (int i = 0; i < size; i++)
        {
            final int hash = hashes[random.nextInt(numberOfHashes)];
            final String value = String.valueOf(i);

            successfulKeys[i] = keyFactory.make(i, hash);
            failKeys[i] = keyFactory.make(-i, hash);
            values.add(value);
            map.put(successfulKeys[i], value);
        }

        System.gc();

        Collections.shuffle(asList(successfulKeys));
        Collections.shuffle(asList(failKeys));

        System.gc();
    }

    // Baseline to be able to remove overhead of nextKey() operation
    @Benchmark
    public Object baseline()
    {
        return nextSuccessfulKey();
    }

    @Benchmark
    public String getSuccess()
    {
        final Object key = nextSuccessfulKey();
        return map.get(key);
    }

    @Benchmark
    public String getFail()
    {
        final Object key = nextFailKey();
        return map.get(key);
    }

    private Object nextSuccessfulKey()
    {
        return successfulKeys[successIndex++ & (successfulKeys.length - 1)];
    }

    private Object nextFailKey()
    {
        return failKeys[failIndex++ & (failKeys.length - 1)];
    }

    // Done:
        // hash collisions
        // Different keys
        // Different Size
        // shuffle keys
        // Comparable vs incomparable
}