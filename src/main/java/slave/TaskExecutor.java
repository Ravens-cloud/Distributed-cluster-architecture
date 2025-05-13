// slave/TaskExecutor.java
package slave;

import core.KeyValue;
import core.NetworkUtils;
import job.MapReduceJob;

import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class TaskExecutor implements Runnable {
    private final Socket socket;
    private final MapReduceJob job;

    public TaskExecutor(Socket socket, MapReduceJob job) {
        this.socket = socket;
        this.job    = job;
        try {
            socket.setSoTimeout(10_000);
        } catch (Exception ignored) {}
    }

    @Override
    public void run() {
        try {
            while (!socket.isClosed() && !Thread.interrupted()) {
                Object task;
                try {
                    // 仅调用一次
                    task = NetworkUtils.receiveObject(socket);
                } catch (Exception e) {
                    // 超时或小故障，就继续等
                    continue;
                }

                System.out.println("[DEBUG] Slave got task: " + task);

                if (task instanceof String) {
                    String msg = (String) task;
                    if ("PING".equals(msg)) {
                        // 心跳，不回应
                    } else if ("EXIT".equals(msg)) {
                        break; // 退出
                    } else {
                        // Map 任务
                        @SuppressWarnings("unchecked")
                        List<KeyValue> kvs = job.map(msg);
                        System.out.println("[DEBUG] Slave map result size: " + kvs.size());
                        NetworkUtils.sendObject(socket, kvs);
                    }
                } else if (task instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, List<String>> grouped = (Map<String, List<String>>) task;
                    List<KeyValue> reducedList = new ArrayList<>();
                    for (Map.Entry<String, List<String>> e : grouped.entrySet()) {
                        reducedList.add(job.reduce(e.getKey(), e.getValue()));
                    }
                    System.out.println("[DEBUG] Slave reduce result size: " + reducedList.size());
                    NetworkUtils.sendObject(socket, reducedList);
                } else {
                    System.err.println("[WARN] Unknown task type: " + task.getClass());
                }
            }
        } catch (Exception e) {
            System.err.println("Task execution error: " + e.getMessage());
        } finally {
            NetworkUtils.closeQuietly(socket);
        }
    }
}
