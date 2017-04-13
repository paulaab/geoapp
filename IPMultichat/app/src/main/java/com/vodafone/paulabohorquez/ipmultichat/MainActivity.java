package com.vodafone.paulabohorquez.ipmultichat;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.net.wifi.WifiManager;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Button;
import android.widget.EditText;
import android.view.View;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.String;
import java.net.MulticastSocket;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;

public class MainActivity extends AppCompatActivity {
    //Define Variables
    private Handler handler = new Handler();
    public ListView msgView;
    public ArrayAdapter<String> msgList;
    protected Context context;
    private static WifiManager wifi_manager;
    protected int PORT_NUM = 5432;
    private static final String MESSAGE = "Hello World!";
    //private static final String MCAST_ADDR = "225.4.5.6";
    //private NetworkInterface mNetworkInterface;

    private static final String MCAST_ADDR = "FF02::1";//"FF01::101"; //IPV6 ADDRESS
    //private static final String MCAST_ADDR = "FF7E:230::1234"; //IPV6 ADDRESS GENYMOTION
    private static InetAddress GROUP;
    private MulticastSocket mcSocketSend;
    private DatagramPacket mcPacketSend;
    public MulticastSocket mcSocketRecv = new MulticastSocket(PORT_NUM);

//Flag used to know when the user wants to stop receiving messages
    volatile boolean shutdown = false;


    public MainActivity() throws IOException {
        System.out.println("Could not set up recv socket");
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

//Define Parameters of the View
        msgView = (ListView) findViewById(R.id.listView);
        msgList = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1);
        msgView.setAdapter(msgList);
        final EditText txtEdit = (EditText) findViewById(R.id.myMsg);

 //Creating JSON Object
        /**
         {
         "MessageTypeID" : 1,
         "Erzeugerzeitpunkt" : 0,
         "Lebensdauer" : 10,
         "Lat" : 31.23
         "Long" : 3.432
         "Cell ID" : [0,1]
         "Message" : "My String"
         }


         */
        final JSONObject myJO = new JSONObject();
        JSONArray jarr = new JSONArray();

/*
        try {
            myJO.put("MessageTypeID", null);
            myJO.put("Erzeugerzeitpunkt", null);
            myJO.put("Lebensdauer", null);
            myJO.put("Lat", null);
            myJO.put("Long",null);
            myJO.put("Cell ID",jarr);
            myJO.put("Message", null);
        } catch (JSONException e) {
            e.printStackTrace();
        }*/

//JSON Object for testing purposes
        try {
            myJO.put("MessageTypeID", 2);
            myJO.put("Erzeugerzeitpunkt", 12);
            myJO.put("Lebensdauer", 12);
            myJO.put("Lat", 51.23610018);
            myJO.put("Long",6.73155069);
            myJO.put("Cell ID",jarr);
            myJO.put("Message", "hello");
        } catch (JSONException e) {
            e.printStackTrace();
        }

 //Click button to start sendMessage class

        final Button sendButton = (Button) findViewById(R.id.buttonSend);

        sendButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //sendMessage("Hello world");
                //sendMessage(txtEdit.getText().toString());
                sendMessage(myJO.toString());
            }
        });

//Click button to start receiveMessage class

        final Button recvButton = (Button) findViewById(R.id.buttonReceive);
        recvButton.setOnClickListener(new View.OnClickListener() {
             @Override
             public void onClick(View v) {
                 shutdown = false;
                 try {
                     receiveMessage();
                 } catch (UnknownHostException e) {
                     e.printStackTrace();
                 }


              }
        });
        final Button stopButton = (Button) findViewById(R.id.buttonStop);
        stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v){
                try {
                    mcSocketRecv.leaveGroup(GROUP);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                shutdown = true;


            }
        });

        /*try {
            this.mNetworkInterface = NetworkInterface.getByName("wlan0");
        } catch (SocketException e) {
            e.printStackTrace();
        }
        if (null != this.mNetworkInterface) {
            System.out.println("Using interface " + this.mNetworkInterface.getName() + " for IPv6");
            try {
                this.mcSocketRecv.setNetworkInterface(this.mNetworkInterface);
            } catch (SocketException e) {
                e.printStackTrace();
            }
            try {
                this.mcSocketSend.setNetworkInterface(this.mNetworkInterface);
            } catch (SocketException e) {
                e.printStackTrace();
            }
        }*/


    }


    /*-------------------------Sending methods------------------------------------------*/

    //public boolean sendMessage()  {
    public void sendMessage(String message) throws IllegalArgumentException {
        if (message == null || message.length() == 0) {
            throw new IllegalArgumentException();
        }
        final String mensaje = message;

        new Thread(new Runnable() {
            @Override
            public void run() {
                // Create the Multicast sending socket and join Multicast Group
                if (mcSocketSend == null) {
                    try {
                        GROUP = InetAddress.getByName(MCAST_ADDR);
                        mcSocketSend = new MulticastSocket(PORT_NUM);
                        //Use in case of IPv6 problems on Samsung...
                        NetworkInterface nif = NetworkInterface.getByName("wlan0");
                        if (null != nif) {
                            System.out.println( "picking interface "+nif.getName()+" for transmit");
                            mcSocketSend.setNetworkInterface(nif);
                        }
                        //...Until here.
                        mcSocketSend.joinGroup(GROUP);

                    } catch (Exception e) {
                        Log.d("Error in the socket: ", e.getMessage());

                    }
                }

//Build the Datagram Packet

                try {
                    mcPacketSend = new DatagramPacket(mensaje.getBytes(), mensaje.length(), GROUP, PORT_NUM);

                } catch (Exception e) {
                    Log.v("Error creating packet: ", e.getMessage());
                }

//Send the packet
                try {
                    mcSocketSend.send(mcPacketSend);
                } catch (IOException e) {
                    System.out.println("There was an error sending the packet");
                    e.printStackTrace();
                }
                System.out.println("Server sent packet with msg: " + mensaje);
                }
        }).start();
    }


/*-------------------------Receiving methods----------------------------------------*/




    public void receiveMessage() throws UnknownHostException {
            giveLock();
            // Get the address of the group that we are going to connect to
            final InetAddress addressGroup = InetAddress.getByName(MCAST_ADDR);
            System.out.println("Inside method");
            new Thread(new Runnable() {
                @Override
                public void run() {


//Create the Multicast receiving socket and join Multicast Group

                    try {
                        //Use in case of IPv6 problems on Samsung...
                        NetworkInterface nif = NetworkInterface.getByName("wlan0");
                        if (null != nif) {
                            System.out.println( "picking interface "+nif.getName()+" for transmit");
                            mcSocketRecv.setNetworkInterface(nif);
                        }
                        //...Until here.
                        mcSocketRecv.joinGroup(addressGroup);

                        while (!shutdown) {
                            // Create a buffer of bytes, which will be used to store incoming messages
                            byte[] buffer = new byte[256];
                            // Receive the info on a socket and print it on the screen
                            DatagramPacket mcPacketRecv = new DatagramPacket(buffer, buffer.length);
                            mcSocketRecv.receive(mcPacketRecv);
                            String msg = new String(buffer, 0, buffer.length);
                            System.out.println("Socket 1 received msg: " + msg);
                            displayMsg(msg);

                        }
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            }).start();

    }


public void giveLock () {

        /*WifiManager wifi;
        //Method to prevent WiFi stack for blocking upcoming Multicast messages
        wifi = (WifiManager) context.getSystemService(context.WIFI_SERVICE);

        //= (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
        if (wifi != null) {
            WifiManager.MulticastLock lock = wifi.createMulticastLock("TAG");
            lock.acquire();
            System.out.println("Lock Acquired");
        }
        else {
            System.out.println("Wifi not enabled");
        }*/
    // Acquire multicast lock

     WifiManager wifi = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
   WifiManager.MulticastLock multicastLock = wifi.createMulticastLock("multicastLock");
    multicastLock.setReferenceCounted(true);
    multicastLock.acquire();
}



/*-----------------------Display my message--------------------------------*/

    public void displayMsg(String msg) {
        if (!shutdown) {
            final String mensajeRecibido = msg;
            handler.post(new Runnable() {

                @Override
                public void run() {
                    //TODO Auto-generated method stub
                    msgList.add(mensajeRecibido);
                    msgView.setAdapter(msgList);
                    msgView.smoothScrollToPosition(msgList.getCount() - 1);

                }
            });
        }
    }
}















