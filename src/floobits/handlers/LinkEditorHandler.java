package floobits.handlers;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import floobits.FloobitsPlugin;
import floobits.common.*;
import floobits.common.protocol.send.FlooRequestCredentials;
import floobits.utilities.Flog;

import java.awt.*;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.UUID;


public class LinkEditorHandler extends BaseHandler {
    protected String token;

    public LinkEditorHandler(FloobitsPlugin context) {
        super(context);
        UUID uuid = UUID.randomUUID();
        token = String.format("%040x", new BigInteger(1, uuid.toString().getBytes()));
    }

    public void link() {
        url = new FlooUrl(Constants.defaultHost, null, null, Constants.defaultPort, true);
        conn = new FlooConn(this);
        conn.start();
        this.isJoined = true;
        openBrowser();
    }


    @Override
    public void on_data(String name, JsonObject obj) {
        if (!name.equals("credentials")) {
            return;
        }
        Settings settings = new Settings(context.project);
        JsonObject credentials = (JsonObject) obj.get("credentials");
        for (Map.Entry<String, JsonElement> thing : credentials.entrySet()) {
            settings.set(thing.getKey(), thing.getValue().getAsString());
        }
        if (settings.isComplete()) {
            settings.write();
        } else {
            Utils.error_message("Something went wrong while receiving data, please contact Floobits support.", context.project);
        }
        shutDown();
    }

    protected void openBrowser() {
        if(!Desktop.isDesktopSupported()) {
            Utils.error_message("Floobits can't use a browser on this system.", context.project);
            shutDown();
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(String.format("https://%s/dash/link_editor/%s/", Constants.defaultHost, token)));
        } catch (IOException error) {
            shutDown();
            Flog.warn(error);
        } catch (URISyntaxException error) {
            shutDown();
            Flog.warn(error);
        }
    }

    @Override
    public void on_connect() {
        Flog.warn("Connected.");
        this.conn.write(new FlooRequestCredentials(token));
    }
}
