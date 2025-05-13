// core/NetworkUtils.java（修正后的网络工具类）
package core;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class NetworkUtils {
    // 缓存每个 Socket 对应的流对象
    private static final ConcurrentMap<Socket, ObjectOutputStream> outputStreamCache = new ConcurrentHashMap<>();
    private static final ConcurrentMap<Socket, ObjectInputStream>  inputStreamCache  = new ConcurrentHashMap<>();

    public static Object receiveObject(Socket socket) throws IOException, ClassNotFoundException {
        // 使用 computeIfAbsent 避免重复创建流对象
        ObjectInputStream in = inputStreamCache.computeIfAbsent(socket, s -> {
            try {
                return new ObjectInputStream(s.getInputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        return in.readObject();
    }

    public static void sendObject(Socket socket, Object obj) throws IOException {
        ObjectOutputStream out = outputStreamCache.computeIfAbsent(socket, s -> {
            try {
                return new ObjectOutputStream(s.getOutputStream());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
        // 对单个输出流加锁，避免多线程并发写入冲突
        synchronized (out) {
            out.writeObject(obj);
            out.flush();
        }
    }

    public static void closeQuietly(Socket socket) {
        try {
            ObjectInputStream in = inputStreamCache.remove(socket);
            if (in != null) in.close();
            ObjectOutputStream out = outputStreamCache.remove(socket);
            if (out != null) out.close();
            socket.close();
        } catch (IOException ignored) {}
    }
}