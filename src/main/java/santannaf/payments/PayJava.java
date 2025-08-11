package santannaf.payments;

import com.sun.net.httpserver.HttpServer;
import santannaf.payments.redis.read.RedisReadClient;
import santannaf.payments.redis.write.RedisWriteClient;

import java.io.IOException;
import java.net.InetSocketAddress;

public class PayJava {
    public static void main(String[] args) throws IOException, InterruptedException {
        int serverPort = 8080;
        String host = "0.0.0.0";
        int backlog = 4096;

        HttpServer server = HttpServer.create(new InetSocketAddress(host, serverPort), backlog);

        server.createContext("/", new PathHandler().getHandlers());
//        var vf = Thread.ofVirtual().name("http-vt-", 0).factory();
//        ExecutorService vtExec = Executors.newThreadPerTaskExecutor(vf);
//        server.setExecutor(vtExec);

        RedisWriteClient.getInstance();
        RedisReadClient.getInstance();
        Warmup.getInstance();
        PaymentsRepository.getInstance();
        ProcessWorker.getInstance();
        PaymentProcessorClient.getInstance();

        server.start();

        Thread.currentThread().join();

        System.out.println("Java Server Revolts on http://" + host + ":" + serverPort);
    }
}
