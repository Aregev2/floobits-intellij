package floobits.common.protocol.handlers;

import com.google.gson.JsonObject;
import floobits.common.*;
import floobits.common.interfaces.IContext;
import floobits.common.interfaces.IFile;
import floobits.common.protocol.Connection;
import floobits.common.protocol.json.send.FlooAuth;
import floobits.utilities.Flog;

import java.util.HashMap;


public class FlooHandler extends BaseHandler {
    private final HashMap<String, String> auth;
    private final boolean shouldUpload;
    private final IFile dirToAdd;
    public FloobitsState state;
    InboundRequestHandler inbound;
    public EditorEventHandler editorEventHandler;

    public FlooHandler(final IContext context, FlooUrl flooUrl, boolean shouldUpload, String path,
                       HashMap<String, String> auth, IFile dirToAdd) {
        super(context);
        this.auth = auth;
        this.shouldUpload = shouldUpload;
        this.dirToAdd = dirToAdd;
        context.setColabDir(Utils.fixPath(path));
        url = flooUrl;
        state = new FloobitsState(context, flooUrl);
        state.username = auth.get("username");
    }

    public void go() {
        super.go();
        if (context == null) {
            Flog.error("Attempted to join a workspace with no context.");
            isJoined = false;
            return;
        }
        Flog.log("joining workspace %s", url);
        conn = new Connection(this);
        outbound = new OutboundRequestHandler(context, state, conn);
        inbound = new InboundRequestHandler(context, state, outbound, shouldUpload, dirToAdd);
        editorEventHandler = new EditorEventHandler(context, state, outbound, inbound);
        PersistentJson persistentJson = PersistentJson.getInstance();
        persistentJson.addWorkspace(url, context.colabDir);
        persistentJson.save();
        conn.start();
        editorEventHandler.go();

        if (context.isAccountAutoGenerated()) {
            FlooUserDetail flooUserDetail = API.getUserDetail(context, state);
            if (flooUserDetail != null && !flooUserDetail.auto_created) {
                persistentJson.auto_generated_account = false;
                persistentJson.save();
                return;
            }
            context.notifyCompleteSignUp();
        }
    }

    public void on_connect () {
        if (conn == null) {
            return;
        }
        context.connected();
        context.statusMessage(String.format("Connecting to %s.", Utils.getLinkHTML(url.toString(), url.toString())));
        conn.write(new FlooAuth(auth.get("username"), auth.get("api_key"), auth.get("secret"), url.owner, url.workspace));
    }

    public void _on_data (String name, JsonObject obj) {
        Flog.debug("Calling %s", name);
        try {
            inbound.on_data(name, obj);
        } catch (Throwable e) {
            Flog.error(String.format("on_data error \n\n%s", e.toString()));
            API.uploadCrash(this, context, e);
        }
    }

    @Override
    public void shutdown() {
        super.shutdown();
        context.statusMessage(String.format("Leaving workspace %s.", Utils.getLinkHTML(url.toString(), url.toString())));
        state.shutdown();
    }
}
