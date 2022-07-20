import java.net.*;
import java.io.*;
import java.util.HashMap;

public class myServer {
    private Socket socket = null;
    private ServerSocket serverSocket = null;
    private DataInputStream dataIn = null;
    private DataOutputStream dataOut = null;

    // constructor with port
    public myServer(int port) {
        try {
            // starts server and waits for a connection
            serverSocket = new ServerSocket(port);
            System.out.println("Server started and waiting for client on port " + port);

            socket = serverSocket.accept(); // passive mode, listens/waits till client connects to the server
            System.out.println("Client Connection Success!"); // ACK for connection

            dataIn = new DataInputStream(
                    new BufferedInputStream(socket.getInputStream()));

            dataOut = new DataOutputStream(socket.getOutputStream());

            String line = ""; // holds the data from socket
            int count = 1;
            int segment = 0; //Used to keep track of total number of segments, aka 1mil
            int duplicates = 0; // sent segments
            HashMap<Integer, Integer> hashMap = new HashMap<Integer, Integer>(); // buffer

            try {
                while (true) {
                    line = dataIn.readUTF();

                    //Convert this UTF into an integer since we want it in integers for the ACK
                    byte[] charset = line.getBytes("UTF-8");
                    String result = new String(charset, "UTF-8");
                    if (result.equals("End")) { //If the client has "End", then the program is just going to end
                        System.out.println("The client chose to end the program!");
                        break;
                    }
                    if (segment == 1000) {
                        //Have to calculate rest of the dups
                        for (Integer dup : hashMap.values()) {
                            duplicates += dup;
                        }
                        //Good-put is received segments/sent segments
                        //So Sent is the duplicates.
                        System.out.println("After 1000 segments, the good-put is " + (duplicates / 1000));
                        duplicates = 0;
                        segment = 0;
                    }
                    if (count > 64) { // Max segment number is 2^16 -> once hit 64, have to wrap around back to 1 again (65536/1024 = 64).
                        count = 1; // reset counter
                        System.out.println("Resetting sequence numbers!");
                        //Calculate the duplicates before clearing the map.
                        for (Integer dup : hashMap.values()) {
                            duplicates += dup;
                        }
                        hashMap.clear(); // clear map for new space.
                    }
                    int sentNum = (Integer.parseInt(result) / 1024); // Divide by 1024 so this will be in 1 , 2 , 3 , 4 etc.

                    if (count == sentNum) // this checks if user sent the correct in order segment
                    {
                        System.out.println("Sending ACK:" + (count * 1024 + 1));
                        dataOut.writeUTF(String.valueOf(count * 1024 + 1));
                        dataOut.flush(); // clear after used
                        count++; //To increment the counter so the segment # matches the new one
                        while (hashMap.containsKey(count)) {
                            System.out.println("Sending ACK: " + (count * 1024 + 1));
                            dataOut.writeUTF(String.valueOf(count * 1024 + 1));
                            dataOut.flush();
                            count++;
                        }
                    } else // If it doesn't match, then have to store it in a buffer. Making a hashmap for this.
                    {
                        hashMap.merge(sentNum, 0, Integer::sum); // if key does not exist, put 0 as value, else sum 1 to the value linked to key

                        int oldAck = (count - 1) * 1024 + 1;
                        if (oldAck != 1) {
                            System.out.println("Sending OLD ACK: " + (oldAck)); // checking
                            dataOut.writeUTF(String.valueOf(oldAck)); //Old ACK
                            dataOut.flush(); // clear after used
                        }
                    }
                    segment++; // Increment the segment everytime we recieve something from the client, regardless if duplicate or not.

                }
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                // close connection
                System.out.println("Closing connection!");
                socket.close();
                serverSocket.close();
                dataIn.close();
                dataOut.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static void main(String args[]) {
        //Server listens for client requests coming in for port
        myServer server = new myServer(1158);
    }
}
