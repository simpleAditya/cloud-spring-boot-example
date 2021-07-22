import net.lingala.zip4j.ZipFile;
import org.eclipse.jgit.api.*;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.*;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.transport.*;
import org.eclipse.jgit.treewalk.TreeWalk;
import org.gitlab4j.api.GitLabApi;
import org.gitlab4j.api.GitLabApiException;
import org.gitlab4j.api.models.Project;
import org.gitlab4j.api.models.User;
import org.glassfish.jersey.internal.guava.Lists;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class Jgit
{
    public static void main(String[] args)
    {
        String repoPath = "/home/adityasrivastava/Documents/jgit-repo/README.md";
        try {
//            GitLabApi gitLabApi = GitLabApi.oauth2Login("http://gitlab.localhost.com", "root", "root");
            CredentialsProvider credentialsProvider = new UsernamePasswordCredentialsProvider("addy1219", "@D!ty@143");

            Git git = cloneRepo(credentialsProvider);

              while(true)
              {
                  Scanner scanner = new Scanner(System.in);
                  int choice = scanner.nextInt();

                  switch (choice)
                  {
                      case 1:
                          init();
                          break;
                      case 2:
                          commit(git);
                          break;
                      case 3:
                          push(git, credentialsProvider);
                          break;
                      case 4:
                          pull(git, credentialsProvider);
                          break;
//                      case 5:
//                          newProject(gitLabApi);
//                          break;
//                      case 6:
//                          newUser(gitLabApi);
//                          break;
                      case 7:
                          branchOps(git, credentialsProvider);
                          break;
                      case 8:
                          getLatestLocalCommit(git);
                          break;
                      case 9:
                          getLatestRemoteCommit(git);
                          break;
                      case 10:
                          getRepoMeta(git);
                          break;
                      case 11:
                          add(git, true);
                          break;
                      case 12:
                          reset(git, repoPath);
                          break;
                      case 13:
                          merge(git, credentialsProvider);
                          break;
                      case 14:
                          createTag(git, credentialsProvider);
                          break;
                      case 15:
                          hasLocalChanges(git);
                          break;
                      case 16:
                          listCommits(git);
                      case 17:
                          branch(git);
                      case 18:
                          downloadTag(git);
                      default:
                          System.exit(0);
                  }
              }
        }
        catch (GitAPIException | URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }
    public static void init() throws IOException, GitAPIException {
        File directory = new File("/home/adityasrivastava/Documents/jgit-repo");

        new File(directory,".git/objects").mkdirs();
        new File(directory,".git/refs/heads").mkdirs();

        File head = new File(directory,".git/HEAD");

        FileWriter fileWriter = new FileWriter(head);
        fileWriter.append("ref:refs/heads/master");
        fileWriter.close();

        Git git = Git.init().setDirectory(directory).call();
        System.out.println(git.getRepository());
    }
    public static void commit(Git git) throws GitAPIException, IOException {
        add(git, true);

        CommitCommand commit = git.commit();
        commit.setMessage("Initial commit...").call();

        RevWalk walk = new RevWalk( git.getRepository() );
        ObjectId head = git.getRepository().resolve( "HEAD" );
            RevCommit lastCommit = walk.parseCommit( head );
        System.out.println(lastCommit.getId());
    }
    public static String getLatestLocalCommit(Git git) throws IOException, GitAPIException {
        List<Ref> branches = git.branchList().setListMode(ListBranchCommand.ListMode.ALL).call();
        try {
            RevWalk walk = new RevWalk(git.getRepository());
            for(Ref branch : branches) {
                RevCommit commit = walk.parseCommit(branch.getObjectId());
                System.out.println(commit.getAuthorIdent().getWhen());
                System.out.println(commit.getAuthorIdent().getName());
                System.out.println(commit.getName());
            }
        } catch (Exception e)
        {
            e.printStackTrace();
        }

        return null;
    }

    public static void getLatestRemoteCommit(Git git) throws GitAPIException, IOException {
        Collection<Ref> collection = git.lsRemote().call();
        System.out.println(collection.iterator().next().getObjectId().getName());

        RevWalk walk = new RevWalk(git.getRepository());
        RevCommit lastCommit = walk.parseCommit(collection.iterator().next().getObjectId());

        PersonIdent personIdent = lastCommit.getAuthorIdent();
        System.out.println(personIdent.getName());
        System.out.println(personIdent.getEmailAddress());
        System.out.println(personIdent.getWhen());
    }
    public static void listCommits(Git git) throws GitAPIException {
        Iterable<RevCommit> logs = git.log().call();
        for(RevCommit commit : logs) {
            String commitID = commit.getName();
            System.out.println(commitID);
        }
    }
    public static Git cloneRepo(CredentialsProvider credentialsProvider) throws GitAPIException {
        Git git = Git.cloneRepository()
                .setURI("https://github.com/addy1219/Git-Java-Integration.git")
                .setCredentialsProvider(credentialsProvider)
                .setDirectory(new File("/home/adityasrivastava/Documents/jgit-repo"))
                .call();
        System.out.println(git.getRepository());
        return git;
    }
    public static void getRepoMeta(Git git) throws IOException, GitAPIException {
            boolean localChanges = false;
            String lastCommit = getLatestLocalCommit(git);
            String currentBranch = git.getRepository().getFullBranch();

            Status status = git.status().call();

            Set<String> uncommittedChanges = status.getUncommittedChanges();
            if( ! uncommittedChanges.isEmpty() )
                localChanges = true;

            AddRepoMeta repoMeta = new AddRepoMeta();
            repoMeta.setCommitId(lastCommit);
            repoMeta.setCurrentBranch(currentBranch);
            repoMeta.setLocalChanges(localChanges);
            repoMeta.setSyncInProgress(false);

            System.out.println(repoMeta.getCommitId());
            System.out.println(repoMeta.getCurrentBranch());
            System.out.println(repoMeta.isLocalChanges());
    }

    public static void add(Git git, boolean file) throws GitAPIException {
        if( file )
            git.add().addFilepattern(".").call();
        else
            listOfFiles(null, git, null );
    }

    private static Git listOfFiles(File filePath, Git git, String projectPath) throws GitAPIException
    {
        if( !filePath.isDirectory() )
        {
            git.add().addFilepattern(filePath.getAbsolutePath().replace(projectPath + File.separator, "")).call();
            return git;
        }
        File[] filesList = filePath.listFiles();
        if( filesList == null )
        {
            return git;
        }
        for( File file : filesList )
        {
            if( file.isFile() )
                git.add().addFilepattern(filePath.getAbsolutePath().replace(projectPath + File.separator, "")).call();
            else
                listOfFiles(file, git, projectPath);
        }
        return git;
    }

    public static void reset(Git git, String repoPath) throws GitAPIException {
        Ref ref = git.reset().addPath(repoPath).call();
        ResetCommand reset = git.reset();
        reset.setRef(ref.getName());
        reset.setMode(ResetCommand.ResetType.HARD);
        reset.call();
    }

    public static void push(Git git, CredentialsProvider credentialsProvider) throws GitAPIException, URISyntaxException {

        CheckoutCommand coCmd = git.checkout();
        // Commands are part of the api module, which include git-like calls
        coCmd.setName("master");
        coCmd.setCreateBranch(false); // probably not needed, just to make sure
        coCmd.call(); // switch to "master" branch

        RemoteAddCommand remoteAddCommand = git.remoteAdd();
        remoteAddCommand.setName("origin");
        remoteAddCommand.setUri(new URIish("https://github.com/addy1219/Java.git"));
        remoteAddCommand.call();

        // git push -u origin master...

        PushCommand pushCommand = git.push();
        pushCommand.setCredentialsProvider(credentialsProvider);
        pushCommand.add("master");
        pushCommand.setRemote("origin");
        pushCommand.call();

        System.out.println("Pushed !");
    }

    public static void pull(Git git, CredentialsProvider credentialsProvider) throws GitAPIException {

        PullResult result = git.pull().setCredentialsProvider(credentialsProvider).call();
        System.out.println(result.isSuccessful());
    }

    public static void merge(Git git, CredentialsProvider credentialsProvider) throws GitAPIException {

        PushCommand pushCommand = git.push();
        pushCommand.setCredentialsProvider(credentialsProvider);
        pushCommand.add("master");
        pushCommand.setRemote("origin");
        Iterable<PushResult> pushResults = pushCommand.call();

        for (PushResult pushResult : pushResults) {
            for (RemoteRefUpdate update : pushResult.getRemoteUpdates()) {
                System.out.println(update.getStatus());
            }
        }
        PullResult result = git.pull().setCredentialsProvider(credentialsProvider).call();
        FetchResult fetchResult = result.getFetchResult();
        MergeResult mergeResult = result.getMergeResult();
        MergeResult.MergeStatus mergeStatus = mergeResult.getMergeStatus();

        for (TrackingRefUpdate update : fetchResult.getTrackingRefUpdates()) {
            System.out.println(update.getLocalName());
            System.out.println(update.getRemoteName());
            System.out.println(update.getResult());
        }
        System.out.println(mergeResult);
        System.out.println(mergeStatus);

        pushResults = pushCommand.call();

        for (PushResult pushResult : pushResults) {
            for (RemoteRefUpdate update : pushResult.getRemoteUpdates()) {
                System.out.println(update.getStatus());
            }
        }
        System.out.println("Pulled !");
    }
    public static void newProject(GitLabApi gitLabApi) throws GitLabApiException {
        Project project = new Project()
                .withName("projectName")
                .withDescription("My project...")
                .withIssuesEnabled(true)
                .withMergeRequestsEnabled(true)
                .withWikiEnabled(true)
                .withSnippetsEnabled(true)
                .withPublic(false);
        Project newProject = gitLabApi.getProjectApi().createProject(project);
        System.out.println(newProject.getName());
    }
    public static void newUser(GitLabApi gitLabApi) throws GitLabApiException {
        User user = new User()
                .withName("name")
                .withUsername("username")
                .withEmail("email")
                .withCanCreateGroup(true)
                .withCanCreateProject(true)
                .withIsAdmin(false);
        gitLabApi.getUserApi().createUser(user,"password", true);
    }
    public static void branchOps(Git git, CredentialsProvider credentialsProvider) throws GitAPIException, URISyntaxException, IOException {
        System.out.println(git.getRepository().getBranch());

        Scanner scanner = new Scanner(System.in);
        int choice = scanner.nextInt();

        if(choice==1) {
            git.branchCreate().setName("newBranch").call();
            System.out.println("new branch created...");
        }
        else if(choice==2) {
            git.checkout().setName("newBranch").call();
            System.out.println("checked out...");
        }
        else if(choice==3) {
            git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call().stream()
                    .anyMatch(branch -> branch.getName().equals("refs/remotes/origin/" + "branchName"));
            System.out.println("Branch exists or not");
        }
        else if(choice==4) {
            git.checkout().setName("master");
            git.branchDelete().setBranchNames("newBranch");
            System.out.println("Back on master branch and removed the created one...");
        }
        else if(choice==5) {
            System.out.println("Listing the existing branches...");
            List<Ref> call = git.branchList().call();
            for (Ref ref : call) {
                System.out.println("Branch: " + ref + " " + ref.getName() + " " + ref.getObjectId().getName());
            }
        }
        else
            System.out.println("Wrong choice !");

        push(git, credentialsProvider);
    }

    public static void branch(Git git) throws GitAPIException {
        Iterable<RevCommit> logs = git.log().call();
        for( RevCommit revCommit : logs )
        {
            for( RevCommit revCommit1 : revCommit.getParents())
            System.out.println(revCommit1.getName());
        }
    }

    public static void createTag(Git git, CredentialsProvider credentialsProvider) throws GitAPIException {
        String tagName = "tagName", tagMessage = "tagMessage";
        git.tag().setName(tagName).setMessage(tagMessage).setForceUpdate(true).call();
        git.push().setCredentialsProvider(credentialsProvider).setPushTags().call();

        System.out.println(git.tag().getName());
    }

    public static void downloadTag(Git git) throws IOException {
        String command =
                "curl -X GET http://35.196.171.58:31900/api/v4/projects/701/"
                        + "repository/archive.zip?sha=f35a4440-70fb-4728-b589-2d032664011d"
                        + "&access_token=d444f86f58689f42ef5422824d098a9949022a624bf03fdca7e5502e55af5038";
        Process process = Runtime.getRuntime().exec(command);
        InputStream inputStream = process.getInputStream();

        Files.copy(inputStream, Paths.get("/home/adityasrivastava/Documents/tag.zip"));

        ZipFile zipFile = new ZipFile("/home/adityasrivastava/Documents/tag.zip");
        zipFile.extractAll(zipFile.getFile().getAbsolutePath().replace("tag", ""));

        System.out.println(zipFile.getFile().getAbsolutePath());
    }

    public static void hasLocalChanges(Git git) throws GitAPIException {
        Status status = git.status().call();

        Set<String> uncommittedChanges = status.getUncommittedChanges();
        if( uncommittedChanges.isEmpty() ) {
            System.out.println(false);
            return;
        }
        else
        {
            for( String uncommitted : uncommittedChanges )
              System.out.println(uncommitted);
        }
    }
}