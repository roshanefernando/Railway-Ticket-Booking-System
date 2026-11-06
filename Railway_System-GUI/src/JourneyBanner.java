import java.awt.*;
import java.awt.geom.*;
import javax.swing.JPanel;

final class JourneyBanner extends JPanel {
    JourneyBanner(LayoutManager layout) {
        super(layout);
        setOpaque(false);
    }

    @Override protected void paintComponent(Graphics graphics) {
        Graphics2D g = DeskStyle.smooth(graphics);
        int w = getWidth(), h = getHeight();
        g.clip(new RoundRectangle2D.Double(0,0,w,h,24,24));
        g.setPaint(new GradientPaint(0,0,new Color(0x182843),w,h,new Color(0x443D65)));
        g.fillRect(0,0,w,h);
        double scale = Math.min(w*.45/460.0,h/170.0);
        g.translate(w-460*scale,h-170*scale);
        g.scale(scale,scale);
        g.setColor(new Color(0xE99E87));
        g.fillOval(298,12,74,74);
        g.setColor(new Color(0xF5BF99));
        g.fillArc(298,12,74,74,0,180);
        g.setColor(new Color(0x6B6689));
        g.fillPolygon(new int[]{0,72,113,162,202,249,293,347,402,460,460,0},
                new int[]{170,100,106,72,94,60,72,49,76,58,170,170},12);
        g.setColor(new Color(0x524F76));
        g.fillPolygon(new int[]{0,78,120,215,264,322,386,460,460,0},
                new int[]{170,129,135,88,105,83,111,77,170,170},10);
        g.setColor(new Color(0x393C60));
        g.fillPolygon(new int[]{0,80,163,235,295,377,460,460,0},
                new int[]{169,145,149,120,140,118,126,170,170},9);
        g.translate(0,4);
        g.rotate(-.055,305,114);
        g.setColor(new Color(0x202C46));
        for(int x=50;x<470;x+=54) {
            g.fillRect(x,114,7,65);
            g.setStroke(new BasicStroke(4));
            g.drawLine(x+4,145,x+51,116);
        }
        g.fillRect(38,113,430,6);
        g.setColor(new Color(0xC99C96)); g.fillRect(38,112,430,2);
        for(int x=110;x<=302;x+=64) coach(g,x,x==302);
        g.dispose();
    }

    private void coach(Graphics2D g, int x, boolean engine) {
        Path2D shell = new Path2D.Double();
        shell.moveTo(x,90); shell.quadTo(x,87,x+4,87);
        shell.lineTo(x+(engine?43:58),87);
        if(engine) { shell.quadTo(x+50,87,x+59,98); shell.lineTo(x+65,104); }
        else shell.quadTo(x+61,87,x+61,91);
        shell.lineTo(x+(engine?65:61),107); shell.lineTo(x,107); shell.closePath();
        g.setColor(new Color(0xF9DFBC)); g.fill(shell);
        Graphics2D body=(Graphics2D)g.create();
        body.clip(shell);
        body.setColor(new Color(0xD98070)); body.fillRect(x,99,67,5);
        body.setColor(new Color(0x333D58));
        for(int i=0;i<5;i++) body.fillRoundRect(x+4+9*i,91,7,6,2,2);
        if(engine) body.fillPolygon(new int[]{x+46,x+53,x+61,x+49}, new int[]{90,90,98,98},4);
        body.dispose();
        g.setColor(new Color(0x1D263B)); g.fillRoundRect(x+4,106,52,3,3,3);
        g.fillOval(x+8,106,6,6); g.fillOval(x+47,106,6,6);
    }
}
