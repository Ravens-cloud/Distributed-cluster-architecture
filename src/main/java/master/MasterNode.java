package master;

import core.KeyValue;
import core.NetworkUtils;
import lombok.var;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class MasterNode {
    private volatile boolean isShutdown = false;
    private final Timer heartbeatTimer;
    private final List<Socket> slaveSockets;
    private final TaskScheduler scheduler;

    public MasterNode(int port, int expectedSlaves) throws IOException, InterruptedException {
        ServerSocket serverSocket = new ServerSocket();
        serverSocket.setReuseAddress(true);
        serverSocket.bind(new InetSocketAddress(port));

        this.slaveSockets = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch latch = new CountDownLatch(expectedSlaves);
        System.out.println("Master waiting for " + expectedSlaves + " slaves...");

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            // 优雅关闭：发送 EXIT
            for (Socket s : slaveSockets) {
                try {
                    NetworkUtils.sendObject(s, "EXIT");
                } catch (IOException ignored) {}
                NetworkUtils.closeQuietly(s);
            }
        }));

        // 接收 slave
        new Thread(() -> {
            try {
                while (slaveSockets.size() < expectedSlaves) {
                    Socket socket = serverSocket.accept();
                    System.out.println("Accepting new slave: " + socket.getRemoteSocketAddress());
                    slaveSockets.add(socket);
                    latch.countDown();
                }
                serverSocket.close();
            } catch (IOException e) {
                if (!serverSocket.isClosed()) e.printStackTrace();
            }
        }).start();

        latch.await();
        this.scheduler = new TaskScheduler(slaveSockets);

        // 心跳线程
        this.heartbeatTimer = new Timer("HeartbeatTimer", true);
        heartbeatTimer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                synchronized (slaveSockets) {
                    Iterator<Socket> it = slaveSockets.iterator();
                    while (it.hasNext()) {
                        Socket s = it.next();
                        try {
                            NetworkUtils.sendObject(s, "PING");
                            Object resp = NetworkUtils.receiveObject(s);
                            // 单向心跳，写失败则认为该 slave 挂了
                            NetworkUtils.sendObject(s, "PING");

                        } catch (Exception e) {
                            it.remove();
                            NetworkUtils.closeQuietly(s);
                            System.err.println("Slave failed heartbeat, removed: " + s);
                        }
                    }
                }
            }
        }, 10_000, 5_000);
    }

    public void cancelHeartbeat() {
        heartbeatTimer.cancel();
    }
    // 在 MasterNode 类中添加：
    public synchronized void shutdown() {
        if (isShutdown) return;
        isShutdown = true;

        // 关闭逻辑
        for (Socket socket : slaveSockets) {
            try {
                NetworkUtils.sendObject(socket, "EXIT");
                socket.close();  // 关闭 Socket
            } catch (IOException e) {
                System.err.println("Error closing socket: " + e.getMessage());
            }
        }
        slaveSockets.clear();
        System.out.println("Master shutdown complete.");
    }


    public List<KeyValue> runMapPhase(List<String> lines) throws Exception {
        ExecutorService pool = Executors.newCachedThreadPool();
        List<Future<List<KeyValue>>> futures = new ArrayList<>();

        for (String line : lines) {
            Socket slave = scheduler.getNextSlave();
            System.out.println("[DEBUG] Master sending line: " + line + " to " + slave);

            futures.add(pool.submit(() -> {
                synchronized (slave) {
                    // 这两行在同一个锁下，串行化
                    NetworkUtils.sendObject(slave, line);
                    @SuppressWarnings("unchecked")
                    List<KeyValue> kvs = (List<KeyValue>) NetworkUtils.receiveObject(slave);
                    System.out.println("[DEBUG] Master received " + kvs.size() + " items from " + slave);
                    return kvs;
                }
            }));
        }

        List<KeyValue> all = new ArrayList<>();
        for (Future<List<KeyValue>> f : futures) {
            try {
                all.addAll(f.get(30, TimeUnit.SECONDS));
            } catch (TimeoutException e) {
                System.err.println("Map task timeout, skipping batch");
            }
        }
        pool.shutdown();
        return all;
    }

    public List<KeyValue> runReducePhase(Map<String, List<String>> grouped) throws Exception {
        ExecutorService pool = Executors.newCachedThreadPool();
        List<Future<List<KeyValue>>> futures = new ArrayList<>();

        for (Map<String, List<String>> batch : createReduceBatches(grouped)) {
            Socket slave = scheduler.getNextSlave();
            futures.add(pool.submit(() -> {
                synchronized (slave) {
                    // 这两行在同一个锁下，串行化
                    NetworkUtils.sendObject(slave, batch);
                    @SuppressWarnings("unchecked")
                    List<KeyValue> kvs = (List<KeyValue>) NetworkUtils.receiveObject(slave);
                    return kvs;
                }
            }));
        }

        List<KeyValue> reducedAll = new ArrayList<>();
        for (Future<List<KeyValue>> f : futures) {
            reducedAll.addAll(f.get());
        }
        pool.shutdown();
        return reducedAll;
    }

    // 将reduce任务分批处理（每个批次包含多个键）
    private List<Map<String, List<String>>> createReduceBatches(Map<String, List<String>> grouped) {
        List<Map<String, List<String>>> batches = new ArrayList<>();
        Map<String, List<String>> current = new HashMap<>();
        int count = 0;
        for (var e : grouped.entrySet()) {
            current.put(e.getKey(), e.getValue());
            if (++count >= 10) {
                batches.add(current);
                current = new HashMap<>();
                count = 0;
            }
        }
        if (!current.isEmpty()) batches.add(current);
        return batches;
    }

    public static Map<String, List<String>> groupByKey(List<KeyValue> kvs) {
        Map<String, List<String>> grouped = new HashMap<>();
        for (KeyValue kv : kvs) {
            grouped.computeIfAbsent(kv.key, k -> new ArrayList<>()).add(kv.value);
        }
        return grouped;
    }


}