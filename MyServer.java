import java.net.*;
import java.io.*;
import java.util.HashMap;

public class MyServer
{
    //initialize socket and input stream

    private Socket socket = null;
    private ServerSocket serverSocket = null;
    private DataInputStream dataIn = null;
    private DataOutputStream dataOut = null;



    // constructor with port
    public MyServer(int port)
    {
        try
        {
            // starts server and waits for a connection
            serverSocket = new ServerSocket(port);
            System.out.println("Server started and waiting for client on port " + port);

            socket = serverSocket.accept(); // passive mode, listens/waits till client connects to the server
            System.out.println("Client Connection Success!"); // ACK for connection

            //Used to get data from the socket
            dataIn = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));

            //Used to write data to the socket, i.e. ACKs
            dataOut = new DataOutputStream(socket.getOutputStream());


            // reads message from client until "Over" is sent
            String line = "";
            int count = 1;
            HashMap<Integer,Integer> hashMap = new HashMap<Integer,Integer>();
            while (!line.equals("Over"))
            {
                try
                {
                    line = dataIn.readUTF();
                    try {
                        System.out.println("UTF: " + line);
                        //Now convert this UTF into a regular String since we want it in integers for the ACK
                        byte[] charset = line.getBytes("UTF-8");
                        String result = new String(charset, "UTF-8");

                        //Print it out for testing
                        System.out.println("Result:" + result);
                        int sendNum = Integer.parseInt(result);
                        int ackNum = sendNum * 1024 + 1;
                        if (count == sendNum) // this checks if user sent the correct in order segment
                        {
                            System.out.println("IF ACK:" + ackNum); // using this to check to make sure its the correct one
                            dataOut.writeInt(ackNum);
                            dataOut.flush(); // clear after used
                            count++; //To increment the counter so the segment # matches the new one
                        } else if (hashMap.containsKey(count)) { // This means the segment number is in the hashmap, so output the correct order
                            System.out.println("In the else if: ");
                            System.out.println(hashMap.get(count)); // This would print the ACK value
                            dataOut.writeInt(ackNum); // NEED TO CHECK IF THIS WORKS.
                            dataOut.flush(); // clear after used
                            count++;
                            hashMap.remove(count); // Going to remove it, so that in the end, if there are any values left in the hashmap that means it's a duplicate?
                        } else // If it doesn't match, then have to store it in a buffer. Making a hashmap for this.
                        {
                            hashMap.put(sendNum, ackNum);
                            //This would be the old ACK
                            System.out.println("OLD ACK: " + (count * 1024 + 1)); // checking
                            dataOut.write(count * 1024 + 1); // Not sure if this works yet. client has to accept input from the socket
                            dataOut.flush(); // clear after used
                        }
                    } catch(NumberFormatException e){
                        e.printStackTrace();
                    }


                }
                catch(IOException i)
                {
                    System.out.println(i);
                }

            }


        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        finally{
            try{
                // close connection
                System.out.println("Closing connection");
                socket.close();
                serverSocket.close();
                dataIn.close();
                dataOut.close();
            } catch(IOException e) {
                e.printStackTrace();;
            }
        }
    }

    public static void main(String args[])
    {
        //Server listens for client requests coming in for port
        MyServer server = new MyServer(158);
    }
}