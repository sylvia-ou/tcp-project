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
    
    //counter for total packets sent (and eventually ACKed)
    //ends program when TOTAL_PACKETS is reached
    //number of sent packets (does not include resent packets)
    private int numSentPkts = 0;
    
    //double after each success until first packet loss, then moves linearly
    private int windowSize = 1;

    //check if window size should be doubled or incremented linearly
    //default: true, window size doubles until first packet loss
    private boolean doubleWindow = true;

    //check if window size needs to be halved
    //default: false, window size gets halved when packet loss occurs
    private boolean halfWindow = false;

    //list for keeping track of ACKs
    //sent but unACKed packets stay in here
    private ArrayList<Integer> packetList = new ArrayList<Integer>(){};


    public MyClient(String ip, int port)
    {
        //establish connection
        try
        {
            //start new socket
            socket = new Socket(ip, port);
            System.out.println("network");

            //set socket read timeout (ms)
            socket.setSoTimeout(1000);

            //to server
            out = new DataOutputStream(socket.getOutputStream());

            //from server
            in = new DataInputStream(socket.getInputStream());
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        //actual packet #
        //loop between 1-64 since max segment number is 2^16 
        int loopedPacketNum = 1;
        
        //handle sliding window
        while (numSentPkts < TOTAL_PACKETS)
        {
            //# of total packets sent in this transmission
            int totalPacketsInWindow = 1;

            //send packets according to window size
            while (totalPacketsInWindow <= windowSize)
            {
                try
                {
                    ////keep between 1-64 since max segment number is 2^16
                    if (loopedPacketNum > 64)
                    {
                        loopedPacketNum = 1;
                    }

                    //write sequence # to server
                    //line is int value casted to a string
                    out.writeUTF(String.valueOf(loopedPacketNum * 1024));
                    System.out.println("Packet " + loopedPacketNum + " sent");

                    //add packet to unACKed packet list
                    packetList.add(Integer.valueOf(loopedPacketNum));

                    //update current packet number (1-64 only)
                    loopedPacketNum++;
                    //update total packets sent in window
                    totalPacketsInWindow++;
                    //update number of total sent packets (not including retransmissions)
                    numSentPkts++;

                    //stop if total number of sent packets reaches TOTAL_PACKETS
                    if (numSentPkts >= TOTAL_PACKETS)
                    {
                        break;
                    }
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }

            //check if all packets are ACKed
            //on last transmission, the list being empty means all sent packets have been ACKed
            while (!packetList.isEmpty())
            {
                try
                {
                    //parsing UTF to int
                    byte[] charset = in.readUTF().getBytes("UTF-8");
                    String response = new String(charset, "UTF-8");
                    //packet number of current ACK
                    int currentACK = (Integer.parseInt(response) - 1) / 1024;
                    
                    //check if packets 
                    if (packetList.get(0) == currentACK)
                    {
                        System.out.println("Packet " + packetList.get(0) + " ACKed: " + response);
                        //remove packet waiting on ACK
                        packetList.remove(0);
                    }
                }
                catch (IOException e)
                {
                    //catch read timeouts - lost ACKs and extra old ACKs
                    System.out.println("Packet lost. Resending packet " + packetList.get(0));
                    try
                    {
                        //resend packet
                        out.writeUTF(String.valueOf(packetList.get(0) * 1024));
                    }
                    catch (IOException i)
                    {
                        e.printStackTrace();
                    }
                    
                    //halve the window size after this window finishes
                    //fluctuates, do for all windows with packet loss
                    halfWindow = true;

                    //switch to linear window size after this window finishes
                    //switches only after first packet loss, never goes back to true
                    doubleWindow = false;
                }
            }

            //update window size
            //check for 2 window size flags
            //good transmission, double window
            if (doubleWindow == true)
            {
                //max window size is 2^15
                if (windowSize < Math.pow(2, 15))
                {
                    windowSize = windowSize*2;
                }
                System.out.println("Window size changed to " + windowSize);
            }
            //good transmission but already had a packet loss in the past
            //linearly change window
            else if (doubleWindow == false && halfWindow == false)
            {
                if (windowSize < Math.pow(2, 15))
                {
                    windowSize++;
                }
                System.out.println("Window size changed to " + windowSize);
            }
            //bad transmission, halve window
            else
            {
                //minimum window size is 1
                //round up when halving so window size can never hit 0
                double tempSize = (double) windowSize;
                tempSize = tempSize / 2 + 0.5;
                windowSize =  (int) tempSize;
                System.out.println("Window size changed to " + windowSize);
                
                //reset so window size only halves when the transmission is bad
                halfWindow = false;
            }
        }

        //packet sending is done, end connection
        try
        {
            System.out.println("Number of packets sent: " + numSentPkts);
            //stop server from reading from socket
            out.writeUTF("End");
            //close streams and connection
            in.close();
            out.close();
            socket.close();
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
    }

    public static void main(String args[])
    {
        //set address to your IP address
        String address = "192.168.1.119";
        MyClient client = new MyClient(address, 1158);
    }
}