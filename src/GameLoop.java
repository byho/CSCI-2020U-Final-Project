import javax.swing.*;

import java.awt.*;
import java.awt.event.*;

public class GameLoop extends JFrame implements ActionListener
{
   private GamePanel gamePanel = new GamePanel();
   private JButton startButton = new JButton("Start");
   private JButton quitButton = new JButton("Quit");
   private JButton pauseButton = new JButton("Pause");
   private boolean running = false;
   private boolean paused = false;
   private int fps = 60;
   private int frameCount = 0;
   
   public GameLoop()
   {
      super("Fixed Timestep Game Loop Test");
      Container cp = getContentPane();
      cp.setLayout(new BorderLayout());
      JPanel p = new JPanel();
      p.setLayout(new GridLayout(1,2));
      p.add(startButton);
      p.add(pauseButton);
      p.add(quitButton);
      cp.add(gamePanel, BorderLayout.CENTER);
      cp.add(p, BorderLayout.SOUTH);
      setSize(500, 500);
      
      startButton.addActionListener(this);
      quitButton.addActionListener(this);
      pauseButton.addActionListener(this);
      
      gamePanel.initTriangle();
      
      
   }
   
   
   
   public static void main(String[] args)
   {
      GameLoop glt = new GameLoop();
      glt.setVisible(true);
   }
   
   public void actionPerformed(ActionEvent e)
   {
      Object s = e.getSource();
      if (s == startButton)
      {
         running = !running;
         if (running)
         {
            startButton.setText("Stop");
            runGameLoop();
         }
         else
         {
            startButton.setText("Start");
         }
      }
      else if (s == pauseButton)
      {
        paused = !paused;
         if (paused)
         {
            pauseButton.setText("Unpause");
         }
         else
         {
            pauseButton.setText("Pause");
         }
      }
      else if (s == quitButton)
      {
         System.exit(0);
      }
   }
   
   //Starts a new thread and runs the game loop in it.
   public void runGameLoop()
   {
      Thread loop = new Thread()
      {
         public void run()
         {
            gameLoop();
         }
      };
      loop.start();
   }
   
   //Only run this in another Thread!
   private void gameLoop()
   {

      final double GAME_HERTZ = 30.0;

      final double TIME_BETWEEN_UPDATES = 1000000000 / GAME_HERTZ;

      final int MAX_UPDATES_BEFORE_RENDER = 5;

      double lastUpdateTime = System.nanoTime();

      double lastRenderTime = System.nanoTime();
      
      final double TARGET_FPS = 60;
      final double TARGET_TIME_BETWEEN_RENDERS = 1000000000 / TARGET_FPS;
      
      int lastSecondTime = (int) (lastUpdateTime / 1000000000);
      
      while (running)
      {
         double now = System.nanoTime();
         int updateCount = 0;
         
         if (!paused)
         {
            while( now - lastUpdateTime > TIME_BETWEEN_UPDATES && updateCount < MAX_UPDATES_BEFORE_RENDER )
            {
               updateGame();
               lastUpdateTime += TIME_BETWEEN_UPDATES;
               updateCount++;
            }
   
            if ( now - lastUpdateTime > TIME_BETWEEN_UPDATES)
            {
               lastUpdateTime = now - TIME_BETWEEN_UPDATES;
            }
         
            float interpolation = Math.min(1.0f, (float) ((now - lastUpdateTime) / TIME_BETWEEN_UPDATES) );
            drawGame(interpolation);
            lastRenderTime = now;
         
            int thisSecond = (int) (lastUpdateTime / 1000000000);
            if (thisSecond > lastSecondTime)
            {
               System.out.println("NEW SECOND " + thisSecond + " " + frameCount);
               fps = frameCount;
               frameCount = 0;
               lastSecondTime = thisSecond;
            }
         
            while ( now - lastRenderTime < TARGET_TIME_BETWEEN_RENDERS && now - lastUpdateTime < TIME_BETWEEN_UPDATES)
            {
               Thread.yield();
            
                try {Thread.sleep(1);} catch(Exception e) {} 
            
               now = System.nanoTime();
            }
         }
      }
   }
   
   private void updateGame()
   {
	  gamePanel.requestFocus();
      gamePanel.update();
   }
   
   private void drawGame(float interpolation)
   {
      gamePanel.setInterpolation(interpolation);
      gamePanel.repaint();
   }
   
   private class GamePanel extends JPanel
   {
      float interpolation;
      
      int lastDrawX, lastDrawY;
      
      Triangle triangle;
      
      public GamePanel()
      {
    	  this.setFocusable(true);
    	  this.addKeyListener(new KeyAdapter() {
              public void keyPressed(KeyEvent e) {
                  handleKey(e);
              }
          });

      }
      
      private void handleKey(KeyEvent e) {
          switch (e.getKeyCode()) {
              case KeyEvent.VK_UP:
                  gamePanel.triangle.speed++;
                  break;
              case KeyEvent.VK_DOWN:
           	   gamePanel.triangle.speed--;
                  break;
              case KeyEvent.VK_LEFT:
           	   gamePanel.triangle.rot -= 5;
                  break;
              case KeyEvent.VK_RIGHT:
           	   gamePanel.triangle.rot += 5;
                  break;
              default:
                  return;
          }

      }
      
      public void initTriangle(){triangle = new Triangle();}
      
      public void setInterpolation(float interp)
      {
         interpolation = interp;
      }
      
      public void update()
      {
    	  triangle.update();
      }
      
      public void paintComponent(Graphics g)
      {
         //erase last triangle
         g.setColor(getBackground());
         g.fillRect((int)triangle.lastX, (int)triangle.lastY, triangle.width+2, triangle.height+2);
         g.fillRect(0, 0, 500, 500);
         
         g.setColor(Color.RED);
         int drawX = (int) ((triangle.x - triangle.lastX) * interpolation + triangle.lastX);
         int drawY = (int) ((triangle.y - triangle.lastY) * interpolation + triangle.lastY);
         int[] xPoints = {drawX + triangle.rotateX((float)0,(float)triangle.height/2,triangle.rot),
        		 drawX + triangle.rotateX((float)triangle.width/2,(float)-triangle.height/2,triangle.rot),
        		 drawX + triangle.rotateX((float)-triangle.width/2,(float)-triangle.height/2,triangle.rot)};
         int[] yPoints = {drawY + triangle.rotateY((float)0,(float)triangle.height/2,triangle.rot),
        		 drawY + triangle.rotateY((float)triangle.width/2,(float)-triangle.height/2,triangle.rot),
        		 drawY + triangle.rotateY((float)-triangle.width/2,(float)-triangle.height/2,triangle.rot)};
         g.drawPolygon(xPoints, yPoints, 3);
         
         lastDrawX = drawX;
         lastDrawY = drawY;
         
         g.setColor(Color.BLACK);
         g.drawString("FPS: " + fps, 5, 10);
         
         frameCount++;
      }
   }
   

   
   private class Triangle
   {
      float x, y, lastX, lastY;
      int width, height;
      float xVelocity, yVelocity;
      float speed;
      float rot;//in degrees
      
      public Triangle()
      {
         width = 10;
         height = 20;
         x = (float) (Math.random() * (400) + 50 );
         y = (float) (Math.random() * (400) +50 );
         lastX = x;
         lastY = y;
         xVelocity = 0;
         yVelocity = 0;
         rot = (float)0;
         speed = 0;

      }
      
      private int rotateX(float px, float py, float angle){
   	   int result = 0;
   	   
   	   result = (int) (px*Math.cos(Math.toRadians(angle)) - py*Math.sin(Math.toRadians(angle)));
   	   
   	   return result;
      }
      private int rotateY(float px, float py, float angle){
   	   int result = 0;
   	   
   	   result = (int) (py*Math.cos(Math.toRadians(angle)) + px*Math.sin(Math.toRadians(angle)));
   	   
   	   return result;
      }
      
      public void update()
      {
         lastX = x;
         lastY = y;

         
         x += xVelocity;
         y += yVelocity;
         
         if (x > gamePanel.getWidth())
         {

            x = 0;

         }
         else if (x < 0)
         {
            x = gamePanel.getWidth();
         }
         
         if (y > gamePanel.getHeight())
         {

            y = 0;

         }
         else if (y < 0)
         {
            y = gamePanel.getHeight();
         }
      }
      
      public void draw(Graphics g)
      {
         
      }
   }
}