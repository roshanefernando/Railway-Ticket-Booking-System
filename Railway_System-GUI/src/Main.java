import java.awt.Dimension;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import javax.swing.*;

public class Main {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
                JFrame frame = new JFrame("Sri Lanka Railway Desk");
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.setContentPane(new RailwayBookingUI(new BookingManager(new FileManager())));
                Rectangle screen = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
                frame.setMinimumSize(new Dimension(Math.min(940, screen.width), Math.min(620, screen.height)));
                frame.setSize(Math.min(1160, screen.width), Math.min(750, screen.height));
                frame.setLocationRelativeTo(null);
                frame.setVisible(true);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(null, ex.getMessage(), "Startup error", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
