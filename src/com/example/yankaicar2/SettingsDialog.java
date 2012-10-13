
package com.example.yankaicar2;
 

import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;


import android.app.Dialog;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;


/**
 * Gather the xmpp settings and create an XMPPConnection
 */
public class SettingsDialog extends Dialog implements android.view.View.OnClickListener {
    private VideoSmackCarControllerActivity xmppClient;

    public SettingsDialog(VideoSmackCarControllerActivity xmppClient) {
        super(xmppClient);
        this.xmppClient = xmppClient;
    }

    @Override
	protected void onStart() {
        super.onStart();
        setContentView(R.layout.settings);
        getWindow().setFlags(4, 4);
        setTitle("XMPP Settings");
        Button ok = (Button) findViewById(R.id.ok);
        ok.setOnClickListener(this);
    }

    @Override
	public void onClick(View v) {
        String host = getText(R.id.host);
        String port = getText(R.id.port);
//        String path = getText(R.id.path);
        String server = getText(R.id.service);
        String username = getText(R.id.userid);
        String password = getText(R.id.password);

        // Create a connection
      
      XMPPConnection.DEBUG_ENABLED = true;  
      final ConnectionConfiguration connectionConfig = new ConnectionConfiguration(host, Integer.parseInt(port), server);  
      connectionConfig.setSASLAuthenticationEnabled(false);  
      XMPPConnection connection = new XMPPConnection(connectionConfig);  
  		try {
			connection.connect();
		} catch (XMPPException e) {
            Log.e("XMPPClient", "[SettingsDialog] Failed to connect to " + connection.getHost(), e);
            xmppClient.setConnection(null);
            throw new IllegalStateException(e);
		}//Á¬½Ó        
    catch (Exception eb){
    	
    	Log.e("XMPPClient", "[SettingsDialog] Failed to log in as " + username, eb);
        xmppClient.setConnection(null);
        throw new IllegalStateException(eb);
    }
        try {
            connection.login(username, password);
            // Set the status to available
//            Presence presence = new Presence(Presence.Type.available);
//            connection.sendPacket(presence);
            xmppClient.setConnection(connection);
            xmppClient.serverName=server;
        } catch (XMPPException ex) {
            Log.e("XMPPClient", "[SettingsDialog] Failed to log in as " + username, ex);
            xmppClient.setConnection(null);
        }catch (Exception eb){
        	
        	Log.e("XMPPClient", "[SettingsDialog] Failed to log in as " + username, eb);
            xmppClient.setConnection(null);
        }
        dismiss();
    }

    private String getText(int id) {
        EditText widget = (EditText) this.findViewById(id);
        return widget.getText().toString();
    }
}
