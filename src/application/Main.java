package application;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.Border;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;
import javafx.stage.Stage;
import javax.swing.*;
import javax.swing.event.*;

import java.awt.*;
import java.awt.event.*;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.time.temporal.Temporal;
import java.util.Iterator;
import java.util.Vector;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main extends Application {

    public static ExecutorService threadPool;
    public static Vector<Client> clients = new Vector<>();

    ServerSocket serverSocket;

    // 서버를 구동시켜 클라이언트의 연결을 기다리는 메소드
    public void startServer(String IP, int port) {
        try {
            serverSocket = new ServerSocket();
            serverSocket.bind(new InetSocketAddress(IP, port));
        } catch (Exception e) {
            e.printStackTrace();
            if (serverSocket.isClosed()) stopServer();
            return;
        }

        // 클라이언트가 접속할 때까지 계속 기다리는 thread
        Runnable thread = new Runnable() {
            @Override
            public void run() {
                while (true) {
                    try {
                        Socket socket = serverSocket.accept();
                        clients.add(new Client(socket));
                        System.out.println("[클라이언트 접속] "
                            + socket.getRemoteSocketAddress()
                            + ": " + Thread.currentThread().getName());
                    } catch (Exception e) {
                        if (!serverSocket.isClosed()) stopServer();
                        break;
                    }
                }
            }
        };
        threadPool = Executors.newCachedThreadPool();
        threadPool.submit(thread);
    }

    // 서버의 끄는 메소드 (자원 할당 해제)
    public void stopServer() {
        try {
            // 현재 작동 중인 모든 소켓 닫기
            Iterator<Client> iterator = clients.iterator();
            while (iterator.hasNext()) {
                Client client = iterator.next();
                client.socket.close();
                iterator.remove();
            }
            // 서버 소켓 객체 닫기
            if (serverSocket != null && !serverSocket.isClosed()) serverSocket.close();
            if (threadPool != null && !threadPool.isShutdown()) threadPool.shutdown();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // UI 생성하고 실질적으로 프로그램 작동시키는 메소드
    @Override
    public void start(Stage primaryStage) {

        BorderPane root = new BorderPane();
        root.setPadding(new Insets(5));

        HBox hbox = new HBox();
        hbox.setSpacing(5);

        TextField userName = new TextField();
        Dimension a = new Dimension();
        a.setSize(150, 150);
        userName.setPreferredSize(a);
        userName.setPromptText("닉네임을 입력하세요.");
        HBox.setHgrow(userName, Priority.ALWAYS);

        TextField IPTEXT = new TextField("127.0.0.1");
        TextField portText = new TextField("9876");
        portText.setPreferredSize(new Dimension(80, 80));

        hbox.getChildren().addAll(userName, IPTEXT, portText);
        root.setTop(hbox);

        TextArea textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setFont(new Font ("나눔고딕", 15));
        root.setCenter(textArea);

        TextField input = new TextField();
        input.setPrefWidth(Double.MAX_VALUE);
        input.setDisable(true);

        input.setOnAction(event-> {
            send(userName.getText() + ": " + input.getText() + "\n");
            input.setText("");
            input.requestFocus();
        });

        Button sendButton = new Button("보내기");
        sendButton.setDisable(true);

        sendButton.setOnAction(event-> {
            send(userName.getText() + ": " + input.getText() + "\n");
            input.setText("");
            input.requestFocus();
        });

        Button connectionButton = new Button("접속하기");
        connectionButton.setOnAction(event -> {
            if (connectionButton.getText().equals("접속하기")) {
                int port = 9876;
                try {
                    port = Integer.parseInt(portText.getText());
                } catch (Exception e) {
                    e.printStackTrace();
                }
                statClient(IPTEXT.getText(), port);
                Platform.runLater(()-> {
                    textArea.appendText("[ 채팅방 접속]\n");
                });
                connectionButton.setText("종료하기");
                input.setDisable(false);
                sendButton.setDisable(false);
                input.requestFocus();
            } else {
                stopClient();
                Platform.runLater(() -> {
                    textArea.appendText("[ 채팅방 퇴장 ]\n");
                });
                connectionButton.setText("접속하기");
                input.setDisable(true);
                sendButton.setDisable(true);
                sendButton.setDisable(true);
            }
        });

        BorderPane pane = new BorderPane();

        pane.setLeft(connectionButton);
        pane.getCenter(input);
        pane.setRight(sendButton);

        root.setBottom(pane);
        Scene scene = new Scene(root, 400, 400);
        primaryStage.setTitle("[ 채팅 클라이언트 ]");
        primaryStage.setScene(scene);
        primaryStage.setOnCloseRequest(event -> stopClient());
        primaryStage.show();

        connectionButton.requestFocus();

        //?

        Button toggleButton = new Button("시작하기");
        toggleButton.setMaxWidth(Double.MAX_VALUE);
        BorderPane.setMargin(toggleButton, new Insets(1,0,0,0));
        root.setBottom(toggleButton);

        String IP = "127.0.0.1";
        int port = 9876;

        toggleButton.setOnAction(event -> {
            if (toggleButton.getText().equals("시작하기")) {
                Platform.runLater(() -> {
                    String msg = String.format("[서버 시작]\n", IP, port);
                    textArea.appendText(msg);
                    toggleButton.setText("종료하기");
                });
            } else {
                stopServer();
                Platform.runLater(() -> {
                    String msg = String.format("[서버종료]\n", IP, port);
                    textArea.append(msg);
                    toggleButton.setText("시작하기");
                });
            }
        });
        Scene scene = new Scene(root, 400 400);
        primaryStage.setTitle("[채팅서버]");
        primaryStage.setOnCloseRequest(event->stopServer());
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // 프로그램 시작!
    public static void main(String[] args) {
        launch(args);
    }
}
