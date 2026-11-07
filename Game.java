import javax.swing.*;
import java.awt.*;

//Represents 2D coordinates/ velocities
class Vector {
    double x;
    double y;

    // initializes vector one dim. keeper
    public Vector(double x) {
        this.x = x;
    }

    //same but two dim. for ball
    public Vector(double x, double y) {
        this.x = x;
        this.y = y;
    }
}

// Defines width and height
class Size {
    double width;
    double height;

    public Size(double width, double height) {
        this.width = width;
        this.height = height;
    }
}

//represents the moving ball 
class Ball {
    Vector position;
    Vector velocity;

    double vx_min = -15;
    double vx_max = 15;
    double vy_min = 20;
    double vy_max = 40;

    public Ball(Vector position, Vector velocity) 
    {
        this.position = position;
        this.velocity = velocity;
    }
}

// same but goalkeeper
class Keeper {
    Vector position;
    Vector velocity;

    final double saveRange = 6.0;

    public Keeper(Vector position, Vector velocity) {
        this.position = position;
        this.velocity = velocity;
    }
}

//game physics and state 
class World 
{
    Size pitch = new Size(40, 80);

    double goal_width = 24;
    double startBallY = 45;

    Ball ball = new Ball( new Vector(pitch.width / 2, startBallY), new Vector(0, 0) );
    Keeper keeper = new Keeper( new Vector(pitch.width / 2, 0), new Vector(0) );

    Vector targetPoint;

    // normalization 1 to -1, we love small easy to work numbers 
    public double convertToMinusOneOne(double n, double n1, double n2) {
        return 2 * (n - n1) / (n2 - n1) - 1;
    }

    // extracts normalized state for nn
    public double[] getInputs() {
        double goalTop = (pitch.width - goal_width) / 2.0;
        double goalBottom = goalTop + goal_width;

        double ball_x = convertToMinusOneOne(ball.position.x, goalTop, goalBottom);
        double ball_y = convertToMinusOneOne(ball.position.y, 0, startBallY);

        double ball_vx = convertToMinusOneOne(ball.velocity.x, ball.vx_min, ball.vx_max);
        double ball_vy = convertToMinusOneOne(ball.velocity.y, ball.vy_min, ball.vy_max);

        double keeper_x = convertToMinusOneOne(keeper.position.x, goalTop, goalBottom);

        return new double[]{ball_x, ball_y, ball_vx, ball_vy, keeper_x};
    }

//Randomizes ball position & target direction
// every game diffrent => learning , not memorizing
    public void reset() {
        double goalTop = (pitch.width - goal_width) / 2.0;
        double goalBottom = goalTop + goal_width;

        ball.position.x = pitch.width / 2;
        ball.position.y = startBallY;

        ball.velocity.y = Math.random() * (ball.vy_max - ball.vy_min) + ball.vy_min;
        double time = ball.position.y / ball.velocity.y;
        double margin = 1.0;
        double targetX = Math.random() * ((goalBottom - margin) - (goalTop + margin)) + (goalTop + margin);

        ball.velocity.x = (targetX - ball.position.x) / time;
        keeper.position.x = pitch.width / 2;
        keeper.velocity.x = 0;
    }

     // calculates fianl position
     public void simulate() {
        double time = ball.position.y / ball.velocity.y;
        targetPoint = new Vector(ball.position.x + ball.velocity.x * time, 0);
        ball.position = targetPoint;
    }

}

// sets up fancy GIU ;)
class Screen {
    JFrame frame;
    public SimulationPanel panel;
    JButton shootAgainButton;

    // initialize necessary graphics pieces
    public Screen() {
        frame = new JFrame("Goalkeeper Simulation");
        panel = new SimulationPanel();
        shootAgainButton = new JButton("Shoot Again");

        frame.setLayout(new BorderLayout());
        frame.add(panel, BorderLayout.CENTER);
        frame.add(shootAgainButton, BorderLayout.SOUTH);

        frame.pack();
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLocationRelativeTo(null);
    }

// press button -> shoot 
    public void setShootAgainAction(Runnable action) {
        shootAgainButton.addActionListener(e -> action.run());
    }

    //shows the GUI
    public void showScreen() {
        frame.setVisible(true);
    }
}

//handles drawing adn animation ( this is were all the budget went, literrally harder than the AI part :( )

class SimulationPanel extends JPanel 
{
    final double WW = 80, WH = 40, GT = 8, GB = 32, SAVE = 6;
    final int ML = 100, MT = 50, PW = 800, PH = 400;
    final double SX = PW / WW, SY = PH / WH;

    double bx, by, bvx, bvy, sx, sy, kx, kv;
    Timer timer;
    boolean visible = true, done = false;
    String result = "";

    // timer and panel setp 
    public SimulationPanel() 
    {
        setPreferredSize(new Dimension(1000, 540));
        setBackground(new Color(20, 100, 35));

        timer = new Timer(16, e -> {update();repaint();});
    }
    //reset variables for new siulation
    public void startSimulation(double x, double y, double vx, double vy, double keeperVelocity) {
        timer.stop();

        bx = sx = x;
        by = sy = y;
        bvx = vx;
        bvy = vy;
        kv = keeperVelocity;

        kx = WH / 2;
        visible = true;
        done = false;
        result = "";

        timer.start();
    }

    //updates physics and checks goal conditions
    void update() 
    {
        if (done) return;

        double oldX = bx;
        double oldY = by;
        bx += bvx;
        by -= bvy;

        if (by <= 0) 
        {
            double t = oldY / bvy;
            bx = oldX + bvx * t;
            by = 0;
        }
        kx += kv;
        kx = Math.max(GT + SAVE / 2, Math.min(GB - SAVE / 2, kx));

        if (by <= 0) 
            {
            timer.stop();
            done = true;

            boolean inside = bx >= GT && bx <= GB;
            boolean saved = inside && Math.abs(bx - kx) <= SAVE / 2;

            if (!inside) 
                {
                result = "OUT";
                visible = false;
            } 
            else if (saved) 
            {
                result = "SAVE";
                bx = sx;
                by = sy;
            } 
            else   
            {
                result = "GOAL";
                visible = false;
            }

            System.out.println(result);
        }
    }

// Converts world Y coordinate to screen X
    int px(double worldY) {
        return (int) (ML + worldY * SX);
    }
//converts world X coordinate to screen Y
    int py(double worldX) {
        return (int) (MT + worldX * SY);
    }

// Draws pitch, goal, ball, and keeper

    protected void paintComponent(Graphics g) 
    {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g;

        for (int i = 0; i < 8; i++) 
        {
            g.setColor(i % 2 == 0 ? new Color(35, 130, 50) : new Color(25, 115, 45));
            g.fillRect(ML + i * PW / 8, MT, PW / 8, PH);
        }

        g.setColor(Color.WHITE);
        g.drawRect(ML, MT, PW, PH);
        g.drawLine(px(WW / 2), MT, px(WW / 2), MT + PH);

        int gx = px(0);
        int top = py(GT);
        int bot = py(GB);
        int d = 45;

        g2.setColor(new Color(0, 0, 0, 80));
        g2.fillRect(gx - d - 4, top - 4, d + 8, bot - top + 8);
        g2.setColor(new Color(255, 255, 255, 100));

        for (int x = gx - d; x <= gx; x += 10) 
        {
            g2.drawLine(x, top, x, bot);
        }

        for (int y = top; y <= bot; y += 12) 
        {
            g2.drawLine(gx - d, y, gx, y);
        }

        g2.setColor(Color.WHITE);
        g2.setStroke(new BasicStroke(6));
        g2.drawLine(gx, top, gx, bot);
        g2.drawLine(gx, top, gx - d, top);
        g2.drawLine(gx, bot, gx - d, bot);
        g2.drawLine(gx - d, top, gx - d, bot);

        g2.setStroke(new BasicStroke(1));
        if (visible) 
        {
            g.setColor(Color.RED);
            g.fillOval(px(by) - 8, py(bx) - 8, 16, 16);
        }

        int kw = 22;
        int kh = (int) (SAVE * SY * 1.25);
        g.setColor(Color.BLUE);
        g.fillRect(gx - kw / 2, py(kx) - kh / 2, kw, kh);
        g.setFont(new Font("Arial", Font.BOLD, 18));
        g.setColor(Color.WHITE);
        g.drawString(String.format("Keeper velocity: %.2f", kv), ML, MT + PH + 30);

        if (done) 
        {
            g.setFont(new Font("Arial", Font.BOLD, 40));
            g.setColor(result.equals("SAVE") ? Color.CYAN : result.equals("GOAL") ? Color.RED : Color.LIGHT_GRAY);
            g.drawString(result, ML + PW / 2 - 60, MT + PH / 2);
        }
    }
}