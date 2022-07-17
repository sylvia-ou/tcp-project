import java.net.*;
import java.io.*;
import java.util.HashMap;

public class myServer
{
    //initialize socket and input stream

    private Socket           socket   = null;
    private ServerSocket     serverSocket   = null;
    private DataInputStream dataIn = null;
    private DataOutputStream dataOut = null;

    // constructor with port
    public myServer(int port)
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


            //NOTE: need to check if dataOut works, and the server goodput

            // reads message from client until "Over" is sent
            String line = "";
            int count = 1;
            int segment = 1; //Used to keep track of total number of segments, aka 1mil
            int duplicates = 0;
            HashMap<Integer,Integer> hashMap = new HashMap<Integer,Integer>(); // buffer
          //  HashMap<Integer,Integer> goodPutMap = new HashMap<Integer,Integer>(); // to calculate good put.

            try {
                while (true)
                     {
                        line = dataIn.readUTF();
                        segment++; // Increment the segment everytime we recieve something from the client, regardless if duplicate or not. 
                        // System.out.println("UTF: " + line); // checking
                        //Now convert this UTF into a regular String since we want it in integers for the ACK
                        byte[] charset = line.getBytes("UTF-8");
                        String result = new String(charset, "UTF-8");
                        if (result.equals("End")) { //If the client has "End", then the program is just going to end
                            System.out.println("The client chose to end the program!");
                            break; 
                        }
                         //Print it out for testing
                         // System.out.println("Result:" + result);
                        int sendNum = Integer.parseInt(result);
                        int ackNum = sendNum * 1024 + 1; // Might be redundant. Will change if needed to just do count * 1024 + 1
                         if(segment == 1001){
                             //Have to calculate rest of the dups
                             for (Integer dup : hashMap.values()) {
                                 duplicates += dup;
                             }
                             System.out.println("After 1000 segments, the good-put is " + (duplicates/1000));
                             //set new segment number for next 1000
                             segment = 0;
                         }
                         if(count == 65) { // Max segment number is 2^16 -> once hit 65, have to wrap around back to 1 again (65536/1024 = 64) .
                              count = 1; // reset counter
                              //Delete map after adding all the values
                              for (Integer dup : hashMap.values()) {
                                  duplicates += dup;
                              }
                              hashMap.clear(); // clear map for new space.
                          }
                            if (count == sendNum) // this checks if user sent the correct in order segment
                            {
                                System.out.println("IF ACK:" + ackNum); // using this to check to make sure its the correct one
                                dataOut.writeUTF(String.valueOf(ackNum));
                                dataOut.flush(); // clear after used
                                count++; //To increment the counter so the segment # matches the new one
                               // segment++;
                                while (hashMap.containsKey(count)) {
                                    System.out.println("IF IF ACK: " + ackNum);
                                    dataOut.writeUTF(String.valueOf(ackNum));
                                    dataOut.flush();
                                    count++;
                                }
                            } else // If it doesn't match, then have to store it in a buffer. Making a hashmap for this.
                            {
                                hashMap.merge(sendNum, 0, Integer::sum); // if key does not exist, put 0 as value, else sum 1 to the value linked to key

                                //This would be the old ACK
                                System.out.println("OLD ACK: " + ((count - 1) * 1024 + 1)); // checking
                                dataOut.writeUTF(String.valueOf((count - 1) * 1024 + 1)); //Old ACK
                                dataOut.flush(); // clear after used
                            }
                         if(segment == 1000) {
                            //Not fully implemented yet since unsure about good-put calculation.
                            //Reaching here means that segment number is 2^16. So we have to wrap around, clear the map and keep track of duplicates?
                            for (Integer dup : hashMap.values()) {
                                duplicates += dup;
                            }
                            hashMap.clear(); // clear map for new space.
                            // count = 1; // set count back to 1.

                        }
                    }
                }catch(IOException | NumberFormatException e)
                {
                    System.out.println(e);
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
        myServer server = new myServer(158);
    }
}     
