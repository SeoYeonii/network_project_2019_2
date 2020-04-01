package application;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

// tcp 통신
public class Client {
    Socket socket;

    public Client(Socket socket) {
        this.socket = socket;
        receive();
    }

    // 반복적으로 클라이언트로부터 메시지를 받는 메소드
    public void receive() {
        Runnable thread = new Runnable() {
            @Override
            public void run() {
                try {
                    while (true) {
                        InputStream in = socket.getInputStream();
                        byte[] buffer = new byte[512];

                        int length = in.read(buffer);
                        if (length==-1) throw new IOException();
                        System.out.println("[메시지 수신 성공] "
                                + socket.getRemoteSocketAddress()
                                + ": " + Thread.currentThread().getName());

                        String msg = new String(buffer, 0, length, "UTF-8");
                        for (Client client: Main.clients) {
                            client.send(msg);
                        }
                    }
                } catch (Exception e) {
                    try {
                        System.out.println("[메시지 수신 오류] "
                                + socket.getRemoteSocketAddress()
                                + ": " + Thread.currentThread().getName());
                        Main.clients.remove(Client.this);
                        socket.close();
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }

                }
            }
        };
        Main.threadPool.submit(thread);
    }

    // 해당 클라이언트에게 메시지를 전송하는 메소드
    public void send(String msg) {
        Runnable thread = new Runnable() {
            @Override
            public void run() {
                try {
                    OutputStream out = socket.getOutputStream();
                    byte[] buffer = msg.getBytes("UTF-8");
                    out.write(buffer);
                    out.flush();
                } catch (Exception e) {
                    try {
                        System.out.println("[메시지 송신 오류]"
                            + socket.getRemoteSocketAddress()
                            + ": " + Thread.currentThread().getName());
                        Main.clients.remove(Client.this);
                        socket.close();
                    } catch (Exception ee) {
                        ee.printStackTrace();
                    }

                }
            }
        };
        Main.threadPool.submit(thread);

    }


}
