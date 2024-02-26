package app;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import com.hazelcast.map.EntryProcessor;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws InterruptedException {
        Config config = new Config();
        config.setClusterName("my-hazelcast-cluster");
        HazelcastInstance instance1 = Hazelcast.newHazelcastInstance(config);
        HazelcastInstance instance2 = Hazelcast.newHazelcastInstance(config);
        HazelcastInstance instance3 = Hazelcast.newHazelcastInstance(config);

        // Scenario 1: No Locking
        System.out.println("Running without locking...");
        runScenario(instance1, instance2, instance3, Main::incrementNoLocking);

        resetCounter(instance1);

        // Scenario 2: Pessimistic Locking
        System.out.println("Running with pessimistic locking...");
        runScenario(instance1, instance2, instance3, Main::incrementWithPessimisticLocking);

        resetCounter(instance1);

        // Scenario 3: Optimistic Locking
        System.out.println("Running with optimistic locking...");
        runScenario(instance1, instance2, instance3, Main::incrementWithOptimisticLocking);
    }

    private static void runScenario(HazelcastInstance instance1, HazelcastInstance instance2,
                                    HazelcastInstance instance3, ScenarioRunner runner)
            throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(3);
        executor.submit(() -> runner.run(instance1));
        executor.submit(() -> runner.run(instance2));
        executor.submit(() -> runner.run(instance3));
        executor.shutdown();
        executor.awaitTermination(10, TimeUnit.MINUTES);

        IMap<String, Integer> map = instance1.getMap("my-distributed-map");
        System.out.println("Final value: " + map.get("key"));
    }

    private static void incrementNoLocking(HazelcastInstance instance) {
        IMap<String, Integer> map = instance.getMap("my-distributed-map");
        map.putIfAbsent("key", 0);
        for (int k = 0; k < 10_000; k++) {
            Integer value = map.get("key");
            value++;
            map.put("key", value);
        }
    }

    private static void incrementWithPessimisticLocking(HazelcastInstance instance) {
        IMap<String, Integer> map = instance.getMap("my-distributed-map");
        map.putIfAbsent("key", 0);
        for (int k = 0; k < 10_000; k++) {
            map.lock("key");
            try {
                Integer value = map.get("key");
                value++;
                map.put("key", value);
            } finally {
                map.unlock("key");
            }
        }
    }

    private static void incrementWithOptimisticLocking(HazelcastInstance instance) {
        IMap<String, Integer> map = instance.getMap("my-distributed-map");
        map.putIfAbsent("key", 0);
        for (int k = 0; k < 10_000; k++) {
            map.executeOnKey("key", new IncrementEntryProcessor());
        }
    }

    private static void resetCounter(HazelcastInstance instance) {
        IMap<String, Integer> map = instance.getMap("my-distributed-map");
        map.delete("key");
    }

    private interface ScenarioRunner {
        void run(HazelcastInstance instance);
    }

    private static class IncrementEntryProcessor implements EntryProcessor<String, Integer, Void> {
        @Override
        public Void process(Map.Entry<String, Integer> entry) {
            Integer value = entry.getValue();
            entry.setValue(value + 1);
            return null;
        }

        @Override
        public EntryProcessor<String, Integer, Void> getBackupProcessor() {
            return IncrementEntryProcessor.this;
        }
    }
}
