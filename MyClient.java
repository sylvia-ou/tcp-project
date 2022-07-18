import java.net.*;
import java.io.*;
import java.util.ArrayList;

public class MyClient {

    //init socket and IO
    private Socket socket = null;
    private DataInputStream in = null;
    private DataOutputStream out = null;

    //total packets to send
    private final int TOTAL_PACKETS = 10000000;

    private int windowSize = 1;
    private int numSentPkts = 0;
    private int currentPkt = 0;
    private ArrayList<Integer> lostPkts = new ArrayList<Integer>(){};

    public MyClient(String ip, int port)
    {
        try
        {
            //establish connection
            socket = new Socket(ip, port);
            System.out.println("network");

            //set socket read timeout
            socket.setSoTimeout(5000);

            //to server
            out = new DataOutputStream(socket.getOutputStream());

            //from server
            in = new DataInputStream(socket.getInputStream());
        }
        catch (UnknownHostException e)
        {
            System.out.println(e);
        }
        catch (IOException e)
        {
            System.out.println(e);
        }

        //handle sliding window
        //while (numSentPkts < TOTAL_PACKETS)
        {
            //# of packets sent = window size
            for (int i = 1; i <= windowSize; i++)
            {
                try
                {
                    //forcing the loss of pkt #3
                    if (i != 3)
                    {
                        //write to server
                        //line is int value casted to a string
                        out.writeUTF(String.valueOf(i));
                        System.out.println("Packet " + i + " sent");
                        out.flush();
                    }
                }
                catch (IOException e)
                {
                    System.out.println(e);
                }

                //update # of sent packets
                numSentPkts++;
            }

            //check for any lost packets
            for (int j = 1; j <= 10; j++)
            {
                currentPkt++;
                try
                {
                    System.out.println(in.readUTF());
                }
                catch (IOException e)
                {
                    System.out.println(e);
                }
            }
        }

        try
        {
            //stop server from reading from socket
            out.writeUTF("End");
            //close streams and connection
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
        MyClient client = new MyClient(address, 1158);
    }
}