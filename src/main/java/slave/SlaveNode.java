// slave/SlaveNode.java
package slave;

import job.MapReduceJob;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;

public class SlaveNode {
    public SlaveNode(String host, int port, MapReduceJob job) {
        int retries = 0;
        while (retries < 3) {
            try {
                Socket sock = new Socket();
                sock.connect(new InetSocketAddress(host, port), 5_000);
                new Thread(new TaskExecutor(sock, job)).start();
                System.out.println("Slave connected to master successfully");
                return;
            } catch (ConnectException e) {
                System.err.println("Connect attempt " + (++retries) + " failed");
                try { Thread.sleep(2_000); } catch (InterruptedException ignored) {}
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        System.err.println("Failed to connect after 3 attempts");
    }
}
