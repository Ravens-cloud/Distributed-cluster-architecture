package master;

import java.net.Socket;
import java.util.List;

// master/TaskScheduler.java（新增任务调度策略）
public class TaskScheduler {
    private final List<Socket> slaves;
    private int currentIndex = 0;

    public TaskScheduler(List<Socket> slaves) {
        this.slaves = slaves;
    }

    public Socket getNextSlave() {
        synchronized (this) {
            Socket selected = slaves.get(currentIndex);
            currentIndex = (currentIndex + 1) % slaves.size();
            return selected;
        }
    }
}