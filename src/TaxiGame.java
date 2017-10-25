import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import javax.imageio.ImageIO;
import javax.swing.JFrame;
import javax.swing.JPanel;

public class TaxiGame extends JPanel {
	private static final long serialVersionUID = -8396459457708163217L;
	public static JFrame frame;

	public static void main(String[] args) {
		frame = new JFrame("Taxi Game");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		TaxiGame panel = new TaxiGame();
		frame.getContentPane().add(panel);

		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setVisible(true);

		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				panel.tick();
				panel.repaint();
			}
		}, 0, 1000 / 60);
	}

	public static final int S_WIDTH = 800, S_HEIGHT = 600, TILE_SIZE = 64;
	public static boolean[][] tracks;
	BufferedImage tracksImg;

	public TaxiGame() {
		tracks = new boolean[12][9];
		int[][] literalTrack = new int[][] {
			{0, 0, 0, 1, 1, 1, 0, 0, 0},
			{0, 0, 1, 1, 0, 1, 0, 0, 0},
			{0, 0, 1, 0, 1, 1, 1, 0, 0},
			{0, 0, 1, 0, 1, 0, 1, 1, 1},
			{0, 1, 1, 0, 1, 0, 1, 0, 1},
			{0, 1, 0, 0, 1, 1, 1, 1, 1},
			{0, 1, 0, 0, 0, 0, 1, 0, 0},
			{0, 1, 1, 1, 1, 0, 1, 0, 0},
			{0, 0, 1, 0, 1, 1, 1, 1, 0},
			{0, 0, 1, 1, 1, 0, 0, 1, 0},
			{0, 0, 0, 0, 1, 1, 1, 1, 0},
			{0, 0, 0, 0, 0, 0, 0, 0, 0}
		};
		
		for(int x = 0; x < literalTrack.length; x++) {
			for(int y = 0; y < literalTrack[x].length; y++) {
				if(literalTrack[x][y] != 0) {
					tracks[x][y] = true;
				}
			}
		}

		try {
			tracksImg = ImageIO.read(new File("res/tracks.png"));
		} catch (IOException e) {
			e.printStackTrace();
		}

		this.setFocusable(true);
		this.requestFocus();
		this.addKeyListener(new InputHandler());

		this.setPreferredSize(new Dimension(S_WIDTH, S_HEIGHT));
	}

	public void tick() {

	}

	public void paintComponent(Graphics gr) {
		Graphics2D g = (Graphics2D) gr;

		// Draw tracks
		g.setColor(Color.white);
		for (int x = 0; x < tracks.length; x++) {
			for (int y = 0; y < tracks[x].length; y++) {
				if (tracks[x][y]) {
					int sx = ((y > 0 && tracks[x][y - 1] ? 2 : 0)
							+ (x + 1 < tracks.length && tracks[x + 1][y] ? 1 : 0));
					int sy = ((x > 0 && tracks[x - 1][y] ? 2 : 0)
							+ (y + 1 < tracks[x].length && tracks[x][y + 1] ? 1 : 0));
					g.drawImage(tracksImg, x * TILE_SIZE, y * TILE_SIZE, (x + 1) * TILE_SIZE, (y + 1) * TILE_SIZE,
							sx * TILE_SIZE, sy * TILE_SIZE, (sx + 1) * TILE_SIZE, (sy + 1) * TILE_SIZE, null);
				} else {
					g.fillRect(x * TILE_SIZE, y * TILE_SIZE, TILE_SIZE, TILE_SIZE);
				}
			}
		}
	}
}
