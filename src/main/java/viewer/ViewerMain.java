package viewer;

import common.Protocol;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.net.Socket;

public class ViewerMain {
    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.out.println("Usage: java viewer.ViewerMain <host-ip> <port> <pairing-code>");
            return;
        }
        String host = args[0];
        int port = Integer.parseInt(args[1]);
        int code = Integer.parseInt(args[2]);

        Socket sock = new Socket(host, port);
        sock.setTcpNoDelay(true);

        DataInputStream in = new DataInputStream(new BufferedInputStream(sock.getInputStream()));
        DataOutputStream out = new DataOutputStream(new BufferedOutputStream(sock.getOutputStream()));

        out.writeByte(Protocol.AUTH);
        out.writeInt(code);
        out.flush();

        ImagePanel panel = new ImagePanel();
        JFrame frame = new JFrame("Remote Support - Viewer");
        frame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        frame.setSize(1200, 700);
        frame.setLocationRelativeTo(null);
        frame.setContentPane(panel);
        frame.setVisible(true);

        attachInputHandlers(panel, out);

        Thread recv = new Thread(() -> receiveFrames(in, panel));
        recv.setDaemon(true);
        recv.start();
    }

    private static void receiveFrames(DataInputStream in, ImagePanel panel) {
        try {
            while (true) {
                byte type = in.readByte();
                if (type != Protocol.FRAME) continue;

                int len = in.readInt();
                byte[] data = in.readNBytes(len);

                BufferedImage img = ImageIO.read(new ByteArrayInputStream(data));
                if (img != null) {
                    panel.image = img;
                    panel.repaint();
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void attachInputHandlers(JComponent c, DataOutputStream out) {
        c.setFocusable(true);
        c.requestFocusInWindow();

        c.addMouseMotionListener(new MouseMotionAdapter() {
            @Override public void mouseMoved(MouseEvent e) { sendMove(e); }
            @Override public void mouseDragged(MouseEvent e) { sendMove(e); }

            private void sendMove(MouseEvent e) {
                float nx = (float) e.getX() / Math.max(1, c.getWidth());
                float ny = (float) e.getY() / Math.max(1, c.getHeight());
                try {
                    out.writeByte(Protocol.MOUSE_MOVE);
                    out.writeFloat(nx);
                    out.writeFloat(ny);
                    out.flush();
                } catch (IOException ignored) {}
            }
        });

        c.addMouseListener(new MouseAdapter() {
            @Override public void mousePressed(MouseEvent e) { sendBtn(e, true); }
            @Override public void mouseReleased(MouseEvent e) { sendBtn(e, false); }

            private void sendBtn(MouseEvent e, boolean down) {
                try {
                    out.writeByte(Protocol.MOUSE_BTN);
                    out.writeInt(e.getButton());
                    out.writeBoolean(down);
                    out.flush();
                } catch (IOException ignored) {}
            }
        });

        c.addMouseWheelListener(e -> {
            try {
                out.writeByte(Protocol.MOUSE_WHEEL);
                out.writeInt(e.getWheelRotation());
                out.flush();
            } catch (IOException ignored) {}
        });

        c.addKeyListener(new KeyAdapter() {
            @Override public void keyPressed(KeyEvent e) { sendKey(e, true); }
            @Override public void keyReleased(KeyEvent e) { sendKey(e, false); }

            private void sendKey(KeyEvent e, boolean down) {
                try {
                    out.writeByte(Protocol.KEY);
                    out.writeInt(e.getKeyCode());
                    out.writeBoolean(down);
                    out.flush();
                } catch (IOException ignored) {}
            }
        });
    }
}
