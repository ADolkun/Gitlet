package gitlet;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.ArrayList;
import java.util.Set;
import java.util.List;
import java.util.TreeMap;
import java.util.LinkedList;
import java.util.ArrayDeque;



public class RepoCommand {
    /**
     * Staging class of the current repo.
     */
    private static Stage _stage;

    /**
     * Current repository class.
     */
    private static Repo _repo;

    /**
     * Current head commit.
     */
    private static Commit _head;

    /**
     * Current branch name.
     */
    private static String _branch;

    /**
     * Current state of fork.
     */
    private static ArrayList<String> _fork;

    /** T/F for merge conflict. */
    private static boolean _conflict;
    /**
     * Standard length of a SHA1 ID.
     */
    private static final int SHA1LENGTH = 40;

    /**
     * RepoCommand class constructor.
     */
    public RepoCommand() {
        _repo = new Repo();
        _stage = _repo.getStage();
        _head = _repo.getHead();
        _branch = _repo.getBranch();
        _fork = _repo.getFork();
        _conflict = false;
    }

    /**
     * Creates a new Gitlet version-control system in the current directory.
     * This system will automatically start with one commit: a commit that
     * contains no files and has the commit message initial commit.
     */
    public static void init() throws IOException {
        new RepoCommand();
        Repo.GITLET_FOLDER.mkdir();
        Repo.COMMIT_FOLDER.mkdir();
        Repo.BLOB_FOLDER.mkdir();
        Repo.BRANCH_FOLDER.mkdir();

        Commit initialCommit = new Commit("initial commit",
                new TreeMap<>(), null, null, _repo);
        Utils.writeObject(Utils.join(Repo.COMMIT_FOLDER,
                initialCommit.getCommitID()), initialCommit);
        _repo.updateHead(initialCommit);

        _repo.updateFork(initialCommit.getCommitID());
        updateBranchHead("master", initialCommit);
        saveRepo(_repo);
    }

    /**
     * Reads all the settings of the current repository.
     *
     * @return editable current repo class
     */
    public static Repo readRepo() {
        return Utils.readObject(Repo.REPO_FILE, Repo.class);
    }

    /**
     * Saves all the setting of the current repo.
     *
     * @param currRepo current repository
     */
    public static void saveRepo(Repo currRepo) {
        Utils.writeObject(Repo.REPO_FILE, currRepo);
    }

    /**
     * Adds a copy of the file as it currently exists to the staging area.
     *
     * @param fileName file name
     */
    public static void add(String fileName) {
        Repo currRepo = readRepo();
        File addFile = Utils.join(currRepo.CWD, fileName);

        if (!addFile.exists()) {
            System.out.println("File does not exist.");
            System.exit(0);
        } else if (currRepo.getStage().getRemoveList().contains(fileName)) {
            Blob commitBlob = readBlob(currRepo.getHead(), fileName);
            String blobContent = commitBlob.getBlobContents();
            Utils.writeContents(addFile, blobContent);
            currRepo.getStage().getRemoveList().remove(fileName);
        } else if (currRepo.getHead().getBlobMap().containsKey(fileName)
                && readBlob(currRepo.getHead(), fileName).
                getBlobContents().equals(Utils.readContentsAsString(addFile))) {
            System.exit(0);
        } else {
            currRepo.getStage().toAddMap(fileName);
        }
        saveRepo(currRepo);
    }

    /**
     * Returns the blob from the blobMap of the given Commit.
     *
     * @param fromCommit commit to be read
     * @param fileName   file name
     */
    public static Blob readBlob(Commit fromCommit, String fileName) {
        String blobID = fromCommit.getBlobMap().get(fileName);
        File blobFile = Utils.join(Repo.BLOB_FOLDER, blobID);
        return Utils.readObject(blobFile, Blob.class);
    }

    /**
     * Returns the Commit using the CommitID in COMMIT_Folder.
     *
     * @param commitID SHA1 ID of the commit
     */
    public static Commit readCommit(String commitID) {
        File commitFile = Utils.join(Repo.COMMIT_FOLDER, commitID);
        return Utils.readObject(commitFile, Commit.class);
    }

    /**
     * Saves a snapshot of tracked files in the current commit and staging
     * area, so they can be restored at a later time, creating a new commit.
     *
     * @param msg commit message
     */
    public static void commit(String msg) {
        Repo currRepo = readRepo();
        _stage = currRepo.getStage();

        if (currRepo.getStage().getAddTree().isEmpty()
                && currRepo.getStage().getRemoveList().isEmpty()) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        } else if (msg.equals("")) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        for (String remove : _stage.getRemoveList()) {
            if (_stage.getAddTree().containsKey(remove)) {
                _stage.getAddTree().remove(remove);
            }
        }

        Commit newCommit = new Commit(msg, _stage.getAddTree(),
                null, currRepo.getHead(), currRepo);
        currRepo.updateHead(newCommit);
        Utils.writeObject(Utils.join(Repo.COMMIT_FOLDER,
                newCommit.getCommitID()), newCommit);
        _stage.clear();

        if (Utils.plainFilenamesIn(currRepo.BRANCH_FOLDER).size() == 1) {
            currRepo.updateFork(newCommit.getCommitID());
        }
        updateBranchHead(currRepo.getBranch(), newCommit);
        saveRepo(currRepo);
    }

    /**
     * Unstage the file if it is currently staged for addition.
     * If the file is tracked in the current commit, stage it for
     * removal and remove the file from the working directory if
     * the user has not already done so.
     *
     * @param fileName file name
     */
    public static void remove(String fileName) {
        Repo currRepo = readRepo();
        File rmFile = Utils.join(currRepo.CWD, fileName);
        Commit currCommit = currRepo.getHead();
        TreeMap<String, String> headBlobMap = currCommit.getBlobMap();
        TreeMap<String, String> currStage = currRepo.getStage().getAddTree();

        if (!rmFile.exists() && !headBlobMap.containsKey(fileName)) {
            System.out.println("File does not exist.");
            System.exit(0);
        } else {
            if (!headBlobMap.containsKey(fileName)
                    && currStage.containsKey(fileName)) {
                currRepo.getStage().getAddTree().remove(fileName);
            } else if (headBlobMap.containsKey(fileName)) {
                currRepo.getStage().toRemoveList(fileName);
                Utils.restrictedDelete(rmFile);
            } else {
                System.out.println("No reason to remove the file.");
                System.exit(0);
            }
        }
        saveRepo(currRepo);
    }

    /**
     * Prints the log info of the current Commit.
     *
     * @param currCommit current commit
     */
    public static void printLog(Commit currCommit) {
        System.out.println("===");
        System.out.println("commit " + currCommit.getCommitID());
        System.out.println("Date: " + currCommit.getTimestamp());
        System.out.println(currCommit.getMessage());
        System.out.println();
    }

    /**
     * Starting at the current head commit, display information about
     * each commit backwards along the commit tree until the initial commit,
     * following the first parent commit links, ignoring any second parents
     * found in merge commits.
     */
    public static void log() {
        Repo currRepo = readRepo();
        Commit currCommit = currRepo.getHead();
        while (currCommit != null) {
            printLog(currCommit);
            currCommit = currCommit.getParent();
        }
        saveRepo(currRepo);
    }

    /**
     * Like log, except displays information about all commits ever made.
     * The order of the commits does not matter.
     */
    public static void gLog() {
        List<String> commitFiles = Utils.plainFilenamesIn(Repo.COMMIT_FOLDER);
        for (String commitID : commitFiles) {
            printLog(readCommit(commitID));
        }
    }

    /**
     * Prints out the ids of all commits that have the given commit message,
     * one per line. If there are multiple such commits, it prints the ids out
     * on separate lines. The commit message is a single operand;
     * to indicate a multiword message, put the operand in quotation marks,
     * as for the commit command above.
     *
     * @param commitMsg commit message
     */
    public static void find(String commitMsg) {
        int counter = 0;
        List<String> commitFiles = Utils.plainFilenamesIn(Repo.COMMIT_FOLDER);
        for (String commitFile : commitFiles) {
            Commit currCommit = readCommit(commitFile);
            if (currCommit.getMessage().equals(commitMsg)) {
                System.out.println(currCommit.getCommitID());
                counter++;
            }
        }
        if (counter == 0) {
            System.out.println("Found no commit with that message.");
            System.exit(0);
        }
    }

    /**
     * Displays what branches currently exist, and marks the current
     * branch with a *. Also displays what files have been staged for
     * addition or removal. An example of the exact format it should
     * follow is as follows.
     */
    public static void status() {
        Repo currRepo = readRepo();
        _head = currRepo.getHead();
        _stage = currRepo.getStage();

        List<String> branchFiles = Utils.plainFilenamesIn(
                currRepo.BRANCH_FOLDER);
        ArrayList<String> addFiles = new ArrayList<>(
                _stage.getAddTree().keySet());
        ArrayList<String> removeFiles = _stage.getRemoveList();

        Collections.sort(branchFiles);
        Collections.sort(addFiles);
        Collections.sort(removeFiles);

        System.out.println("=== Branches ===");
        for (String branchName : branchFiles) {
            if (branchName.equals(currRepo.getBranch())) {
                System.out.println("*" + currRepo.getBranch());
            } else {
                System.out.println(branchName);
            }
        }

        System.out.println();
        System.out.println("=== Staged Files ===");
        for (String stagedFile : addFiles) {
            System.out.println(stagedFile);
        }
        System.out.println();
        System.out.println("=== Removed Files ===");
        for (String removeFile : removeFiles) {
            System.out.println(removeFile);
        }
        System.out.println();
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();

        System.out.println("=== Untracked Files ===");
        for (String untrackedFile : untracked(currRepo.CWD)) {
            System.out.println(untrackedFile);
        }
        System.out.println();
    }

    /**
     * Returns the correct commitID if the passed
     * in SHA1 is the first six digits.
     *
     * @param commitID SHA1 ID of the commit
     * @param currRepo current repository
     */
    public static String fixIDLength(String commitID, Repo currRepo) {
        List<String> commitFiles =
                Utils.plainFilenamesIn(currRepo.COMMIT_FOLDER);
        if (commitID.length() != SHA1LENGTH) {
            for (String commitName : commitFiles) {
                if (commitName.startsWith(commitID)) {
                    commitID = commitName;
                }
            }
        }
        return commitID;
    }

    /**
     * Prints out a message if the given CommitID is not
     * in the given Repo.
     *
     * @param commitID SHA1 ID of the commit
     * @param currRepo current repository
     */
    public static void commDExist(String commitID, Repo currRepo) {
        List<String> commitFiles =
                Utils.plainFilenamesIn(currRepo.COMMIT_FOLDER);
        if (!commitFiles.contains(commitID)) {
            System.out.println("No commit with that id exists.");
            System.exit(0);
        }
    }

    /**
     * Prints out a message if the file is not in the given Commit.
     *
     * @param currCommit current commit
     * @param fileName   file name
     */
    public static void fileDExist(Commit currCommit, String fileName) {
        if (!currCommit.getBlobMap().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }
    }

    /**
     * Returns the untracked files in the given directory as a ArrayList.
     *
     * @param directory current working directory
     */
    public static ArrayList<String> untracked(File directory) {
        Repo currRepo = readRepo();
        List<String> dirFiles = Utils.plainFilenamesIn(directory);
        ArrayList<String> untracked = new ArrayList<>();
        for (String dirFile : dirFiles) {
            if (!currRepo.getStage().getAddTree().containsKey(dirFile)
                    && !currRepo.getHead().getBlobMap().containsKey(dirFile)) {
                untracked.add(dirFile);
            }
        }
        return untracked;
    }

    /**
     * Prints a message if an untracked file exist.
     *
     * @param untrackedFiles untracked files in the directory
     */
    public static void printUntracked(ArrayList<String> untrackedFiles) {
        if (!untrackedFiles.isEmpty()) {
            System.out.println("There is an untracked file in the way; "
                    + "delete it, or add and commit it first.");
            System.exit(0);
        }
    }

    /**
     * Returns the Head Commit of the given branch.
     *
     * @param branchName branch name
     * @param currRepo   current repo
     */
    public static Commit readBranch(String branchName, Repo currRepo) {
        File branchFile = Utils.join(currRepo.BRANCH_FOLDER, branchName);
        return Utils.readObject(branchFile, Commit.class);
    }

    /**
     * Restores the directory with the given arguments.
     *
     * @param args checkout arguments
     */
    public static void checkout(String[] args) {
        Repo currRepo = readRepo();
        _head = currRepo.getHead();

        if (args.length == 3) {
            String fileName = args[2];
            fileDExist(_head, fileName);
            File overWrite = Utils.join(Repo.CWD, fileName);
            Blob blob = readBlob(_head, fileName);
            String blobContent = blob.getBlobContents();
            Utils.writeContents(overWrite, blobContent);
        } else if (args.length == 4) {
            String commitID = fixIDLength(args[1], currRepo);
            String fileName = args[3];
            commDExist(commitID, currRepo);
            fileDExist(_head, fileName);
            File overWrite = Utils.join(Repo.CWD, fileName);
            Commit fromCommit = readCommit(commitID);
            Blob blob = readBlob(fromCommit, fileName);
            String blobContent = blob.getBlobContents();
            Utils.writeContents(overWrite, blobContent);
        } else if (args.length == 2) {
            String branchName = args[1];
            File bFolder = currRepo.BRANCH_FOLDER;
            List<String> bFiles = Utils.plainFilenamesIn(bFolder);
            if (!bFiles.contains(branchName)) {
                System.out.println("No such branch exists.");
                System.exit(0);
            }
            if (branchName.equals(currRepo.getBranch())) {
                System.out.println("No need to checkout the current branch.");
                System.exit(0);
            }
            printUntracked(untracked(currRepo.CWD));

            File newBFile = Utils.join(bFolder, branchName);
            Commit newBHead = Utils.readObject(newBFile, Commit.class);

            replaceFiles(newBHead, currRepo);

            currRepo.updateBranch(branchName);
        }
        saveRepo(currRepo);
    }

    /** replaces the CWD files with the given commit files.
     * @param newCommit commit that provides files
     * @param currRepo current repository
     * */
    public static void replaceFiles(Commit newCommit, Repo currRepo) {

        List<String> dirFiles = Utils.plainFilenamesIn(currRepo.CWD);
        TreeMap<String, String> newBlobMap = newCommit.getBlobMap();
        Set<String> newBlobFiles = newBlobMap.keySet();

        for (String dirFile : dirFiles) {
            if (!newBlobMap.containsKey(dirFile)) {
                Utils.restrictedDelete(dirFile);
            }
        }
        for (String fileName : newBlobFiles) {
            Utils.writeContents(Utils.join(currRepo.CWD, fileName),
                    readBlob(newCommit, fileName).getBlobContents());
        }
        currRepo.updateHead(newCommit);
        currRepo.getStage().clear();
    }

    public static void existInAll(Set<String> currBlobFiles,
                                  Set<String> givenBlobFiles,
                                  Set<String> splitBlobFiles,
                                  Commit splitCommit, Commit currCommit,
                                  Commit givenBCommit, Commit newCommit,
                                  Repo currRepo) throws IOException {
        for (String splitFile : splitBlobFiles) {
            if (currBlobFiles.contains(splitFile)) {
                for (String currFile : currBlobFiles) {
                    if (givenBlobFiles.contains(splitFile)) {
                        for (String givenFile : givenBlobFiles) {
                            if (splitFile.equals(currFile) && splitFile.
                                    equals(givenFile)) {
                                if (readBlob(splitCommit, splitFile).
                                        getBlobContents().equals(
                                        readBlob(currCommit, currFile).
                                                getBlobContents())
                                        && !readBlob(splitCommit, splitFile).
                                        getBlobContents().equals(
                                        readBlob(givenBCommit, givenFile).
                                                getBlobContents())) {
                                    newCommit.addBlob(givenFile, readBlob(
                                            givenBCommit, givenFile));
                                } else if (!readBlob(splitCommit, splitFile).
                                        getBlobContents().equals(
                                        readBlob(currCommit, currFile).
                                                getBlobContents())
                                        && readBlob(splitCommit, splitFile).
                                        getBlobContents().equals(
                                        readBlob(givenBCommit, givenFile).
                                                getBlobContents())) {
                                    newCommit.addBlob(currFile, readBlob(
                                            currCommit, currFile));
                                } else if (!readBlob(splitCommit, splitFile).
                                        getBlobContents().equals(
                                        readBlob(currCommit, currFile).
                                                getBlobContents())
                                        && readBlob(currCommit, currFile).
                                        getBlobContents().equals(
                                        readBlob(givenBCommit, givenFile).
                                                getBlobContents())) {
                                    newCommit.addBlob(currFile, readBlob(
                                            currCommit, currFile));
                                }
                            }
                        }
                    } else if (!givenBlobFiles.contains(splitFile)
                            && readBlob(splitCommit, splitFile)
                            .getBlobContents().equals(readBlob(currCommit,
                                    currFile).getBlobContents())) {
                        newCommit.removeBlob(currFile);
                    }
                }
            }
        }
    }

    public static void conflictCheck(Set<String> currBlobFiles,
                                     Set<String> givenBlobFiles,
                                     Set<String> splitBlobFiles,
                                     Commit splitCommit, Commit currCommit,
                                     Commit givenBCommit,
                                     Commit newCommit, Repo currRepo)
            throws IOException {
        for (String splitFile : splitBlobFiles) {
            if (currBlobFiles.contains(splitFile)) {
                for (String currFile : currBlobFiles) {
                    if (givenBlobFiles.contains(splitFile)) {
                        for (String givenFile : givenBlobFiles) {
                            if (splitFile.equals(currFile) && splitFile.
                                    equals(givenFile)) {
                                if (!readBlob(splitCommit, splitFile).
                                        getBlobContents().equals(
                                                readBlob(currCommit, currFile).
                                                        getBlobContents())
                                        && !readBlob(splitCommit, splitFile).
                                        getBlobContents().equals(readBlob(
                                                givenBCommit, givenFile).
                                                        getBlobContents())
                                        && !readBlob(currCommit, currFile).
                                        getBlobContents().equals(readBlob(
                                                givenBCommit, givenFile).
                                                        getBlobContents())) {
                                    conflictHelp2(currCommit, givenBCommit,
                                            newCommit, currRepo, splitFile,
                                            currFile, givenFile);
                                }
                            }
                        }
                    }
                }
            }
        }
        for (String split : splitBlobFiles) {
            if (currBlobFiles.contains(split) && !givenBlobFiles.
                    contains(split)) {
                conflictHelp(currBlobFiles, splitCommit,
                        currCommit, currCommit, newCommit,
                        currRepo, split);
            } else if (!currBlobFiles.contains(split) && givenBlobFiles.
                    contains(split)) {
                conflictHelp(givenBlobFiles, splitCommit,
                        currCommit, givenBCommit, newCommit,
                        currRepo, split);
            }
        }
        for (String currFile : currBlobFiles) {
            if (!splitBlobFiles.contains(currFile) && givenBlobFiles.contains(
                    currFile)) {
                for (String givenFile : givenBlobFiles) {
                    if (currFile.equals(givenFile)
                            && !readBlob(currCommit, currFile).getBlobContents()
                            .equals(readBlob(givenBCommit, givenFile)
                                    .getBlobContents())) {
                        conflictHelp2(currCommit, givenBCommit, newCommit,
                                currRepo, currFile, currFile, givenFile);
                    }
                }
            }
        }
    }

    public static void conflictHelp2(Commit currCommit, Commit givenBCommit,
                                     Commit newCommit, Repo currRepo,
                                     String splitFile, String currFile,
                                     String givenFile) throws IOException {
        _conflict = true;
        File conflictFile = Utils.join(
                currRepo.CWD, splitFile);
        conflictFile.createNewFile();
        String conflictContent = "<<<<<<< HEAD\n"
                + readBlob(currCommit, currFile)
                .getBlobContents()
                + "=======\n"
                + readBlob(givenBCommit, givenFile)
                .getBlobContents()
                + ">>>>>>>\n";
        Utils.writeContents(conflictFile,
                conflictContent);
        Blob conflictBlob = new Blob(Utils.join(
                Repo.CWD, splitFile));
        newCommit.addBlob(splitFile, conflictBlob);
    }

    public static void conflictHelp(Set<String> givenBlobFiles,
                                    Commit splitCommit, Commit currCommit,
                                    Commit givenBCommit, Commit newCommit,
                                    Repo currRepo,
                                    String split) throws IOException {
        for (String givenFile : givenBlobFiles) {
            if (split.equals(givenFile)
                    && !readBlob(splitCommit, givenFile).
                    getBlobContents().equals(readBlob(givenBCommit,
                            givenFile).getBlobContents())) {
                _conflict = true;
                File conflictFile = Utils.join(currRepo.CWD, givenFile);
                conflictFile.createNewFile();
                String conflictContent = "<<<<<<< HEAD\n" + readBlob(
                        currCommit, givenFile).getBlobContents()
                        + "=======\n" + ">>>>>>>\n";
                Utils.writeContents(conflictFile, conflictContent);
                Blob conflictBlob = new Blob(Utils.join(Repo.CWD,
                        givenFile));
                newCommit.addBlob(givenFile, conflictBlob);
            }
        }
    }

    /** Replaces the directory wit the given branch commit files.
     * @param splitCommit split point commit
     * @param currCommit current branch head commit
     * @param givenBCommit given branch head commit
     * @param givenBranch given branch name
     * @param currRepo Current repo
     * */
    public static void mergeConditions(Commit splitCommit, Commit currCommit,
                                       Commit givenBCommit, String givenBranch,
                                       Repo currRepo) throws IOException {
        Set<String> currBlobFiles = currCommit.getBlobKeys();
        Set<String> givenBlobFiles = givenBCommit.getBlobKeys();
        Set<String> splitBlobFiles = splitCommit.getBlobKeys();

        Commit newCommit = new Commit("Merged " + givenBranch + " into "
                + currRepo.getBranch() + ".", new TreeMap<>(),
                givenBCommit, currCommit, currRepo);

        existInAll(currBlobFiles, givenBlobFiles, splitBlobFiles, splitCommit,
                currCommit, givenBCommit, newCommit, currRepo);
        conflictCheck(currBlobFiles, givenBlobFiles, splitBlobFiles,
                splitCommit, currCommit, givenBCommit, newCommit, currRepo);

        for (String currFile : currBlobFiles) {
            if (!splitBlobFiles.contains(currFile)
                    && !givenBlobFiles.contains(currFile)) {
                newCommit.addBlob(currFile, readBlob(currCommit, currFile));
            }
        }
        for (String givenFile : givenBlobFiles) {
            if (!splitBlobFiles.contains(givenFile)
                    && !currBlobFiles.contains(givenFile)) {
                newCommit.addBlob(givenFile, readBlob(givenBCommit, givenFile));
            }
        }
        for (String splitFile : splitBlobFiles) {
            if (givenBlobFiles.contains(splitFile)
                    && !currBlobFiles.contains(splitFile)
                    && readBlob(splitCommit, splitFile).getBlobContents()
                    .equals(readBlob(givenBCommit, splitFile)
                                    .getBlobContents())) {
                newCommit.removeBlob(splitFile);
            }
        }
        Utils.writeObject(Utils.join(Repo.COMMIT_FOLDER,
                newCommit.getCommitID()), newCommit);

        replaceFiles(newCommit, currRepo);
        updateBranchHead(currRepo.getBranch(), newCommit);
    }

    /**
     * Updates the given branch with the currCommit.
     *
     * @param branchName branch name
     * @param currCommit current commit
     */
    public static void updateBranchHead(String branchName, Commit currCommit) {
        File currBranch = Utils.join(Repo.BRANCH_FOLDER, branchName);
        Utils.writeObject(currBranch, currCommit);
    }

    /**
     * Creates a branch with the given name at the current Commit.
     *
     * @param branchName branch name
     */
    public static void branch(String branchName) {
        Repo currRepo = readRepo();
        File branchFile = new File(Repo.BRANCH_FOLDER, branchName);
        if (branchFile.exists()) {
            System.out.println("A branch with that name already exists.");
            System.exit(0);
        } else {
            currRepo.updateFork(currRepo.getHead().getCommitID());
            updateBranchHead(branchName, currRepo.getHead());
        }
        saveRepo(currRepo);
    }

    /**
     * Prints a message if the given branch does doesn't exist.
     *
     * @param branchName branch name
     * @param currRepo   current repo
     */
    public static void branchDExist(String branchName, Repo currRepo) {
        File branchFile = Utils.join(currRepo.BRANCH_FOLDER, branchName);
        if (!branchFile.exists()) {
            System.out.println("A branch with that name does not exist.");
            System.exit(0);
        }
    }

    /**
     * Deletes the branch with the given name.
     * This only means to delete the pointer associated with the branch;
     * it does not mean to delete all commits that were created
     * under the branch, or anything like that.
     *
     * @param branchName branch name
     */
    public static void rmBranch(String branchName) {
        Repo currRepo = readRepo();
        File branchFile = Utils.join(currRepo.BRANCH_FOLDER, branchName);

        branchDExist(branchName, currRepo);
        if (branchName.equals(currRepo.getBranch())) {
            System.out.println("Cannot remove the current branch.");
            System.exit(0);
        } else {
            branchFile.delete();
        }
        saveRepo(currRepo);
    }

    /**
     * Checks out all the files tracked by the given commit.
     * Removes tracked files that are not present in that commit.
     * Also moves the current branch's head to that commit node.
     * The [commit id] may be abbreviated as for checkout.
     *
     * @param commitID SHA1 of commit
     */
    public static void reset(String commitID) {
        Repo currRepo = readRepo();
        String cID = fixIDLength(commitID, currRepo);
        commDExist(cID, currRepo);
        printUntracked(untracked(currRepo.CWD));

        Commit newCommit = readCommit(commitID);
        TreeMap<String, String> newBlobMap = newCommit.getBlobMap();
        List<String> dirFiles = Utils.plainFilenamesIn(currRepo.CWD);
        Set<String> newStageFiles = newBlobMap.keySet();

        for (String dirFile : dirFiles) {
            if (!newBlobMap.containsKey(dirFile)) {
                Utils.restrictedDelete(dirFile);
            }
        }
        for (String fileName : newStageFiles) {
            Utils.writeContents(Utils.join(currRepo.CWD, fileName),
                    readBlob(newCommit, fileName).getBlobContents());
        }
        updateBranchHead(currRepo.getBranch(), newCommit);
        currRepo.updateHead(newCommit);
        currRepo.getStage().clear();
        saveRepo(currRepo);
    }

    /**
     * Returns the original ancestors and merge ancestors
     * of the given commit as a LinkedList.
     *
     * @param commit Current commit
     * @return A list of SHA1 IDs of given commits' ancestors.
     */
    public static LinkedList<String> getAncestors(Commit commit) {
        LinkedList<String> ancestors = new LinkedList<>();
        if (commit == null) {
            return new LinkedList<>();
        } else {
            ancestors.addAll(getAncestors(commit.getParent()));
            ancestors.addAll(getAncestors(commit.getMergeParent()));
            if (commit.getParent() != null) {
                ancestors.add(commit.getParent().getCommitID());
            } else if (commit.getMergeParent() != null) {
                ancestors.add(commit.getMergeParent().getCommitID());
            }
        }
        return ancestors;
    }

    /** Returns the splitPoint Commit of the given two branches.
     * Returns null otherwise.
     * @param currBranch Current master branch name
     * @param givenBranch Given branch name
     * @param currRepo Current repository
     * */
    public static Commit specialSplit(String currBranch,
                                      String givenBranch, Repo currRepo) {
        Commit currBCommit = readBranch(currBranch, currRepo);
        Commit givenBCommit = readBranch(givenBranch, currRepo);
        ArrayList<String> pastPoints = new ArrayList<>();
        while (currBCommit != null) {
            for (String splitID : currRepo.getFork()) {
                if (splitID.equals(currBCommit.getCommitID())) {
                    pastPoints.add(splitID);
                }
            }
            currBCommit = currBCommit.getParent();
        }

        Collections.reverse(pastPoints);
        while (givenBCommit != null) {
            for (String past : pastPoints) {
                if (past.equals(givenBCommit.getCommitID())) {
                    return givenBCommit;
                }
            }
            givenBCommit = givenBCommit.getParent();
        }
        return null;
    }

    /**
     * Returns the splitPoint Commit of the given two branches.
     * Returns null otherwise.
     *
     * @param currBranch  current branch
     * @param givenBranch given branch
     * @param currRepo    current repo
     */
    public static Commit splitPoint(String currBranch,
                                    String givenBranch, Repo currRepo) {

        Commit currBCommit = readBranch(currBranch, currRepo);
        Commit givenBCommit = readBranch(givenBranch, currRepo);

        LinkedList<String> givenBCAncestIDs = getAncestors(givenBCommit);
        LinkedList<String> splitIDs = new LinkedList<>();
        ArrayDeque<String> sha1Q = new ArrayDeque<>();

        String currBCID = currBCommit.getCommitID();

        sha1Q.add(currBCID);
        while (!sha1Q.isEmpty()) {
            String tempCID = sha1Q.pop();
            if (givenBCAncestIDs.contains(tempCID)) {
                splitIDs.add(tempCID);
            }
            if (readCommit(tempCID).getParent() != null) {
                String parentCID = readCommit(tempCID).getParent().
                        getCommitID();
                sha1Q.add(parentCID);
            }
            if (readCommit(tempCID).getMergeParent() != null) {
                String mergeParentCID = readCommit(tempCID).
                        getMergeParent().getCommitID();
                sha1Q.add(mergeParentCID);
            }
        }
        return readCommit(splitIDs.get(0));
    }

    /**
     * Merges files from the given branch into the current branch.
     *
     * @param branchName branch name
     */
    public static void merge(String branchName) throws IOException {
        Repo currRepo = readRepo();
        String[] args = {"checkout", currRepo.getBranch()};
        if (!currRepo.getStage().getAddTree().isEmpty()
                || !currRepo.getStage().getRemoveList().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            System.exit(0);
        }
        branchDExist(branchName, currRepo);
        if (currRepo.getBranch().equals(branchName)) {
            System.out.println("Cannot merge a branch with itself.");
            System.exit(0);
        }
        printUntracked(untracked(currRepo.CWD));

        Commit currCommit = currRepo.getHead();
        Commit givenBCommit = readBranch(branchName, currRepo);
        Commit splitCommit = splitPoint(currRepo.getBranch(),
                branchName, currRepo);
        Commit specialSplit = specialSplit(currRepo.getBranch(),
                branchName, currRepo);

        String currBCID = currCommit.getCommitID();
        String givenBCID = givenBCommit.getCommitID();

        if (specialSplit.getCommitID().equals(currBCID)) {
            args[1] = branchName;
            checkout(args);
            System.out.println("Current branch fast-forwarded.");
            System.exit(0);
        } else if (specialSplit.getCommitID().equals(givenBCID)) {
            System.out.println(
                    "Given branch is an ancestor of the current branch.");
            System.exit(0);
        } else {
            _conflict = false;

            mergeConditions(splitCommit, currCommit, givenBCommit,
                    branchName, currRepo);

            if (_conflict) {
                System.out.println("Encountered a merge conflict.");
            }

            saveRepo(currRepo);
        }
    }
}
