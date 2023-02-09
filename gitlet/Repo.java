package gitlet;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;

public class Repo implements Serializable {

    /** Head commit in the current branch. */
    private Commit _head;

    /** Current stage name. */
    private Stage _stage;

    /** Current branch name. */
    private String _branch;

    /** TreeMap of all the split points of branches.
     * Key is the branch name created at the Commit,
     * Value is the initial commitID on this branch. */
    private ArrayList<String> _forks;

    /** Current Working Directory pointer. */
    static final File CWD = new File(System.getProperty("user.dir"));

    /** .gitlet folder to store all operating files. */
    static final File GITLET_FOLDER = Utils.join(CWD, ".gitlet");

    /** Repo folder to store all git repositories. */
    static final File REPO_FILE = Utils.join(GITLET_FOLDER, "repo");

    /** Commit folder to store all commit files. */
    static final File COMMIT_FOLDER = Utils.join(GITLET_FOLDER, "commits");

    /** Blob folder to store all added files with blobSHAID. */
    static final File BLOB_FOLDER = Utils.join(GITLET_FOLDER, "blobs");

    /** Branch folder to store all branches. */
    static final File BRANCH_FOLDER = Utils.join(GITLET_FOLDER, "branches");

    /** Repo class constructor. */
    public Repo() {
        _head = null;
        _stage = new Stage();
        _branch = "master";
        _forks = new ArrayList<>();
    }

    /** Returns the head commit of the Repo at the current branch.
     * @return current head commit
     * */
    public Commit getHead() {
        return _head;
    }

    /** Returns the Stage class of the current repo.
     * @return current staging area
     * */
    public Stage getStage() {
        return _stage;
    }

    /** Returns the current branch name.
     * @return current branch name
     * */
    public String getBranch() {
        return _branch;
    }

    /** Returns the current state of fork list.
     * @return split points list
     * */
    public ArrayList<String> getFork() {
        return _forks;
    }

    /** Updates the _head of the current repo.
     * @param newCommit new head commit
     * */
    public void updateHead(Commit newCommit) {
        _head = newCommit;
    }

    /** Updates the _stage of the current repo.
     * @param newStage new staging area
     * */
    public void updateStage(Stage newStage) {
        _stage = newStage;
    }

    /** Updates the _branch of the current repo.
     * @param newBranch new current branch name
     * */
    public void updateBranch(String newBranch) {
        _branch = newBranch;
    }

    /** Updates the _branch of the current repo.
     * @param commitID split commitID of a branch
     * */
    public void updateFork(String commitID) {
        _forks.add(commitID);
    }
}
