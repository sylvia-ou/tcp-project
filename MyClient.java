import java.net.*;
import java.io.*;

public class MyClient {

    //init socket and IO
    private Socket socket = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;

    public MyClient(String ip, int port)
    {
        try
        {
            //establish connection
            socket = new Socket(ip, port);
            System.out.println("network");

            //to server
            out = new DataOutputStream(socket.getOutputStream());

            //from server
            in = new DataInputStream(socket.getInputStream());
        }
        catch(UnknownHostException e)
        {
            System.out.println(e);
        }
        catch(IOException e)
        {
            System.out.println(e);
        }

        for(int i = 1; i <= 5; i++)
        {
            //line to send to server (each line is an int value casted to a string)
            String line = String.valueOf(i);
            try
            {
                //write to server
                out.writeUTF(line);
                System.out.println("Packet " + i + " sent");
                out.flush();
            }
            catch(IOException e)
            {
                System.out.println(e);
            }
        }

        //close the connection
        try
        {
            in.close();
            out.close();
            socket.close();
        }
        catch(IOException e)
        {
            System.out.println(e);
        }
    }

    public static void main(String args[])
    {
        //set address to your IP address
        String address = "192.168.1.119";
        MyClient client = new MyClient(address, 158);
    }
}