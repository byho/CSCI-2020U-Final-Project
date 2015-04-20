import java.net.*;
import java.io.*;

public class ClientThread extends Thread
{  private Socket           socket   = null;
  private Client       client   = null;
  private DataInputStream  streamIn = null;

  public ClientThread(Client _client, Socket _socket)
  {  client   = _client;
     socket   = _socket;
     open();  
     start();
  }
  public void open()
  {  try
     {  streamIn  = new DataInputStream(socket.getInputStream());
     }
     catch(IOException ioe)
     {  System.out.println("Error getting input stream: " + ioe);
        client.stop();
     }
  }
  public void close()
  {  try
     {  if (streamIn != null) streamIn.close();
     }
     catch(IOException ioe)
     {  System.out.println("Error closing input stream: " + ioe);
     }
  }
  public void run()
  {  while (true)
     {  try
        {  client.handle(streamIn.readUTF());
           Thread.yield();
        }
        catch(IOException ioe)
        {  System.out.println("Listening error: " + ioe.getMessage());
           client.stop();
        }
     }
  }
}