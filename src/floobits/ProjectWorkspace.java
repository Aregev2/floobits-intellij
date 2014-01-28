package floobits;

import com.intellij.openapi.components.ProjectComponent;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;

public class ProjectWorkspace implements ProjectComponent {
    protected final Project project;

    public ProjectWorkspace(Project project) {
        this.project = project;
    }

    public void initComponent() {
        // TODO: insert component initialization logic here


    }

    public void disposeComponent() {
        // TODO: insert component disposal logic here
    }

    @NotNull
    public String getComponentName() {
        return "ProjectWorkspace";
    }

    public void projectOpened() {
        // called when project is opened

//        Settings settings = new Settings();
//        String secret = settings.get("secret");
//        String apiKey = settings.get("api_key");
//        String username = settings.get("username");
//        if (secret == null || (apiKey == null && username == null)) {
//            createAccount();
//        }
        CreateAccountDialog d = new CreateAccountDialog(project);
        d.createCenterPanel();
        d.show();
    }

    public void projectClosed() {
        // called when project is being closed
    }
}