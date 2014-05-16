package floobits.common;

import com.intellij.notification.NotificationType;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.vfs.VirtualFile;
import floobits.FlooContext;
import floobits.Listener;
import floobits.utilities.Flog;
import floobits.utilities.ThreadSafe;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Created by kans on 5/7/14.
 */
public class EditorEventHandler {
    private final FlooContext context;
    private final FloobitsState state;
    private Listener listener;
    private final OutboundRequestHandler outbound;
    private InboundRequestHandler inbound;

    public EditorEventHandler(FlooContext context, FloobitsState state, OutboundRequestHandler outbound, InboundRequestHandler inbound) {
        this.context = context;
        this.state = state;
        this.outbound = outbound;
        this.inbound = inbound;
        listener = new Listener(this, context);
    }

    public void go() {
        listener.start();
    }
    public void rename(String path, String newPath) {
        if (!state.can("patch")) {
            return;
        }
        Flog.log("Renamed buf: %s - %s", path, newPath);
        Buf buf = state.get_buf_by_path(path);
        if (buf == null) {
            Flog.info("buf does not exist.");
            return;
        }
        String newRelativePath = context.toProjectRelPath(newPath);
        if (newRelativePath == null) {
            Flog.warn(String.format("%s was moved to %s, deleting from workspace.", buf.path, newPath));
            outbound.deleteBuf(buf, true);
            return;
        }
        if (buf.path.equals(newRelativePath)) {
            Flog.info("rename handling workspace rename, aborting.");
            return;
        }
        outbound.renameBuf(buf, newRelativePath);
    }

    public void change(VirtualFile file) {
        String filePath = file.getPath();
        if (!state.can("patch")) {
            return;
        }
        if (!context.isShared(filePath)) {
            return;
        }
        final Buf buf = state.get_buf_by_path(filePath);
        if (buf == null) {
            return;
        }
        synchronized (buf) {
            if (Buf.isBad(buf)) {
                Flog.info("buf isn't populated yet %s", file.getPath());
                return;
            }
            buf.send_patch(file);
        }
    }

    public void changeSelection(String path, ArrayList<ArrayList<Integer>> textRanges) {
        Buf buf = state.get_buf_by_path(path);
        outbound.highlight(buf, textRanges, false);
    }

    public void save(String path) {
        Buf buf = state.get_buf_by_path(path);
        outbound.saveBuf(buf);
    }

    public void softDelete(HashSet<String> files) {
        if (!state.can("patch")) {
            return;
        }

        for (String path : files) {
            Buf buf = state.get_buf_by_path(path);
            if (buf == null) {
                context.statusMessage(String.format("The file, %s, is not in the workspace.", path), NotificationType.WARNING);
                continue;
            }
            outbound.deleteBuf(buf, false);
        }
    }

    void delete(String path) {
        Buf buf = state.get_buf_by_path(path);
        if (buf == null) {
            return;
        }
        outbound.deleteBuf(buf, true);
    }

    public void deleteDirectory(ArrayList<String> filePaths) {
        if (!state.can("patch")) {
            return;
        }

        for (String filePath : filePaths) {
            delete(filePath);
        }
    }

    public void msg(String chatContents) {
        outbound.message(chatContents);
    }

    public void kick(int userId) {
        outbound.kick(userId);
    }

    public void changePerms(int userId, String[] perms) {
        outbound.setPerms("set", userId, perms);
    }

    public void upload(VirtualFile virtualFile) {
        if (state.readOnly) {
            return;
        }
        if (!virtualFile.isValid()) {
            return;
        }
        String path = virtualFile.getPath();
        Buf b = state.get_buf_by_path(path);
        if (b != null) {
            Flog.info("Already in workspace: %s", path);
            return;
        }
        outbound.createBuf(virtualFile);
    }

    public void beforeChange(final VirtualFile file, final Document document) {
        final Buf bufByPath = state.get_buf_by_path(file.getPath());
        if (bufByPath == null) {
            return;
        }
        String msg;
        if (state.readOnly) {
            msg = "This document is readonly because you don't have edit permission in the workspace.";
        } else if (!bufByPath.isPopulated()) {
            msg = "This document is temporarily readonly while we fetch a fresh copy.";
        } else {
            return;
        }
        context.statusMessage(msg, false);
        document.setReadOnly(true);
        context.editor.readOnlyBufferIds.add(bufByPath.path);
        final String text = document.getText();
        context.setTimeout(0, new Runnable() {
            @Override
            public void run() {
                ThreadSafe.write(context, new Runnable() {
                    @Override
                    public void run() {
                        if (!state.readOnly && bufByPath.isPopulated()) {
                            return;
                        }
                        Document d = FileDocumentManager.getInstance().getDocument(file);
                        if (d == null) {
                            return;
                        }
                        try {
                            Listener.flooDisable();
                            d.setReadOnly(false);
                            d.setText(text);
                            d.setReadOnly(true);
                        } finally {
                            Listener.flooEnable();
                        }
                    }
                });
            }
        });
    }

    public boolean follow() {
        boolean mode = !state.stalking;
        state.stalking = mode;
        context.statusMessage(String.format("%s follow mode", mode ? "Enabling" : "Disabling"), false);;
        if (mode && state.lastHighlight != null) {
            inbound._on_highlight(state.lastHighlight);
        }
        return mode;
    }

    public void summon(String path, Integer offset) {
        outbound.summon(path, offset);
    }

    public void sendEditRequest() {
        outbound.requestEdit();
    }

    public void clearHighlights (){
        context.editor.clearHighlights();
    }

    public void openChat() {
        Flog.info("Showing user window.");
        context.chatManager.openChat();
    }
    public void openInBrowser() {
        if(!Desktop.isDesktopSupported()) {
            context.statusMessage("This version of java lacks to support to open your browser.", false);
            return;
        }
        try {
            Desktop.getDesktop().browse(new URI(state.url.toString()));
        } catch (IOException error) {
            Flog.warn(error);
        } catch (URISyntaxException error) {
            Flog.warn(error);
        }
    }
    public void shutdown() {
        if (listener != null) {
            listener.shutdown();
            listener = null;
        }
    }
}