import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowFocusListener;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.imageio.ImageIO;
import javax.swing.AbstractAction;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.border.LineBorder;

public class Main {

	private JFrame frame;
	private Component currentImage;
	private ClusteredList clusters;
	private List<ClusteredList.ClusteredFile> cluster;

	private String clusterData;

	private int index = -1;

	private static int SCREEN_WIDTH;
	private static int SCREEN_HEIGHT;

	private static final int CURSOR_SHOW_TIME_MS = 750;
	
	private static final Font INFOBOX_FONT = new Font(Font.MONOSPACED, Font.PLAIN, 18);
	private static final LineBorder INFOBOX_BORDER = new LineBorder(Color.BLACK, 8);
	private static FlowLayout DEFAULT_LAYOUT = new FlowLayout(FlowLayout.CENTER);

	public static void main(String[] args) {
		new Main();
	}

	public Main() {
		// test typical folder name first
		if (new File("./Parakeets").exists()) {
			clusters = new ClusteredList(new File("./Parakeets").listFiles());
		}

		// look in root execution folder for images
		if (clusters == null) {
			clusters = new ClusteredList(new File(".").listFiles());
		}

		// exit if nothing found
		if (clusters.size() == 0) {
			System.out.println("no images found");
			return;
		}

		// set up initial cluster
		cluster = clusters.getNext();
		clusterData = clusters.getClusterData();
		
		// tighten up default vertical gap for displayed Components
		DEFAULT_LAYOUT.setVgap(0);

		// graphical awt/swing environment
		setupEnvironment();

		// start showing pictures
		showNext();
	}

	private void showPrevious() {
		if (index < 0) {
			index = 0;
		} else if (index == 0) {
			return;
		} else if (index > 0) {
			index -= 1;
		}

		changeImage();
	}

	private void showNext() {
		int max = cluster.size() - 1;

		if (index > max) {
			index = max;
		} else if (index == max) {
			return;
		} else if (index < max) {
			index += 1;
		}

		changeImage();
	}

	private void changeImage() {
		try {
			BufferedImage buffered = ImageIO.read(cluster.get(index).file);
			
			int imageWidth = buffered.getWidth();
			int imageHeight = buffered.getHeight();

			double scaleFactor = getDownscaleFitFactor(imageWidth, imageHeight);

			imageWidth = (int) (imageWidth * scaleFactor);
			imageHeight = (int) (imageHeight * scaleFactor);

			Image image = buffered.getScaledInstance(imageWidth, imageHeight, Image.SCALE_FAST);

			JLabel newImage = new JLabel(new ImageIcon(image));
			newImage.setLayout(DEFAULT_LAYOUT);

			JLabel textJLabel = new JLabel(getImageData() + clusterData);
			textJLabel.setOpaque(true);
			textJLabel.setBorder(INFOBOX_BORDER);
			textJLabel.setFont(INFOBOX_FONT);
			textJLabel.setBackground(Color.BLACK);
			textJLabel.setForeground(Color.WHITE);
			newImage.add(textJLabel);

			Component compToRemove = currentImage;
			currentImage = frame.add(newImage);

			if (compToRemove != null) {
				frame.remove(compToRemove);
			}

			frame.setVisible(true);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private String getImageData() {
		return "[ image " + (index + 1) + " of " + (cluster.size()) + " ] ";
	}

	private void setupEnvironment() {
		GraphicsEnvironment env = GraphicsEnvironment.getLocalGraphicsEnvironment();
		GraphicsDevice device = env.getDefaultScreenDevice();

		SCREEN_WIDTH = device.getDisplayMode().getWidth();
		SCREEN_HEIGHT = device.getDisplayMode().getHeight();

//		System.out.println("screen dimensions: " + SCREEN_WIDTH + " x " + SCREEN_HEIGHT);

		frame = new JFrame();
		frame.add(new Bindings());

		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		frame.setUndecorated(true);
		frame.getContentPane().setBackground(Color.BLACK);
		frame.setTitle("Pigeon Holes");

		// set up mouse cursor to show briefly on movement
		frame.addMouseMotionListener(getMouseListener());

		// set up behaviour on loss of window focus
		frame.addWindowFocusListener(getFocusListener(device));

		device.setFullScreenWindow(frame);
	}

	private static double getDownscaleFitFactor(int imageWidth, int imageHeight) {
		double defaultFactor = 1d;
		double downscaleFactor = 1d;

		double scaleWidth = getScaleFactor(imageWidth, SCREEN_WIDTH);
		double scaleHeight = getScaleFactor(imageHeight, SCREEN_HEIGHT);
		downscaleFactor = (scaleWidth < scaleHeight) ? scaleWidth : scaleHeight;

		return (downscaleFactor < defaultFactor) ? downscaleFactor : defaultFactor;
	}

	private static double getScaleFactor(int imageDimension, int screenDimension) {
		double factor = 1d;
		if (imageDimension > screenDimension) {
			factor = (double) screenDimension / (double) imageDimension;
		}
		return (factor > 0) ? factor : 1d;
	}

	private MouseMotionListener getMouseListener() {
		BufferedImage nullCursorImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Cursor nullCursor = frame.getToolkit().createCustomCursor(nullCursorImage, new Point(), null);
		frame.getContentPane().setCursor(nullCursor);

		return new MouseMotionListener() {
			@Override
			public void mouseMoved(MouseEvent move) {
				if (frame.getContentPane().getCursor() == nullCursor) {
					new Thread(new Runnable() {
						public void run() {
							frame.getContentPane().setCursor(Cursor.getDefaultCursor());
							try {
								Thread.sleep(CURSOR_SHOW_TIME_MS);
								frame.getContentPane().setCursor(nullCursor);
							} catch (InterruptedException e) {
								e.printStackTrace();
							}
						}
					}).start();
				}
			}

			@Override
			public void mouseDragged(MouseEvent e) {
			}
		};
	}

	private WindowFocusListener getFocusListener(GraphicsDevice device) {
		return new WindowFocusListener() {

			@Override
			public void windowLostFocus(WindowEvent e) {
				frame.setExtendedState(JFrame.ICONIFIED);
				device.setFullScreenWindow(null);
			}

			@Override
			public void windowGainedFocus(WindowEvent e) {
				device.setFullScreenWindow(frame);
			}
		};
	}

	private class Bindings extends JPanel {

		private static final long serialVersionUID = 1L;
		private final int ESCAPE = KeyEvent.VK_ESCAPE;
		private final int NEXT = KeyEvent.VK_RIGHT;
		private final int PREVIOUS = KeyEvent.VK_LEFT;
		private final String NEXT_CLUSTER = "next_cluster";
		private final String PREVIOUS_CLUSTER = "previous_cluster";

		public Bindings() {
			setupInputMap();
			setupActionMap();
			setOpaque(false);
		}

		private void setupInputMap() {
			// regular single keystrokes
			this.getInputMap().put(KeyStroke.getKeyStroke(ESCAPE, 0), ESCAPE);
			this.getInputMap().put(KeyStroke.getKeyStroke(NEXT, 0), NEXT);
			this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, 0), NEXT);
			this.getInputMap().put(KeyStroke.getKeyStroke(PREVIOUS, 0), PREVIOUS);
			this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, 0), PREVIOUS);

			// keystrokes with shift key
			this.getInputMap().put(KeyStroke.getKeyStroke(NEXT, KeyEvent.SHIFT_DOWN_MASK), NEXT_CLUSTER);
			this.getInputMap().put(KeyStroke.getKeyStroke(PREVIOUS, KeyEvent.SHIFT_DOWN_MASK), PREVIOUS_CLUSTER);
			this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_RIGHT, KeyEvent.SHIFT_DOWN_MASK), NEXT_CLUSTER);
			this.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_KP_LEFT, KeyEvent.SHIFT_DOWN_MASK), PREVIOUS_CLUSTER);
		}

		@SuppressWarnings("serial")
		private void setupActionMap() {
			this.getActionMap().put(ESCAPE, new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
//					System.out.println("> escape");
					frame.setVisible(false);
					frame.dispose();
				}
			});

			this.getActionMap().put(NEXT, new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
//					System.out.println("> right arrow");
					showNext();
				}
			});

			this.getActionMap().put(PREVIOUS, new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
//					System.out.println("> left arrow");
					showPrevious();
				}
			});

			this.getActionMap().put(NEXT_CLUSTER, new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
//					System.out.println("> shifted right arrow");
					cluster = clusters.getNext();
					clusterData = clusters.getClusterData();
					index = -1;
					showNext();
				}
			});

			this.getActionMap().put(PREVIOUS_CLUSTER, new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
//					System.out.println("> shifted left arrow");
					cluster = clusters.getPrevious();
					clusterData = clusters.getClusterData();
					index = -1;
					showPrevious();
				}
			});
		}
	}

}