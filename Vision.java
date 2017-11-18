package com.main;

import java.awt.AWTException;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.DataBufferInt;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.opencv.core.*;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.*;

import com.sun.glass.events.MouseEvent;

/* USAGE
	Run this java program, using this class in the manifest
	An overlay should popup on the top of your primary display
	The Start button will start capturing the screen continuously
	and will automatically click on the circles with the given color
	in the given area.
	Exit button will exit the program.
	Also for false-safe operation pressing ESC anytime will exit the
	program. (You have to use keyboardhook for this)
	If pressing ESC doesn't work then I recommend pressing CTRL+SHIFT+DEL
	opening up a task manager and killing the java.exe.
*/

/* OPERATION
    While running
	Capture screen area specified by x, y, w (width), h (height)
	Filter for the given color (turqoise in this case)
	Find circles
	Move the cursor to a circle
	Press on it
	Release left mouse button
	And repeat.

NOTES:
	The values below are calibrated for streamlab's sequence game.
	If you want to use it for a different game then change the r,g,b and threshold values
	Also the below x,y,w,h values are for an 1080p screen watching Twitch in Chrome in normal mode (not theather/fullscreen)
	
*/

//Comment out if you don't want or can't use keyboardhook
import de.ksquared.system.keyboard.GlobalKeyListener;
import de.ksquared.system.keyboard.KeyEvent;
import de.ksquared.system.keyboard.KeyListener;

public class Vision implements Runnable {
	
	//Comment out if you don't want or can't use keyboardhook
	static GlobalKeyListener listener = new GlobalKeyListener();

	static JFrame frame = new JFrame("Sequence");
	static JPanel panel = new JPanel();
	static JButton start = new JButton("Start");
	static JButton exit = new JButton("Exit");
	
	static Robot robot;
	
	//State of the screen capturing thread
	static boolean running = false;
	
	//Screen region to capture
	static int x = 340;
	static int y = 260;
	static int w = 1120;
	static int h = 640;
	static Rectangle cap = new Rectangle(x, y, w, h);
	
	//Color: 0, 148, 133
	//RGB values used for the filter
	static int r = 20;
	static int g = 151;
	static int b = 136;
	//Maximum difference in value per color channel
	static int threshold = 20;

	public static void main(String[] args) throws AWTException {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		
		robot = new Robot();
		
		//Setting up transparent window
		frame.setUndecorated(true);
		frame.setBackground(new Color(0, 0, 0, 0));
		frame.setAlwaysOnTop(true);
		frame.setExtendedState(JFrame.MAXIMIZED_BOTH);
		frame.getRootPane().putClientProperty("apple.awt.draggableWindowBackground", false);
		frame.getContentPane().add(panel);
		
		panel.setBackground(new Color(0,0,0,0));
		panel.add(start);
		panel.add(exit);
		
		exit.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				running = false;
				System.exit(0);
			}
		});
		start.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent arg0) {
				if(!running) {
					Vision v = new Vision();
					Thread t = new Thread(v);
					t.start();
					running = true;
					start.setText("Stop");
				}
				else {
					running = false;
					start.setText("Start");
				}
			}
			
		});
        frame.setVisible(true);
        
        /*
         * Attaches a listener to the keyboard so that you can stop the program
         * without requiring special intervention like ctrl+alt+del
         */
	//Comment out if you don't want or can't use keyboardhook
        listener.addKeyListener(new KeyListener() {
			@Override
			public void keyPressed(KeyEvent event) {
				if(event.getVirtualKeyCode() == KeyEvent.VK_ESCAPE) {
					running = false;
					System.exit(0);
				}
			}
			@Override
			public void keyReleased(KeyEvent arg0) {}
        });
	}
	
	/**
	 * @param bi Image consisting of integer values
	 * @return Matrix containing the RGB values
	 */
	public static Mat bufferedImageToMat(BufferedImage bi) {
	    Mat mat = new Mat(bi.getHeight(), bi.getWidth(), CvType.CV_8UC(3));
	    byte r, g, b;
	    for (int y = 0; y < bi.getHeight(); y++) {
	        for (int x = 0; x < bi.getWidth(); x++) {
	            int rgb = bi.getRGB(x, y);
	            r = (byte) ((rgb >> 0) & 0xFF);
	            g = (byte) ((rgb >> 8) & 0xFF);
	            b = (byte) ((rgb >> 16) & 0xFF);
	            mat.put(y, x, new byte[]{r, g, b});
	        }
	    }
	    return mat;
	}
	
	@Override
	public void run() {
		System.out.println("Started polling.");
		
		while(running) {
			//Take a picture
			BufferedImage img = robot.createScreenCapture(cap);
	        
	        Mat screen = bufferedImageToMat(img);
	        Mat filter = new Mat();
	        
		//Filter the specific color
	        Core.inRange(screen, new Scalar(b-threshold, g-threshold, r-threshold), new Scalar(b+threshold, g+threshold, r+threshold), filter);
	        Imgproc.GaussianBlur(filter, filter, new Size(9, 9), 2, 2);
	        /*
	         * filter.cols/16 - minimum distance
	         * 255 - maximum in value
	         * 40 - minimum value
	         * 0 - min radius
	         * 0 - max radius
	         */
		//Find circles on the image
	        Mat circles = new Mat();
	        Imgproc.HoughCircles(filter, circles, Imgproc.HOUGH_GRADIENT, 1, filter.cols() / 16, 255, 40, 0, 0);
	        
	        for (int i = 0; i < circles.cols(); i++) {
	            double[] params = circles.get(0, i);
	            
	            try {
			//Move the cursor to the given circle and click on it
	            	robot.mouseMove(x+(int)Math.round(params[0]), y+(int)Math.round(params[1]));
		            robot.mousePress(InputEvent.BUTTON1_MASK);
		            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
				} catch (InterruptedException e) {
					
				}
	        }
		}
		
		System.out.println("Stopped polling.");
	}

}
