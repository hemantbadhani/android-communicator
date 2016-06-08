package com.hbadhani.android.communicator;

import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;

public class TCPCommunicatorActivity extends AppCompatActivity  implements View.OnClickListener{

    public static final String TAG = "TCPCommunicatorActivity";
    public static final int TIMEOUT_SECONDS = 5;
    private Button btnStartServer;
    private String connectionStatus = null;
    private final String sendMsg = "Hello From Server";
    private Handler mHandler = null;
    private ServerSocket server = null;
    private Socket client = null;
    private ObjectOutputStream objectOutputStream;
    public static InputStream inputStream = null;

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tcpcommunicator);

        btnStartServer = (Button) findViewById(R.id.btnView);
        btnStartServer.setOnClickListener(this);
        mHandler = new Handler();
    }


    @Override
    public void onClick(View v) {
        //initialize server socket in a new separate thread
        new Thread(initializeConnection).start();
        String msg = "Waiting for incoming connection";
        Toast.makeText(TCPCommunicatorActivity.this, msg, Toast.LENGTH_LONG).show();
    }

    /**
     * Runnable to show pop-up for connection status
     */
    private final Runnable showConnectionStatus = new Runnable()
    {
        //----------------------------------------
        /**
         * @see java.lang.Runnable#run()
         */
        //----------------------------------------
        @Override
        public void run()
        {
            Toast.makeText(TCPCommunicatorActivity.this, connectionStatus, Toast.LENGTH_SHORT).show();
        }
    };

    /**
     * Thread to initialize Socket connection
     */
    private final Runnable initializeConnection = new Thread()
    {
        @Override
        public void run()
        {
            // initialize server socket
            try
            {
                EditText portNumText = (EditText) findViewById(R.id.tcpPortNumText);
                assert portNumText != null;
                server = new ServerSocket(Integer.parseInt(portNumText.getText().toString()));
                server.setSoTimeout(TCPCommunicatorActivity.TIMEOUT_SECONDS * 1000);

                //attempt to accept a connection
                client = server.accept();

                objectOutputStream = new ObjectOutputStream(client.getOutputStream());
                inputStream = client.getInputStream();
                try
                {
                    objectOutputStream.writeObject(sendMsg);
                    System.out.println("client >" + sendMsg);

                    byte[] bytes = new byte[1024];
                    int numRead = 0;
                    while ((numRead = TCPCommunicatorActivity.inputStream.read(bytes)) >= 0)
                    {
                        connectionStatus = new String(bytes, 0, numRead);
                        mHandler.post(showConnectionStatus);
                    }
                }
                catch (IOException ioException)
                {
                    Log.e(TCPCommunicatorActivity.TAG, "" + ioException);
                }
            }
            catch (SocketTimeoutException e)
            {
                connectionStatus = "Connection has timed out! Please try again";
                mHandler.post(showConnectionStatus);
            }
            catch (IOException e)
            {
                connectionStatus = "IO Exception encountered";
                mHandler.post(showConnectionStatus);
                Log.e(TCPCommunicatorActivity.TAG, "" + e);
            }

            if (client != null)
            {
                connectionStatus = "Connection was successful!";
                mHandler.post(showConnectionStatus);
            }
        }
    };

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        try
        {
            // Close the opened resources on activity destroy
            TCPCommunicatorActivity.inputStream.close();
            objectOutputStream.close();
            if (server != null)
            {
                server.close();
            }
        }
        catch (IOException ec)
        {
            Log.e(TCPCommunicatorActivity.TAG, "Cannot close server socket" + ec);
        }
    }
}
