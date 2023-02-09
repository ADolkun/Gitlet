package gitlet;

import java.io.IOException;

/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Abdumijit A. Dolkun
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) throws IOException {
        if (args.length == 0) {
            commandError();
        } else if (args[0].equals("init")) {
            initCheck();
        } else if (!Repo.GITLET_FOLDER.exists()) {
            initError();
        } else {
            switch (args[0]) {
            case "add":
                RepoCommand.add(args[1]);
                break;
            case "commit":
                RepoCommand.commit(args[1]);
                break;
            case "rm":
                RepoCommand.remove(args[1]);
                break;
            case "log":
                RepoCommand.log();
                break;
            case "global-log":
                RepoCommand.gLog();
                break;
            case "find":
                RepoCommand.find(args[1]);
                break;
            case "status":
                RepoCommand.status();
                break;
            case "checkout":
                checkout(args);
                break;
            case "branch":
                RepoCommand.branch(args[1]);
                break;
            case "rm-branch":
                RepoCommand.rmBranch(args[1]);
                break;
            case "reset":
                RepoCommand.reset(args[1]);
                break;
            case "merge":
                RepoCommand.merge(args[1]);
                break;
            default:
                notFound();
            }
        }
    }
    public static void commandError() {
        System.out.println("Please enter a command.");
        System.exit(0);
    }

    public static void initCheck() throws IOException {
        if (Repo.GITLET_FOLDER.exists()) {
            System.out.println("A Gitlet version-control system "
                    + "already exists in the current directory.");
            System.exit(0);
        }
        RepoCommand.init();
    }

    public static void initError() {
        System.out.println("Not in an initialized Gitlet directory.");
        System.exit(0);
    }
    public static void checkout(String[] args) {
        int argLength = args.length;
        if (argLength != 2 && argLength != 3 && argLength != 4) {
            System.out.println("Incorrect Operands");
            System.exit(0);
        } else if ((argLength == 3 && !args[1].equals("--"))
                || (argLength == 4 && !args[2].equals("--"))) {
            System.out.println("Incorrect Operands");
            System.exit(0);
        } else {
            RepoCommand.checkout(args);
        }
    }

    public static void notFound() {
        System.out.println("No command with that name exists.");
        System.exit(0);
    }
}
