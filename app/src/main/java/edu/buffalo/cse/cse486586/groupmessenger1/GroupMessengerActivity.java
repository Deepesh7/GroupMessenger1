package edu.buffalo.cse.cse486586.groupmessenger1;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {

    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String[] REMOTE_PORTS = new String[] {"11108","11112","11116","11120","11124"};
    static final int SERVER_PORT = 10000;
    public int sequenceNumber = 0;
    private final Uri mUri = buildUri("content", "edu.buffalo.cse.cse486586.groupmessenger1.provider");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());

        try {
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);
            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        Button send = (Button) findViewById(R.id.button4);
        final EditText msgEditText = (EditText) findViewById(R.id.editText1);

        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String msg = msgEditText.getText().toString();
                msgEditText.setText("");
                new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg);
            }
        });
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */
    }

    private Uri buildUri(String scheme, String authority) {
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.authority(authority);
        uriBuilder.scheme(scheme);
        return uriBuilder.build();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }


    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {
            ServerSocket serverSocket = sockets[0];
            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             */
            try {
                while(true){
                    Socket s = serverSocket.accept();
                    BufferedReader in = new BufferedReader(new InputStreamReader(s.getInputStream()));
                    String msg = in.readLine();
                    publishProgress(msg);
                    s.close();
                }
            }catch (IOException e){ e.printStackTrace(); }
            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */

            String strReceived = strings[0];

            TextView displayTextView = (TextView) findViewById(R.id.textView1);
            displayTextView.append(strReceived+"\n");

            ContentValues contentValues = new ContentValues();
            contentValues.put("key",String.valueOf(sequenceNumber));
            contentValues.put("value", strReceived);
            sequenceNumber++;
            getContentResolver().insert(mUri,contentValues);

            
            return;
        }
    }


    private class ClientTask extends AsyncTask<String, Void, Void> {

        @Override
        protected Void doInBackground(String... msgs) {

            String msgToSend = msgs[0];

            byte[] b = new byte[] {10, 0, 2, 2};

            try {
                for(int i=0;i<5;i++){
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10,0,2,2}),
                            Integer.parseInt(REMOTE_PORTS[i]));
                    PrintWriter out = new PrintWriter(socket.getOutputStream(),true);
                    out.println(msgToSend);
                    out.close();
                    socket.close();
                }
            } catch (UnknownHostException e) {
                Log.e("Hello", "ClientTask UnknownHostException");
            } catch (IOException e) {
                Log.e("Hello", "ClientTask socket IOException");
            }

            return null;
        }
    }
}
