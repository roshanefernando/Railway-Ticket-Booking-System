import java.awt.*;
import java.awt.geom.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.plaf.basic.BasicComboBoxUI;
import javax.swing.border.Border;

final class DeskStyle {
    static final Color NIGHT = new Color(0x203252);
    static final Color PAPER = new Color(0xF4F6FA);
    static final Color EDGE = new Color(0xDAE1EB);
    static final Color AMBER = new Color(0xFFC27D);
    static final Color LAVENDER = new Color(0xE7E7FA);

    private DeskStyle() {}

    static Graphics2D smooth(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics.create();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        return g;
    }

    static Color mix(Color a, Color b, float t) {
        return new Color(Math.round(a.getRed() + (b.getRed() - a.getRed()) * t),
                Math.round(a.getGreen() + (b.getGreen() - a.getGreen()) * t),
                Math.round(a.getBlue() + (b.getBlue() - a.getBlue()) * t));
    }

    static JPanel card(LayoutManager layout, int padding) {
        JPanel panel = new JPanel(layout) {
            @Override protected void paintComponent(Graphics graphics) {
                Graphics2D g = smooth(graphics);
                g.setColor(getBackground());
                g.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g.setColor(EDGE);
                g.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 16, 16);
                g.dispose();
            }
        };
        panel.setOpaque(false);
        panel.setBackground(Color.WHITE);
        panel.setBorder(new EmptyBorder(padding, padding, padding, padding));
        return panel;
    }

    static Border inputBorder() {
        return new EmptyBorder(5, 9, 5, 9) {
            @Override public void paintBorder(Component c, Graphics graphics, int x, int y, int w, int h) {
                Graphics2D g = smooth(graphics);
                g.setColor(c.hasFocus() ? new Color(0x6673A8) : new Color(0xB7C1D3));
                g.drawRoundRect(x, y, w-1, h-1, 9, 9);
                g.dispose();
            }
        };
    }

    static void combo(JComboBox<?> box) {
        box.setUI(new BasicComboBoxUI() {
            @Override protected JButton createArrowButton() {
                JButton arrow = new JButton() {
                    @Override protected void paintComponent(Graphics graphics) {
                        Graphics2D g = smooth(graphics);
                        int x = getWidth()/2, y = getHeight()/2;
                        g.setColor(NIGHT);
                        g.setStroke(new BasicStroke(1.6f));
                        g.drawPolyline(new int[]{x-4,x,x+4}, new int[]{y-2,y+2,y-2}, 3);
                        g.dispose();
                    }
                };
                arrow.setOpaque(false);
                arrow.setBorder(null);
                arrow.setFocusable(false);
                return arrow;
            }
        });
        box.setBackground(Color.WHITE);
        box.setBorder(inputBorder());
    }

    static final class Pages extends JPanel {
        private float progress = 1;
        private long start;
        private final Timer timer = new Timer(16, e -> tick());

        Pages(CardLayout layout) { super(layout); }

        void reveal() {
            if (!isShowing()) return;
            start = System.nanoTime();
            progress = 0;
            timer.restart();
            repaint();
        }

        private void tick() {
            progress = Math.min(1f, (System.nanoTime() - start) / 190_000_000f);
            if (progress >= 1 || !isShowing()) timer.stop();
            repaint();
        }

        @Override protected void paintChildren(Graphics graphics) {
            if (progress >= 1 || isPaintingForPrint()) { super.paintChildren(graphics); return; }
            Graphics2D g = smooth(graphics);
            float eased = 1 - (1-progress)*(1-progress);
            g.translate(0, Math.round((1-eased)*7));
            g.setComposite(AlphaComposite.SrcOver.derive(0.6f + 0.4f*eased));
            super.paintChildren(g);
            g.dispose();
        }

        @Override public void removeNotify() { timer.stop(); progress = 1; super.removeNotify(); }
    }

    static final class Action extends JButton {
        private final boolean seat;
        private final Timer timer;
        private Color from, to, shown;
        private long start;

        Action(String text, Color base, boolean seat) {
            super(text);
            this.seat = seat;
            setUI(new BasicButtonUI());
            setContentAreaFilled(false);
            setFocusPainted(false);
            setOpaque(false);
            setRolloverEnabled(true);
            setBorder(new EmptyBorder(seat ? 2 : 9, seat ? 2 : 14, seat ? 5 : 9, seat ? 2 : 14));
            setFont(new Font("SansSerif", Font.BOLD, 12));
            setForeground(Color.BLACK);
            setBackground(base);
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            shown = to = base;
            timer = new Timer(16, e -> tick());
            getModel().addChangeListener(e -> restyle());
            addPropertyChangeListener("background", e -> restyle());
            addPropertyChangeListener("enabled", e -> restyle());
        }

        private void restyle() {
            Color base = getBackground();
            Color next = !isEnabled() ? mix(base, Color.WHITE, .67f)
                    : getModel().isPressed() ? mix(base, NIGHT, .18f)
                    : getModel().isRollover() ? mix(base, NIGHT, .08f) : base;
            if (next.equals(to)) return;
            from = shown;
            to = next;
            if (!isShowing()) { shown = next; timer.stop(); repaint(); return; }
            start = System.nanoTime();
            timer.restart();
        }

        private void tick() {
            float t = Math.min(1f, (System.nanoTime() - start) / 150_000_000f);
            shown = mix(from, to, t);
            if (t >= 1 || !isShowing()) { shown = to; timer.stop(); }
            repaint();
        }

        @Override protected void paintComponent(Graphics graphics) {
            Graphics2D g = smooth(graphics);
            int w = getWidth()-3, h = getHeight()-3;
            int radius = seat ? 12 : 15;
            g.setColor(shown);
            g.fillRoundRect(1, 1, w, h, radius, radius);
            g.setColor(mix(shown, NIGHT, seat ? .24f : .11f));
            g.drawRoundRect(1, 1, w, h, radius, radius);
            if (seat) {
                g.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g.drawLine(7, h-4, w-5, h-4);
            }
            if (hasFocus()) {
                g.setStroke(new BasicStroke(1.6f));
                g.setColor(new Color(0x344CA2));
                g.drawRoundRect(3, 3, w-4, h-4, radius-3, radius-3);
            }
            g.dispose();
            super.paintComponent(graphics);
        }

        @Override public void removeNotify() { timer.stop(); shown = to; super.removeNotify(); }
    }

    enum Symbol implements Icon {
        BRAND, TIMETABLE, SEAT, SEARCH, RECORDS;
        public int getIconWidth() { return this == BRAND ? 38 : 16; }
        public int getIconHeight() { return getIconWidth(); }

        public void paintIcon(Component c, Graphics graphics, int x, int y) {
            Graphics2D g = smooth(graphics);
            g.translate(x,y);
            g.scale(getIconWidth()/20.0, getIconHeight()/20.0);
            g.setColor(NIGHT);
            g.setStroke(new BasicStroke(1.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            switch (this) {
                case BRAND:
                    g.fillRoundRect(0,0,20,20,6,6);
                    g.setColor(AMBER);
                    g.fillRoundRect(5,3,10,12,4,4);
                    g.setColor(NIGHT);
                    g.fillRoundRect(6,5,8,5,2,2);
                    g.fillOval(7,12,2,2); g.fillOval(11,12,2,2);
                    g.setColor(AMBER); g.drawLine(7,15,5,17); g.drawLine(13,15,15,17);
                    break;
                case TIMETABLE:
                    g.drawRoundRect(2,4,16,13,3,3); g.drawLine(3,8,17,8);
                    g.drawLine(6,3,6,6); g.drawLine(14,3,14,6); g.drawLine(6,12,10,12); break;
                case SEAT:
                    g.drawRoundRect(5,2,10,10,3,3); g.drawRoundRect(3,12,14,4,2,2);
                    g.drawLine(5,16,5,18); g.drawLine(15,16,15,18); break;
                case SEARCH:
                    g.drawOval(3,3,10,10); g.drawLine(12,12,17,17); break;
                case RECORDS:
                    g.drawRoundRect(3,3,14,14,2,2); g.drawLine(6,7,14,7);
                    g.drawLine(6,10,14,10); g.drawLine(6,13,11,13); break;
            }
            g.dispose();
        }
    }
}
