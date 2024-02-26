package app;

import com.hazelcast.config.Config;
import com.hazelcast.config.QueueConfig;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.collection.IQueue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {
    public static void main(String[] args) {
        Config config = new Config();
        QueueConfig queueConfig = new QueueConfig();
        queueConfig.setName("my-bounded-queue");
        queueConfig.setMaxSize(10);
        config.addQueueConfig(queueConfig);

        HazelcastInstance instance = Hazelcast.newHazelcastInstance(config);

        ExecutorService executor = Executors.newFixedThreadPool(3);

        executor.submit(() -> {
            IQueue<Integer> queue = instance.getQueue("my-bounded-queue");
            for (int i = 1; i <= 100; i++) {
                try {
                    queue.put(i);
                    System.out.println("Produced: " + i);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        });

        for (int i = 0; i < 2; i++) {
            executor.submit(() -> {
                IQueue<Integer> queue = instance.getQueue("my-bounded-queue");
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        Integer value = queue.take();
                        System.out.println("Consumed by " + Thread.currentThread().getName() + ": " + value);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
            });
        }

        executor.shutdown();
    }
}
