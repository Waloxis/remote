package host;

import javax.swing.*;
import java.awt.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class ConsentUI {
    private final AtomicBoolean allowed = new AtomicBoolean(false);
    private final AtomicBoolean controlEnabled = new AtomicBoolean(false);
    private final AtomicBoolean stopped = new AtomicBoolean(false);

    public void show(String sessionLabel) {
        SwingUtilities.invokeLater(() -> {
            JFrame f = new JFrame("Remote Support - Host");
            f.setAlwaysOnTop(true);
            f.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);

            JLabel status = new JLabel(statusText(sessionLabel), SwingConstants.CENTER);
            status.setFont(status.getFont().deriveFont(Font.BOLD, 14f));

            JButton allow = new JButton("Allow Viewer");
            JButton deny = new JButton("Deny Viewer");
            JButton toggleControl = new JButton("Enable Control");
            JButton stop = new JButton("STOP");

            toggleControl.setEnabled(false);

            allow.addActionListener(e -> {
                allowed.set(true);
                deny.setEnabled(false);
                allow.setEnabled(false);
                toggleControl.setEnabled(true);
                status.setText(statusText(sessionLabel));
            });

            deny.addActionListener(e -> {
                allowed.set(false);
                controlEnabled.set(false);
                stopped.set(true);
                status.setText(statusText(sessionLabel));
                f.dispose();
            });

            toggleControl.addActionListener(e -> {
                boolean next = !controlEnabled.get();
                controlEnabled.set(next);
                toggleControl.setText(next ? "Disable Control" : "Enable Control");
                status.setText(statusText(sessionLabel));
            });

            stop.addActionListener(e -> {
                stopped.set(true);
                status.setText(statusText(sessionLabel));
                f.dispose();
            });

            JPanel buttons = new JPanel(new GridLayout(2, 2, 8, 8));
            buttons.add(allow);
            buttons.add(deny);
            buttons.add(toggleControl);
            buttons.add(stop);

            JPanel root = new JPanel(new BorderLayout(10, 10));
            root.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
            root.add(status, BorderLayout.NORTH);
            root.add(buttons, BorderLayout.CENTER);

            f.setContentPane(root);
            f.setSize(360, 170);
            f.setLocationRelativeTo(null);
            f.setVisible(true);
        });
    }

    private String statusText(String sessionLabel) {
        return "<html><div style='text-align:center;'>"
                + "Session: " + sessionLabel + "<br/>"
                + "Viewer Allowed: " + (allowed.get() ? "YES" : "NO") + "<br/>"
                + "Control Enabled: " + (controlEnabled.get() ? "YES" : "NO") + "<br/>"
                + "</div></html>";
    }

    public boolean isAllowed() { return allowed.get(); }
    public boolean isControlEnabled() { return controlEnabled.get(); }
    public boolean isStopped() { return stopped.get(); }
}
