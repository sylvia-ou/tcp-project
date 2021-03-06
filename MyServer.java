//~* Server side of TCP.
//~*Group members: Ivana Chen, Sylvia Ou.

import java.net.*;
import java.io.*;
import java.util.HashMap;

public class MyServer {
    private Socket socket = null;
    private ServerSocket serverSocket = null;
    private DataInputStream dataIn = null;
    private DataOutputStream dataOut = null;

    /**
     * Constructor for MyServer. This function will forever have the server running, and will system print what port
     * it is waiting for the client on. It also will  system print whenever the client has successfully connected.
     *
     * @param port - port number for connection
     */
    public MyServer(int port) {
        while (true) {
            try {
                // starts server and waits for a connection
                serverSocket = new ServerSocket(port);
                System.out.println("Server started and waiting for client on port " + port);

                socket = serverSocket.accept(); // passive mode, listens/waits till client connects to the server
                System.out.println("Success!"); // ACK for connection
                //clientConnected = true; Used for graphs

                dataIn = new DataInputStream(
                        new BufferedInputStream(socket.getInputStream()));

                dataOut = new DataOutputStream(socket.getOutputStream());

                ack();
                closeSocket();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * This function will read in socket input from the client. It will then check to see if the data is in-order or not.
     * If it is in-order, it will print out an ACK to the socket for the client to read from. If it is not in-order,
     * the server will store it in a buffer, a hash map, and output to the socket the oldest ACK it had done. Once the
     * missing packet is finally received, it will output to the socket all the necessary in-order ACKS
     */
    public void ack() {
        String line = ""; // holds the data from socket
        int count = 1;
        int segment = 0; //Used to keep track of total number of segments, aka 1mil
        double sentDuplicates = 0;
        HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>(); // buffer


        try {
            while (true) {
                line = dataIn.readUTF();

                //Convert this UTF into an integer since we want it in integers for the ACK
                byte[] charset = line.getBytes("UTF-8");
                String result = new String(charset, "UTF-8");

                if (result.equals("End")) { //If the client has "End", then the program is just going to end
                    //System.out.println("The client chose to end the program!");
                    break;
                }
                int sentNum = (Integer.parseInt(result) / 1024); // Divide by 1024 so this will be in 1 , 2 , 3 , 4 etc.
                //System.out.println("Recieved " + sentNum);
                if (segment == 1000) {
                   // System.out.println("After 1000 segments, the good-put is " + (sentDuplicates / 1000));
                    segment = 0;
                    sentDuplicates = 0;
                }
                if (count > 64) { // Max segment number is 2^16 -> once hit 64, have to wrap around back to 1 again (65536/1024 = 64).
                    count = 1; // reset counter
                    hashMap.clear(); // clear map for new space.
                }

                if (count == sentNum) // this checks if user sent the correct in order segment
                {
                    //System.out.println("Sending ACK: " + (count * 1024 + 1));

                    sentDuplicates += 1;
                    dataOut.writeUTF(String.valueOf(count * 1024 + 1));
                    dataOut.flush(); // clear after used
                    count++; //To increment the counter so the segment # matches the new one
                    while (hashMap.containsKey(count)) {
                        //System.out.println("Sending ACK: " + (count * 1024 + 1));
                        dataOut.writeUTF(String.valueOf(count * 1024 + 1));
                        dataOut.flush();
                        count++;
                    }

                } else // If it doesn't match, then have to store it in a buffer. Making a hashmap for this.
                {
                    hashMap.merge(sentNum, 1, Integer::sum); // if key does not exist, put 1 as value, else sum 1 to the value linked to key

                    int oldAck = (count - 1) * 1024 + 1;
                    if (oldAck != 1) {
                        //System.out.println("Sending OLD ACK: " + (oldAck)); // checking
                        dataOut.writeUTF(String.valueOf(oldAck)); //Old ACK
                        dataOut.flush(); // clear after used
                    }
                    sentDuplicates += hashMap.get(sentNum);
                }
                segment++; // Increment the segment everytime we recieve something from the client, regardless if duplicate or not.
                // System.out.println("segments: " + segment);
            }
            if (segment == 1000) {
                //System.out.println("After 1000 segments, the good-put is " + (sentDuplicates / 1000));
            }

        } catch (IOException | NumberFormatException e) {
            System.out.println("Client closed connection.");
            //e.printStackTrace();
        }

    }

    /**
     * This function will close the input and output streams, and system print that it is closing.
     */
    public void closeSocket() {
        try {
            // close streams 
            System.out.println("Closing connection!");
            serverSocket.close();
            dataIn.close();
            dataOut.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String args[]) {
        //Server listens for client requests coming in for port
        MyServer server = new MyServer(1158);

    }
}
