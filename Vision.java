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

import de.ksquared.system.keyboard.GlobalKeyListener;
import de.ksquared.system.keyboard.KeyEvent;
import de.ksquared.system.keyboard.KeyListener;

public class Vision implements Runnable {
	
	//Keyboard hook
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

	/**
	 * Screen capturing thread.
	 * Working principle:
	 * 		- Take a screenshot
	 * 		- Filter out the picture for the given color
	 * 		- Smoothen out edges
	 * 		- Find 
	 */
	@Override
	public void run() {
		System.out.println("Started polling.");
		
		while(running) {
			BufferedImage img = robot.createScreenCapture(cap);
	        
	        Mat screen = bufferedImageToMat(img);
	        Mat filter = new Mat();
	        
	        Core.inRange(screen, new Scalar(b-threshold, g-threshold, r-threshold), new Scalar(b+threshold, g+threshold, r+threshold), filter);
	        Imgproc.GaussianBlur(filter, filter, new Size(9, 9), 2, 2);
	        /*
	         * filter.cols/16 - minimum distance
	         * 255 - maximum in value
	         * 40 - minimum value
	         * 0 - min radius
	         * 0 - max radius
	         */
	        Mat circles = new Mat();
	        Imgproc.HoughCircles(filter, circles, Imgproc.HOUGH_GRADIENT, 1, filter.cols() / 16, 255, 40, 0, 0);
	        
	        for (int i = 0; i < circles.cols(); i++) {
	            double[] params = circles.get(0, i);
	            
	            try {
	            	robot.mouseMove(x+(int)Math.round(params[0]), y+(int)Math.round(params[1]));
	            	Thread.sleep(1);
		            robot.mousePress(InputEvent.BUTTON1_MASK);
		            Thread.sleep(1);
		            robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
					Thread.sleep(1);
				} catch (InterruptedException e) {
					
				}
	        }
		}
		
		System.out.println("Stopped polling.");
	}

}
