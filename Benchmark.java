package com.main;

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.Random;

import javax.swing.JFrame;
import javax.swing.JPanel;

public class Benchmark {
	
	static int width = 1000;
	static int height = 500;
	static long start = 0;
	
	static int hit = 0;
	static int all = 0;
	static Color turqoise = new Color(5, 151, 136);
	static Random random = new Random();
	static int radius = 200;
	
	static volatile int circle_x;
	static volatile int circle_y;
	
	public static void main(String[] args) {
		JFrame frame = new JFrame("Benchmark");
		JPanel canvas = new JPanel() {
			int x = 0;
			int y = 0;
			
			protected void paintComponent(Graphics g) {
				g.setColor(Color.WHITE);
				g.fillRect(0, 0, width, height);
				roll();
				g.setColor(turqoise);
				g.fillOval(x - radius / 2, y - radius / 2, radius, radius);
			}
			
			public void roll() {
				int newx = radius + random.nextInt(width - radius * 2);
				int newy = radius + random.nextInt(height - radius * 2);
				
				while(Math.abs(newx-x) < radius && Math.abs(newy-y) < radius) {
					newx = radius + random.nextInt(width - radius * 2);
					newy = radius + random.nextInt(height - radius * 2);
				}
				
				x = newx;
				y = newy;
				
				circle_x = x;
				circle_y = y;
			}
		};
		canvas.addMouseListener(new MouseListener() {

			@Override
			public void mouseClicked(MouseEvent e) {
				if(e.getButton() == MouseEvent.BUTTON1) {
					all++;
					int dx = circle_x-e.getX();
					int dy = circle_y-e.getY();
			        if(Math.sqrt(dx*dx + dy*dy) < radius / 2) {
			        	hit++;
			        	((Component)e.getSource()).repaint();
			        }
			        float hps = hit/((System.currentTimeMillis()-start)/1000.0f);
			        //frame.setTitle("Benchmark - Accuracy: " + (((float)hit)/all)*100 + "% - " + hps + " hits/sec");
				}
			}

			@Override
			public void mouseEntered(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mouseExited(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void mousePressed(MouseEvent e) {
				
			}

			@Override
			public void mouseReleased(MouseEvent arg0) {
				// TODO Auto-generated method stub
				
			}
			
		});
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		canvas.setSize(width, height);
		
		start = System.currentTimeMillis();
		frame.add(canvas);
		frame.setSize(width, height);
		frame.setVisible(true);
	}

}
