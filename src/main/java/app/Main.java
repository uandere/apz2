package app;

import com.hazelcast.config.Config;
import com.hazelcast.core.Hazelcast;
import com.hazelcast.core.HazelcastInstance;

public class Main {
    public static void main(String[] args) {
        Config config = new Config();
        config.setClusterName("my-hazelcast-cluster");

        // starting three nodes
        HazelcastInstance instance1 = Hazelcast.newHazelcastInstance(config);
        HazelcastInstance instance2 = Hazelcast.newHazelcastInstance(config);
        HazelcastInstance instance3 = Hazelcast.newHazelcastInstance(config);

        System.out.println("Three Hazelcast instances have started in a single cluster.");

        // crating a map on the first node
        var distributedMap = instance1.getMap("my-distributed-map");
        // pushing a value to a map
        distributedMap.put("key", "value");

        // comparing the values between nodes
        System.out.println("Value from instance2: " + instance2.getMap("my-distributed-map").get("key"));
        System.out.println("Value from instance3: " + instance3.getMap("my-distributed-map").get("key"));
    }
}
