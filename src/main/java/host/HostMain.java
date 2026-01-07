package host;

import common.Protocol;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;

public class HostMain {
    public static void main(String[] args) throws Exception {
        int port = args.length >= 1 ? Integer.parseInt(args[0]) : 5000;

        int code = new SecureRandom().nextInt(900000) + 100000;
        String sessionLabel = "PORT " + port + " / CODE " + code;

        ConsentUI ui = new ConsentUI();
        ui.show(sessionLabel);

        System.out.println("Host listening on port: " + port);
        System.out.println("Pairing code: " + code);

        try (ServerSocket server = new ServerSocket(port);
             Socket sock = server.accept()) {

            sock.setTcpNoDelay(true);

            DataInputStream in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
            DataOutputStream out = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream()));

            if (!handleAuth(in, code)) {
                System.out.println("Auth failed. Closing.");
                return;
            }

            System.out.println("Viewer connected + authenticated.");

            Robot robot = new Robot();
            Rectangle screen = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());

            Thread inputThread = new Thread(() -> receiveAndApplyInput(in, robot, screen, ui));
            inputThread.setDaemon(true);
            inputThread.start();

            int fps = 10;
            long frameDelayMs = 1000L / fps;

            while (!ui.isStopped()) {
                if (!ui.isAllowed()) {
                    Thread.sleep(100);
                    continue;
                }

                BufferedImage img = robot.createScreenCapture(screen);

                ByteArrayOutputStream baos = new ByteArrayOutputStream(256_000);
                ImageIO.write(img, "jpg", baos);
                byte[] bytes = baos.toByteArray();

                out.writeByte(Protocol.FRAME);
                out.writeInt(bytes.length);
                out.write(bytes);
                out.flush();

                Thread.sleep(frameDelayMs);
            }
        }

        System.out.println("Host stopped.");
    }

    private static boolean handleAuth(DataInputStream in, int code) throws IOException {
        byte type = in.readByte();
        if (type != Protocol.AUTH) return false;
        int received = in.readInt();
        return received == code;
    }

    private static void receiveAndApplyInput(
            DataInputStream in, Robot robot, Rectangle screen, ConsentUI ui
    ) {
        try {
            while (!ui.isStopped()) {
                byte type = in.readByte();

                if (!ui.isAllowed()) {
                    drainMessage(in, type);
                    continue;
                }

                if (!ui.isControlEnabled()) {
                    drainMessage(in, type);
                    continue;
                }

                switch (type) {
                    case Protocol.MOUSE_MOVE -> {
                        float nx = in.readFloat();
                        float ny = in.readFloat();
                        int x = clamp(Math.round(nx * screen.width), 0, screen.width - 1);
                        int y = clamp(Math.round(ny * screen.height), 0, screen.height - 1);
                        robot.mouseMove(x, y);
                    }
                    case Protocol.MOUSE_BTN -> {
                        int button = in.readInt();
                        boolean down = in.readBoolean();
                        int mask = switch (button) {
                            case 1 -> InputEvent.BUTTON1_DOWN_MASK;
                            case 2 -> InputEvent.BUTTON2_DOWN_MASK;
                            case 3 -> InputEvent.BUTTON3_DOWN_MASK;
                            default -> InputEvent.BUTTON1_DOWN_MASK;
                        };
                        if (down) robot.mousePress(mask);
                        else robot.mouseRelease(mask);
                    }
                    case Protocol.MOUSE_WHEEL -> {
                        int amount = in.readInt();
                        robot.mouseWheel(amount);
                    }
                    case Protocol.KEY -> {
                        int keyCode = in.readInt();
                        boolean down = in.readBoolean();
                        if (down) robot.keyPress(keyCode);
                        else robot.keyRelease(keyCode);
                    }
                    default -> {
                        drainMessage(in, type);
                    }
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void drainMessage(DataInputStream in, byte type) throws IOException {
        switch (type) {
            case Protocol.MOUSE_MOVE -> { in.readFloat(); in.readFloat(); }
            case Protocol.MOUSE_BTN -> { in.readInt(); in.readBoolean(); }
            case Protocol.MOUSE_WHEEL -> { in.readInt(); }
            case Protocol.KEY -> { in.readInt(); in.readBoolean(); }
            default -> { }
        }
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
