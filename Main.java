import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
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

public class Main {

	private JFrame frame;
	private Component currentImage;
	private ClusteredList list;
	private List<File> cluster;

	private int index = -1;

	private static int SCREEN_WIDTH, SCREEN_HEIGHT;

	public static void main(String[] args) {
		new Main();
	}

	public Main() {
		// get collection of file clusters
		list = new ClusteredList(new File("./Parakeets").listFiles());
		
		// get first cluster
		cluster = list.getNext();
		
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
			BufferedImage buffered = ImageIO.read(cluster.get(index));
//			System.out.println(buffered.getHeight());
			
			// TODO do better scaling such as that in Feather.class
			Image image = buffered.getScaledInstance(SCREEN_WIDTH, SCREEN_HEIGHT, Image.SCALE_FAST);
//			System.out.println(image.get);
			
			JLabel newImage = new JLabel(new ImageIcon(image));
			newImage.setLayout(new FlowLayout(FlowLayout.CENTER));
			
			StringBuilder overlayText = new StringBuilder()
					.append("image")
					.append(index + 1)
					.append("/")
					.append(cluster.size() - 1)
					.append(" : ")
					.append(cluster.get(index).getName());
			
			JLabel textJLabel = new JLabel(overlayText.toString());
//			filenameText.setForeground(getContrastingColour(colour));
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

		BufferedImage nullCursorImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
		Cursor nullCursor = frame.getToolkit().createCustomCursor(nullCursorImage, new Point(), null);
		frame.getContentPane().setCursor(nullCursor);

		device.setFullScreenWindow(frame);
	}

	private class Bindings extends JPanel {

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
			this.getInputMap().put(KeyStroke.getKeyStroke(PREVIOUS, 0), PREVIOUS);
			
			// keystrokes with shift key 
			this.getInputMap().put(KeyStroke.getKeyStroke(NEXT, KeyEvent.SHIFT_DOWN_MASK), NEXT_CLUSTER);
			this.getInputMap().put(KeyStroke.getKeyStroke(PREVIOUS, KeyEvent.SHIFT_DOWN_MASK), PREVIOUS_CLUSTER);
		}

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
					System.out.println("> right arrow");
					showNext();
				}
			});

			this.getActionMap().put(PREVIOUS, new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					System.out.println("> left arrow");
					showPrevious();
				}
			});
			
			this.getActionMap().put(NEXT_CLUSTER, new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					System.out.println("> shifted right arrow");
					cluster = list.getNext();
					index = -1;
					showNext();
				}
			});
			
			this.getActionMap().put(PREVIOUS_CLUSTER, new AbstractAction() {
				public void actionPerformed(ActionEvent e) {
					System.out.println("> shifted left arrow");
					cluster = list.getPrevious();
					index = -1;
					showPrevious();
				}
			});
		}
	}

}