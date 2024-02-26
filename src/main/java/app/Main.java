package app;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class Main {
    public static void main(String[] args) {
        Config config = new Config();
        config.setClusterName("my-hazelcast-cluster");

        HazelcastInstance instance1 = Hazelcast.newHazelcastInstance(config);
        HazelcastInstance instance2 = Hazelcast.newHazelcastInstance(config);
        HazelcastInstance instance3 = Hazelcast.newHazelcastInstance(config);

        System.out.println("Three Hazelcast instances have started in a single cluster.");

        var distributedMap = instance1.getMap("my-distributed-map");
        for (int i = 0; i != 1000; i++) {
            distributedMap.put(i, String.valueOf(i));
        }

    }
}
