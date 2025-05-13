import core.KeyValue;
import master.MasterNode;
import slave.SlaveNode;
import job.WordCountJob;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class App {
    private static MasterNode master; // 共享 Master 实例

    public static void main(String[] args) throws InterruptedException {
        // 1. 读取配置
        Properties props = new Properties();
        try (InputStream is = Files.newInputStream(Paths.get("config.properties"))) {
            props.load(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        int port = Integer.parseInt(props.getProperty("master.port", "9000"));
        int slaves = Integer.parseInt(props.getProperty("slaves.count", "2"));

        // 2. 启动 Master 线程（通过共享 master 实例）
        Thread masterThread = new Thread(() -> {
            try {
                master = new MasterNode(port, slaves);
                // 注册 JVM 关闭钩子
                Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                    master.shutdown();
                    master.cancelHeartbeat();
                    System.out.println("System shutdown gracefully");
                }));

                // 3. 加载输入数据
                InputStream in = App.class.getClassLoader().getResourceAsStream("input.txt");
                BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                List<String> lines = reader.lines().collect(Collectors.toList());

                // 4. 执行 MapReduce
                List<KeyValue> mapped = master.runMapPhase(lines);
                Map<String, List<String>> grouped = MasterNode.groupByKey(mapped);
                List<KeyValue> reduced = master.runReducePhase(grouped);

                // 5. 输出结果
                Path outputPath = Paths.get("src/main/resources/output.txt");
                try (BufferedWriter writer = Files.newBufferedWriter(outputPath)) {
                    for (KeyValue kv : reduced) {
                        writer.write(String.format("%-15s => %s%n", kv.key, kv.value));
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        masterThread.start();

        // 6. 启动多个 Slave 节点
        TimeUnit.SECONDS.sleep(1); // 等待 Master 初始化完毕
        for (int i = 0; i < slaves; i++) {
            new SlaveNode("localhost", port, new WordCountJob());
        }

        // 7. 等待任务线程执行完毕
        masterThread.join();

        // 8. 主动关闭资源并退出
        if (master != null) {
            master.shutdown();
            master.cancelHeartbeat();
        }

        System.out.println("All tasks done, exiting.");
        System.exit(0); // 可选，但确保所有线程退出
    }
}
