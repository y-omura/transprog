package antlr;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.antlr.v4.runtime.*;
import org.antlr.v4.runtime.misc.TestRig;
import org.antlr.v4.runtime.tree.*;
import org.antlr.v4.runtime.tree.gui.TreeViewer;

public class Main {
	static ParseTree currentTree;
	public static void main(String[] args) throws Exception {
		JavaLexer lexer = new JavaLexer(new ANTLRFileStream("../antlrtest/src/Test.java"));
		final JavaParser parser = new JavaParser(new CommonTokenStream(lexer));
		final ParseTree tree = parser.compilationUnit();
		
		tree.getChild(0).getText();
		
		SwingUtilities.invokeLater(new Runnable(){
			@Override
			public void run() {
				final TreeViewer viewer = new TreeViewer(Arrays.asList(parser.getRuleNames()), tree);
				currentTree = tree;
				
				JFrame frame = new JFrame();
				frame.setLayout(new BorderLayout());
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setSize(1500, 850);
				frame.setLocationByPlatform(true);
				frame.setVisible(true);

				final JSlider slider = new JSlider();

				final JScrollPane pane = new JScrollPane(viewer);
				frame.add(pane, BorderLayout.CENTER);
				MouseAdapter ma = new MouseAdapter(){
					@Override
					public void mouseClicked(MouseEvent e) {
						if(e.getButton() == 1){
							if(e.getClickCount() == 2) {
								slider.setValue((int)(slider.getValue()*1.2));
							}
						} else {
							slider.setValue((int)(slider.getValue()*0.8));
						}
					}
					Point p;
					@Override
					public void mousePressed(MouseEvent e) {
						p = e.getLocationOnScreen();
					}
					@Override
					public void mouseDragged(MouseEvent e) {
						if(p != null){
							Point now = pane.getViewport().getViewPosition();
							now.translate(p.x, p.y);
							p = e.getLocationOnScreen();
							now.translate(-p.x, -p.y);
							if(now.x < 0){
								now.x = 0;
							}
							if(now.y < 0){
								now.y = 0;
							}
							if(now.x > pane.getViewport().getView().getWidth()-pane.getViewport().getWidth()){
								now.x = pane.getViewport().getView().getWidth()-pane.getViewport().getWidth();
							}
							if(now.y > pane.getViewport().getView().getHeight()-pane.getViewport().getHeight()){
								now.y = pane.getViewport().getView().getHeight()-pane.getViewport().getHeight();
							}
							pane.getViewport().setViewPosition(now);
						}
					}
					@Override
					public void mouseReleased(MouseEvent e) {
						p = null;
					}
				};
				viewer.addMouseListener(ma);
				viewer.addMouseMotionListener(ma);
				
				JPanel panel = new JPanel(new FlowLayout());
				frame.add(panel, BorderLayout.SOUTH);
				
				JButton p = new JButton("parent");
				panel.add(p);
				p.addActionListener(new ActionListener(){
					@Override
					public void actionPerformed(ActionEvent arg0) {
						if(currentTree.getParent() == null){
							return;
						}
						currentTree = currentTree.getParent();
						viewer.setTree(currentTree);
					}
				});
				
				for(int i = 0; i < 10; i++){
					JButton c = new JButton("child "+i);
					panel.add(c);
					final int k = i;
					c.addActionListener(new ActionListener(){
						@Override
						public void actionPerformed(ActionEvent arg0) {
							if(currentTree.getChild(k) == null){
								return;
							}
							currentTree = currentTree.getChild(k);
							viewer.setTree(currentTree);
						}
					});
				}
				frame.add(slider, BorderLayout.NORTH);
				slider.setMaximum(150);
				slider.setValue(100);
				slider.addChangeListener(new ChangeListener(){
					@Override
					public void stateChanged(ChangeEvent arg0) {
						int value = slider.getValue();
						viewer.setScale(value/100d);
					}
				});
			}
		});
	}
}