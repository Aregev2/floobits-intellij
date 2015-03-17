package floobits.windows;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.IconLoader;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import floobits.common.interfaces.IContext;
import floobits.common.FlooUrl;
import floobits.common.protocol.handlers.FlooHandler;
import floobits.common.protocol.FlooUser;
import floobits.impl.ContextImpl;
import floobits.utilities.Flog;

import java.util.*;


public class ChatManager {
    protected IContext context;
    protected ToolWindow toolWindow;
    protected ChatForm chatForm;

    public ChatManager (ContextImpl context) {
       this.context = context;
       chatForm = new ChatForm(context);
       this.createChatWindow(context.project);
    }

    protected void createChatWindow(Project project) {
        ToolWindowManager toolWindowManager = ToolWindowManager.getInstance(project);
        toolWindow = toolWindowManager.registerToolWindow("Floobits Chat", true, ToolWindowAnchor.BOTTOM);
        toolWindow.setIcon(IconLoader.getIcon("/icons/floo13.png"));
        Content content = ContentFactory.SERVICE.getInstance().createContent(chatForm.getChatPanel(), "", true);
        toolWindow.getContentManager().addContent(content);
    }

    public void openChat() {
        FlooHandler flooHandler = context.getFlooHandler();
        if (flooHandler == null) {
            return;
        }
        try {
            toolWindow.show(null);
        } catch (NullPointerException e) {
            Flog.warn("Could not open chat window.");
            return;
        }
        FlooUrl url = flooHandler.getUrl();
        toolWindow.setTitle(String.format("- %s", url.toString()));
    }

    public void closeChat() {
        toolWindow.hide(null);
    }

    public boolean isOpen() {
        try {
            return toolWindow.isVisible();
        } catch (IllegalStateException e) {
            return false;
        }
    }

    public void clearUsers() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.clearClients();
            }
        }, ModalityState.NON_MODAL);
    }

    public void addUser(final FlooUser user) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.addUser(user);
            }
        }, ModalityState.NON_MODAL);
    }

    public void statusMessage(final String message) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.statusMessage(message);
            }
        }, ModalityState.NON_MODAL);
    }

    public void errorMessage(final String message) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.errorMessage(message);
            }
        }, ModalityState.NON_MODAL);
    }

    public void chatMessage(final String username, final String msg, final Date messageDate) {
        if (context.lastChatMessage != null && context.lastChatMessage.compareTo(messageDate) > -1) {
            // Don't replay previously shown chat messages.
            return;
        }
        context.lastChatMessage = messageDate;
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.chatMessage(username, msg, messageDate);
            }
        }, ModalityState.NON_MODAL);
    }

    public void removeUser(final FlooUser user) {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.removeUser(user);
            }
        }, ModalityState.NON_MODAL);
    }

    public void updateUserList() {
        ApplicationManager.getApplication().invokeLater(new Runnable() {
            @Override
            public void run() {
                chatForm.updateGravatars();
            }
        }, ModalityState.NON_MODAL);
    }
}
