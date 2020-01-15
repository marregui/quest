package io.crate.cli.gui.widgets;

import io.crate.cli.gui.common.GUIFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.geom.*;
import java.io.Closeable;
import java.util.concurrent.TimeUnit;


public class InfiniteProgressPanel extends JPanel implements Closeable, Runnable, MouseListener {

    private static final int BAR_HEIGHT = 6;
    private static final int BAR_WIDTH = BAR_HEIGHT * 6;
    private static final int BAR_STRETCH = BAR_WIDTH / 3;
    private static final int BAR_COUNT = 11;
    private static final long REFRESH_MILLIS = 120L;
    private static final double FIXED_ANGLE = 2.0 * Math.PI / (1.0 * BAR_COUNT);
    private static final Color BACKGROUND_COLOR = new Color(242, 242, 242, 120);
    private static final Color [] BAR_COLORS = new Color[BAR_COUNT];
    static {
        for (int i=0; i < BAR_COUNT; i++) {
            int channel = 242 - 120 / (i + 1);
            BAR_COLORS[i] = new Color(channel, channel, channel);
        }
    }


    private volatile Thread animation;
    private Area[] ticker;


    public InfiniteProgressPanel() {
        setOpaque(false);
        addMouseListener(this);
    }

    public synchronized void start() {
        if (null == animation) {
            ticker = buildTicker();
            animation = new Thread(this);
            animation.start();
            setVisible(true);
            requestFocusInWindow();
            setFocusTraversalKeysEnabled(false);
        }
    }

    @Override
    public synchronized void close() {
        if (null != animation) {
            animation.interrupt();
            animation = null;
            setVisible(false);
            for (int i=0; i < ticker.length; i++) {
                ticker[i] = null;
            }
        }
    }

    @Override
    public void run() {
        Point2D.Double center = updateTicker();
        AffineTransform rotate = AffineTransform.getRotateInstance(FIXED_ANGLE, center.getX(), center.getY());
        while (false == Thread.currentThread().isInterrupted()) {
            for (int i = 0; i < ticker.length; i++) {
                ticker[i].transform(rotate);
            }
            repaint();
            try {
                TimeUnit.MILLISECONDS.sleep(REFRESH_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (null != animation) {
            Graphics2D g2 = (Graphics2D) g;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(BACKGROUND_COLOR);
            g2.fillRect(0, 0, getWidth(), getHeight());
            for (int i = 0; i < ticker.length; i++) {
                g2.setColor(BAR_COLORS[i % BAR_COLORS.length]);
                g2.fill(ticker[i]);
            }
        }
    }

    private static Area buildPrimitive() {
        Rectangle2D.Double body = new Rectangle2D.Double(BAR_HEIGHT / 2.0, 0, BAR_WIDTH, BAR_HEIGHT);
        Ellipse2D.Double head = new Ellipse2D.Double(0, 0, BAR_HEIGHT, BAR_HEIGHT);
        Ellipse2D.Double tail = new Ellipse2D.Double(BAR_WIDTH, 0, BAR_HEIGHT, BAR_HEIGHT);
        Area tick = new Area(body);
        tick.add(new Area(head));
        tick.add(new Area(tail));
        return tick;
    }

    private Area[] buildTicker() {
        Point2D.Double center = new Point2D.Double(getWidth() / 2.0, getHeight() / 2.0);
        Area[] ticker = new Area[BAR_COUNT];
        for (int i = 0; i < BAR_COUNT; i++) {
            ticker[i] = buildPrimitive();
        }
        return ticker;
    }

    private Point2D.Double updateTicker() {
        Point2D.Double center = new Point2D.Double(getWidth() / 2.0, getHeight() / 2.0);
        AffineTransform toCenter = AffineTransform.getTranslateInstance(center.getX(), center.getY());
        AffineTransform toBorder = AffineTransform.getTranslateInstance(BAR_STRETCH, -BAR_HEIGHT / 2.0);
        for (int i = 0; i < BAR_COUNT; i++) {
            AffineTransform rotate = AffineTransform.getRotateInstance(-1.0 * i * FIXED_ANGLE, center.getX(), center.getY());
            Area primitive = ticker[i];
            primitive.transform(toCenter);
            primitive.transform(toBorder);
            primitive.transform(rotate);
        }
        return center;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        // nothing
    }

    @Override
    public void mousePressed(MouseEvent e) {
        // nothing
    }

    @Override
    public void mouseReleased(MouseEvent e) {
        // nothing
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        // nothing
    }

    @Override
    public void mouseExited(MouseEvent e) {
        // nothing
    }

    public static void main(String[] args) {
        InfiniteProgressPanel spinner = new InfiniteProgressPanel();
        SQLResultsManager table = new SQLResultsManager();
        JFrame frame = GUIFactory.newFrame(
                "Spinner",
                80,
                80, table);
        frame.setGlassPane(spinner);
        frame.setVisible(true);
        spinner.start();
    }
}
