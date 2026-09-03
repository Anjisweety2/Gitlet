package gitlet;
import static java.lang.System.exit;
import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.TreeSet;
import java.util.Stack;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;


/** Driver class for Gitlet, the tiny stupid version-control system.
 *  @author Anji
 */
public class Main {
    /** Current Working Directory. */
    static final File CWD = new File(".");
    /** GitLet repo directory. */
    static final File REPO = Utils.join(CWD, ".gitlet");
    /** GitLet branch directory. */
    static final File BRANCHES = Utils.join(REPO, "Branches");
    /** GitLet current branch file. */
    static final File HEAD = Utils.join(REPO, "HEAD");
    /** GitLet staging area directory. */
    static final File STAGE = Utils.join(REPO, "StagingArea");
    /** GitLet ALL the commits directory. */
    static final File COMMITS = Utils.join(REPO, "Commits");
    /** GitLet ALL the blobs. */
    static final File BLOBS = Utils.join(REPO, "Blobs");
    /** GitLet ALL the remote repos directory. */
    static final File REMOTES = Utils.join(REPO, "Remotes");

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND> .... */
    public static void main(String... args) {
        if (args == null || args.length == 0) {
            System.out.println("Please enter a command.");
            exit(0);
        } else {
            if (args[0].equals("init")) {
                init(args);
            } else {
                if (REPO.exists()) {
                    switch (args[0]) {
                    case "add":
                        add(args); break;
                    case "commit":
                        commit(args); break;
                    case "rm":
                        rm(args); break;
                    case "log":
                        log(args); break;
                    case "global-log":
                        globalLog(args); break;
                    case "find":
                        find(args); break;
                    case "status":
                        status(args); break;
                    case "checkout":
                        checkout(args); break;
                    case "branch":
                        branch(args); break;
                    case "rm-branch":
                        rmBranch(args); break;
                    case "reset":
                        reset(args); break;
                    case "merge":
                        merge(args); break;
                    case "add-remote":
                        addRemote(args); break;
                    case "rm-remote":
                        rmRemote(args); break;
                    case "fetch":
                        fetch(args); break;
                    case "push":
                        push(args); break;
                    case "pull":
                        pull(args); break;
                    default:
                        System.out.println(
                                "No command with that name exists.");
                        exit(0);
                    }
                } else {
                    System.out.println(
                            "Not in an initialized Gitlet directory.");
                    exit(0);
                }
            }
        }
    }

    /** initialize the repository. Make sure that only init once.
     * where ARGS contains [init] .... */
    public static void init(String[] args) {
        try {
            if (REPO.exists()) {
                System.out.println(
                        "A Gitlet version-control system "
                                + "already exists in the current directory.");
            }
            checkArgsOne(args);
            REPO.mkdir();
            STAGE.createNewFile();
            BRANCHES.mkdir();
            COMMITS.mkdir();
            BLOBS.mkdir();
            REMOTES.mkdir();
            HEAD.createNewFile();

            stage = new StagingArea();
            stage.saveStage();
            String commitDate = "Wed Dec 31 16:00:00 1969 -0800";
            Commit initial = new Commit(null, "initial commit", commitDate);
            initial.saveCommit();

            currentCommit = initial;
            Utils.writeObject(HEAD, currentCommit);

            Branch newBranch = new Branch("master", currentCommit);
            newBranch.setHead(true);
            newBranch.saveBranch();

        } catch (IOException ioe) {
            System.out.println("IOE exception");
            exit(0);
        }

    }

    /**add to the staging area, where ARGS contains [add] [filename].*/
    public static void add(String[] args) {
        checkArgsTwo(args);
        String fileName = args[1];
        File file = Utils.join(CWD, fileName);
        if (file.exists()) {
            String sha1 = Utils.sha1(fileName,
                    Utils.sha1(Utils.readContentsAsString(file)));
            currentCommit = Utils.readObject(HEAD, Commit.class);
            stage = StagingArea.fromFile();
            String blobid = currentCommit.getTree().get(fileName);

            if (blobid != null) {
                if (sha1.equals(blobid)) {
                    if (stage.getAdd().get(fileName) != null) {
                        stage.getAdd().remove(fileName);
                    }
                    if (stage.getRemoval().contains(fileName)) {
                        stage.getRemoval().remove(fileName);
                    }
                    Utils.writeObject(STAGE, stage);
                    return;
                } else {
                    if (stage.getRemoval().contains(fileName)) {
                        stage.getRemoval().remove(fileName);
                    }
                }
            }
            if (stage.getRemoval().contains(fileName)) {
                stage.getRemoval().remove(fileName);
            }
            Blob newBlob = new Blob(fileName);
            newBlob.saveBlob();
            stage.add(fileName, newBlob);
            Utils.writeObject(STAGE, stage);
        } else {
            System.out.println("File does not exist.");
            exit(0);
        }

    }

    /**Mimic commit, where ARGS contains [commit] [message]. */
    public static void commit(String [] args) {
        if (args.length == 1 || args[1].equals("")) {
            System.out.println("Please enter a commit message.");
            exit(0);
        }
        checkArgsTwo(args);
        date = new Date();
        SimpleDateFormat formatDate =
                new SimpleDateFormat("E MMM dd hh:mm:ss yyyy -0800");
        String commitDate = formatDate.format(date);
        currentCommit = Utils.readObject(HEAD, Commit.class);
        stage = StagingArea.fromFile();
        if (stage.getRemoval().isEmpty() && stage.getAdd().isEmpty()) {
            System.out.println("No changes added to the commit.");
            exit(0);
        }
        Commit newCommit = new Commit(currentCommit, args[1], commitDate);
        newCommit.saveCommit();
        Utils.writeObject(HEAD, newCommit);
        for (File branch : BRANCHES.listFiles()) {
            Branch thisBranch = Utils.readObject(branch, Branch.class);
            if (thisBranch.isHeadBranch()) {
                thisBranch.setTotalCurCommit(newCommit);
                thisBranch.saveAlreadyBranch();
                break;
            }
        }
    }

    /**Mimic rm, where ARGS contains [rm] [fileName].*/
    public static void rm(String [] args) {
        checkArgsTwo(args);
        String fileName = args[1];
        currentCommit = Utils.readObject(HEAD, Commit.class);
        stage = Utils.readObject(STAGE, StagingArea.class);
        boolean right = false;
        if (stage.getAdd().get(fileName) != null) {
            stage.getAdd().remove(fileName);
            Utils.writeObject(STAGE, stage);
            right = true;
        }
        if (currentCommit.getTree().get(fileName) != null) {
            stage.untrack(fileName);
            Utils.restrictedDelete(fileName);
            Utils.writeObject(STAGE, stage);
            right = true;
        }
        if (!right) {
            System.out.println("No reason to remove the file.");
            exit(0);
        }
    }

    /**Display the commit history of the current branch.
     * where ARGS contains [log]. */
    public static void log(String [] args) {
        checkArgsOne(args);
        String result = "";
        currentCommit = Utils.readObject(HEAD, Commit.class);
        while (currentCommit.getParent() != null) {
            result += "===\n";
            result += "commit " + currentCommit.getSha1ID() + "\n";
            if (currentCommit.getSecondParent() != null) {
                result += "Merge: " + currentCommit.
                        sevenDigit(currentCommit.getParent());
                result += " " + currentCommit.
                        sevenDigit(currentCommit.getSecondParent()) + "\n";
            }
            result += "Date: " + currentCommit.getDate() + "\n";
            result += currentCommit.getmsg() + "\n\n";

            File next = Utils.join(COMMITS, currentCommit.getParent());
            currentCommit = Utils.readObject(next, Commit.class);
        }
        result += "===\n";
        result += "commit " + currentCommit.getSha1ID() + "\n";
        result += "Date: " + currentCommit.getDate() + "\n";
        result += currentCommit.getmsg() + "\n\n";
        System.out.print(result);

    }

    /**Display all commit history, where ARGS contains [global-log].*/
    public static void globalLog(String [] args) {
        checkArgsOne(args);
        String result = "";
        if (COMMITS.listFiles() != null) {
            for (File commit : COMMITS.listFiles()) {
                Commit thisCommit = Utils.readObject(commit, Commit.class);
                result += "===\n";
                result += "commit " + thisCommit.getSha1ID() + "\n";
                if (thisCommit.getSecondParent() != null) {
                    result += "Merge: " + thisCommit.
                            sevenDigit(thisCommit.getParent());
                    result += " " + thisCommit.
                            sevenDigit(thisCommit.getSecondParent()) + "\n";
                }
                result += "Date: " + thisCommit.getDate() + "\n";
                result += thisCommit.getmsg() + "\n\n";
            }
        }

        System.out.print(result);
    }

    /**Find all the commits with commit message.
     * where ARGS contains [find] [commit msg].*/
    public static void find(String [] args) {
        checkArgsTwo(args);
        String result = "";
        if (COMMITS.listFiles() != null) {
            for (File commit : COMMITS.listFiles()) {
                Commit thisCommit = Utils.readObject(commit, Commit.class);
                if (args[1].equals(thisCommit.getmsg())) {
                    result += thisCommit.getSha1ID() + "\n";
                }
            }
        }

        if (result.equals("")) {
            System.out.println("Found no commit with that message.");
            exit(0);
        }
        System.out.print(result);
    }

    /** Display the current status, where ARGS contains [status].*/
    public static void status(String [] args) {
        checkArgsOne(args); String result = ""; result += "=== Branches ===\n";
        File [] files = BRANCHES.listFiles(); Arrays.sort(files);
        for (File branch : files) {
            Branch thisbranch = Utils.readObject(branch, Branch.class);
            if (thisbranch.isHeadBranch()) {
                result += "*";
            }
            result += thisbranch.getName() + "\n";
        }
        result += "\n=== Staged Files ===\n"; stage = StagingArea.fromFile();
        for (String fileName : stage.getAdd().keySet()) {
            result += fileName + "\n";
        }
        result += "\n=== Removed Files ===\n";
        for (String fileName : stage.getRemoval()) {
            result += fileName + "\n";
        }
        result += "\n=== Modifications Not Staged For Commit ===\n";
        currentCommit = Commit.headfromFile();
        TreeSet<String> fff = new TreeSet<>();
        for (String fileName : currentCommit.getTree().keySet()) {
            String blobid = currentCommit.getTree().get(fileName);
            File file = Utils.join(CWD, fileName);
            if (file.exists()) {
                String sha1 = Utils.sha1(fileName,
                        Utils.sha1(Utils.readContentsAsString(file)));
                if (!blobid.equals(sha1) && !stage.getAdd().
                        containsKey(fileName)) {
                    fff.add(file.getName() + " (modified)");
                }
            } else {
                if (!stage.getRemoval().contains(fileName)) {
                    fff.add(fileName + " (deleted)");
                }
            }
        }
        for (String fileName : stage.getAdd().keySet()) {
            if (Utils.join(CWD, fileName).exists()) {
                String sha1 = Utils.sha1(fileName, Utils.sha1(Utils.
                        readContentsAsString(Utils.join(CWD, fileName))));
                if (!stage.getAdd().get(fileName).equals(sha1)) {
                    fff.add(fileName + " (modified)");
                }
            } else {
                fff.add(fileName + " (deleted)");
            }
        }
        if (!fff.isEmpty()) {
            for (String item : fff) {
                result += item + "\n";
            }
        }
        result += findUntrackedFile(); System.out.print(result);
    }

    /**Helper function for status.
     * @return untrakcedfiles.*/
    private static String findUntrackedFile() {
        String result = "";
        result += "\n=== Untracked Files ===\n";
        TreeSet<String> untracked = new TreeSet<>();
        stage = StagingArea.fromFile();
        currentCommit = Commit.headfromFile();
        File[] filess = CWD.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".txt");
            }
        });
        for (File fil : filess) {
            if (!stage.getAdd().containsKey(fil.getName())) {
                if (!currentCommit.getTree().containsKey(fil.getName())) {
                    untracked.add(fil.getName());
                } else if (stage.getRemoval().contains(fil.getName())) {
                    String sha1 = Utils.sha1(fil.getName(),
                            Utils.sha1(Utils.readContentsAsString(fil)));
                    if (!currentCommit.getTree().get
                            (fil.getName()).equals(sha1)) {
                        untracked.add(fil.getName());
                    }
                }
            }
        }
        if (!untracked.isEmpty()) {
            for (String item : untracked) {
                result += item + "\n";
            }
        }

        return result;

    }

    /** Checkout method where ARGS contain [checkout] [branch].*/
    public static void checkout(String [] args) {
        if (args.length == 2) {
            Branch[] result = checkoutBranch(args[1]); initialize();
            Branch curBranch = result[0]; Branch thisBranch = result[1];
            TreeSet<String> untracked = new TreeSet<>();
            File[] filess = cWDFiles();
            ArrayList<String> fileNamesss = new ArrayList<>();
            Commit thisCommit = Commit.fromFile(thisBranch.getCurCommit());
            ArrayList<File> removalFile = new ArrayList<>();
            for (File fil : filess) {
                fileNamesss.add(fil.getName());
                String thissha = Utils.sha1(fil.getName(),
                        Utils.sha1(Utils.readContentsAsString(fil)));
                if (!currentCommit.getTree().containsKey(fil.getName())) {
                    untracked.add(fil.getName());
                } else if (!currentCommit.getTree().
                        get(fil.getName()).equals(thissha)) {
                    untracked.add(fil.getName());
                }
                if (currentCommit.getTree().containsKey(fil.getName())
                    && currentCommit.getTree().get(fil.getName()).
                        equals(thissha)
                    && !thisCommit.getTree().containsKey(fil.getName())) {
                    removalFile.add(fil);
                }
            }
            if (!untracked.isEmpty()) {
                for (String untrackedFile : untracked) {
                    if (thisCommit.getTree().containsKey(untrackedFile)) {
                        System.out.println("There is an untracked file in the "
                            + "way; delete it, or add and commit it first.");
                        exit(0);
                    }
                }
            }
            for (String fileName : thisCommit.getTree().keySet()) {
                String content = Blob.fromFile(thisCommit.getTree().
                        get(fileName)).getContent();
                try {
                    if (!fileNamesss.contains(fileName)) {
                        Utils.join(CWD, fileName).createNewFile();
                    }
                    Utils.writeContents(Utils.join(CWD, fileName), content);
                } catch (IOException ioe) {
                    System.out.println("ioe"); exit(0);
                }
            }
            for (File file : removalFile) {
                Utils.restrictedDelete(file);
            }
            stage.clear(); Utils.writeObject(STAGE, stage);
            thisBranch.setHead(true); thisBranch.saveAlreadyBranch();
            curBranch.setHead(false); curBranch.saveAlreadyBranch();
            currentCommit = thisCommit; Utils.writeObject(HEAD, currentCommit);
        } else {
            checkout2(args);
        }
    }
    /** helper for function for getting STAGE and currentcommit initialized.*/
    static void initialize() {
        currentCommit = Commit.headfromFile();
        stage = StagingArea.fromFile();
    }
    /** get all files ending with .txt in CWD.
     * @return files in CWD.*/
    static File[] cWDFiles() {
        File[] files = CWD.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return name.endsWith(".txt");
            }
        });
        return files;
    }

    /** helper function for checkout [branch].
     * @param thisBranchName the checkout branch Name
     * @return RESULT [currentBranch, thisBranch]*/
    static Branch[] checkoutBranch(String thisBranchName) {
        Branch[] result = new Branch[2];
        Branch thisBranch = null;
        boolean found = false;
        Branch currentBranch = null;
        String currentName = "";
        thisBranchName = thisBranchName.replace("/", "-");
        for (File branch : BRANCHES.listFiles()) {
            Branch savedBranch = Utils.readObject(branch, Branch.class);
            if (savedBranch.isHeadBranch()) {
                currentBranch = savedBranch;
                currentName = currentBranch.getName();
            }
            if (savedBranch.getName().equals(thisBranchName)) {
                found = true; thisBranch = savedBranch;
            }
        }
        if (!found) {
            System.out.println("No such branch exists."); exit(0);
        }
        if (currentName.equals(thisBranchName)) {
            System.out.println("No need to checkout the current branch.");
            exit(0);
        }
        result[0] = currentBranch;
        result[1] = thisBranch;
        return result;
    }

    /** checkout for args length 3 and 4.
     * ARGS contain [checkout -- fileName] or
     * [checkout commmitid -- fileName]. */
    static void checkout2(String [] args) {
        if (args.length == 3) {
            if (!args[1].equals("--")) {
                System.out.println("Incorrect operands."); exit(0);
            }
            String fileName = args[2]; helperCheckout(HEAD, fileName);
        } else if (args.length == 4) {
            if (!args[2].equals("--")) {
                System.out.println("Incorrect operands."); exit(0);
            }
            String commitid = args[1]; String fileName = args[3];
            if (commitid.length() < 6) {
                System.out.println("Not enough digits for commit id.");
                exit(0);
            }
            File thisCommit = null;
            if (COMMITS.listFiles() != null) {
                for (File file : COMMITS.listFiles()) {
                    if (file.getName().startsWith(commitid)) {
                        thisCommit = file;
                        break;
                    }
                }
            }

            if (thisCommit == null) {
                System.out.println("No commit with that id exists.");
                exit(0);
            }
            helperCheckout(thisCommit, fileName);
        } else {
            System.out.println("Incorrect operands."); exit(0);
        }
    }
   /** checkout helper function for arg length 3 and 4.
    * @param thisCommit represents the checkedout commit
    * @param fileName represents the file name. */
    static void helperCheckout(File thisCommit, String fileName) {
        currentCommit = Utils.readObject(thisCommit, Commit.class);
        if (!currentCommit.getTree().containsKey(fileName)) {
            System.out.println("File does not exist in that commit.");
            exit(0);
        }
        String blobid = currentCommit.getTree().get(fileName);
        File thisBlob = Utils.join(BLOBS, blobid);
        String content = Utils.readObject(thisBlob, Blob.class).getContent();
        File thisFile = Utils.join(CWD, fileName);

        try {
            if (!thisFile.exists()) {
                thisFile.createNewFile();
            }
            Utils.writeContents(thisFile, content);

        } catch (IOException ioe) {
            System.out.println("ioe error");
            exit(0);
        }
    }

    /** create a new branch. ARGS contain [branch] [name]. */
    public static void branch(String [] args) {
        checkArgsTwo(args);
        String newBranchName = args[1].replace("/", "-");
        for (File branch : BRANCHES.listFiles()) {
            if (branch.getName().equals(newBranchName)) {
                System.out.println("A branch with that name already exists.");
                exit(0);
            }
        }
        currentCommit = Commit.headfromFile();
        Branch newBranch = new Branch(newBranchName, currentCommit);
        try {
            newBranch.saveBranch();
        } catch (IOException ioe) {
            System.out.println("ioe");
            exit(0);
        }
    }

    /**Remove a branch, ARGS contain [rm] [branchName].*/
    public static void rmBranch(String [] args) {
        checkArgsTwo(args);
        File deleteBranch = null;
        if (BRANCHES.listFiles() != null) {
            for (File branch : BRANCHES.listFiles()) {
                if (branch.getName().equals(args[1].
                        replace("/", "-"))) {
                    deleteBranch = branch;
                    break;
                }
            }
        }
        if (deleteBranch == null) {
            System.out.println("A branch with that name does not exist.");
            exit(0);
        }
        Branch todo = Utils.readObject(deleteBranch, Branch.class);
        String todoCommit = todo.getCurCommit();
        if (todoCommit.equals(Commit.headfromFile().getSha1ID())) {
            System.out.println("Cannot remove the current branch.");
            exit(0);
        }
        deleteBranch.delete();
    }

    /**reset to a specific commit. ARGS contain [reset] [commitid].*/
    public static void reset(String [] args) {
        checkArgsTwo(args); String commitid = args[1];
        Commit thisCommit = null; initialize();
        for (File file : COMMITS.listFiles()) {
            if (file.getName().startsWith(commitid)) {
                thisCommit = Commit.fromFile(file.getName()); break;
            }
        }
        if (thisCommit == null) {
            System.out.println("No commit with that id exists."); exit(0);
        }
        TreeSet<String> untracked = new TreeSet<>(); File[] filess = cWDFiles();
        ArrayList<String> fileNamesss = new ArrayList<>();
        ArrayList<File> removalFile = new ArrayList<>();
        for (File fil : filess) {
            fileNamesss.add(fil.getName()); String sha = Utils.sha1(fil.
                    getName(), Utils.sha1(Utils.readContentsAsString(fil)));
            if (!currentCommit.getTree().containsKey(fil.getName())) {
                untracked.add(fil.getName());
            } else if (!currentCommit.getTree().
                    get(fil.getName()).equals(sha)) {
                untracked.add(fil.getName());
            }
            if (currentCommit.getTree().containsKey(fil.getName())
                    && currentCommit.getTree().get(fil.getName()).equals(sha)
                    && !thisCommit.getTree().containsKey(fil.getName())) {
                removalFile.add(fil);
            }
        }
        if (!untracked.isEmpty()) {
            for (String untrackedFile : untracked) {
                if (thisCommit.getTree().containsKey(untrackedFile)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    exit(0);
                }
            }
        }
        for (String fileName : thisCommit.getTree().keySet()) {
            File blob = Utils.join(BLOBS, thisCommit.getTree().get(fileName));
            String content = Utils.readObject(blob, Blob.class).getContent();
            try {
                if (!fileNamesss.contains(fileName)) {
                    Utils.join(CWD, fileName).createNewFile();
                }
                Utils.writeContents(Utils.join(CWD, fileName), content);
            } catch (IOException ioe) {
                System.out.println("ioe"); exit(0);
            }
        }
        deleteFileFromCWD(removalFile);
        stage.clear(); Utils.writeObject(STAGE, stage); Branch curBranch = null;
        for (File branch : BRANCHES.listFiles()) {
            if (Utils.readObject(branch, Branch.class).isHeadBranch()) {
                curBranch = Utils.readObject(branch, Branch.class); break;
            }
        }
        curBranch.setTotalCurCommit(thisCommit); curBranch.saveAlreadyBranch();
        currentCommit = thisCommit; Utils.writeObject(HEAD, currentCommit);
    }

    /** delete file from CWD.
     * @param removalFile represents the file going to be removed.*/
    public static void deleteFileFromCWD(ArrayList<File> removalFile) {
        for (File file : removalFile) {
            Utils.restrictedDelete(file);
        }
    }

    /** Mimic merge. ARGS contain [merge] [branchName].*/
    public static void merge(String [] args) {
        checkArgsTwo(args); initialize();
        checkStaged();
        Branch [] result = checkoutBranch2(args[1]);
        Branch curBranch = result[0]; Branch thisBranch = result[1];
        findUntrackedForMerge(thisBranch);
        Commit splitpoint = findSplit(curBranch, thisBranch);
        if (splitpoint.getSha1ID().equals(thisBranch.getCurCommit())) {
            System.out.println("Given branch is an ancestor "
                    + "of the current branch.");
            exit(0);
        }
        if (splitpoint.getSha1ID().equals(curBranch.getCurCommit())) {
            args[0] = "checkout";
            checkout(args);
            System.out.println("Current branch fast-forwarded.");
            exit(0);
        }
        stagingFiles(splitpoint, thisBranch);
        removingFiles(splitpoint, thisBranch);
        boolean conflict = conflictedFiles(splitpoint, thisBranch);

        String[] arg = new String[2];
        arg[0] = "Merged " + thisBranch.getName().replace("-", "/")
                + " into " + curBranch.getName() + ".";
        arg[1] = Commit.fromFile(thisBranch.getCurCommit()).getSha1ID();
        specialCommit(arg);
        if (conflict) {
            System.out.println("Encountered a merge conflict.");
        }
    }

    /**special commit.
     *ARGS contain [msg, secondparent].*/
    public static void specialCommit(String [] args) {
        date = new Date();
        SimpleDateFormat formatDate =
                new SimpleDateFormat("E MMM dd hh:mm:ss yyyy -0800");
        String commitDate = formatDate.format(date);
        currentCommit = Utils.readObject(HEAD, Commit.class);
        stage = StagingArea.fromFile();
        if (stage.getRemoval().isEmpty() && stage.getAdd().isEmpty()) {
            System.out.println("No changes added to the commit.");
            exit(0);
        }
        Commit newCommit = new Commit(currentCommit, args[0], commitDate);
        newCommit.setSecondParent(args[1]);
        newCommit.saveCommit();
        Utils.writeObject(HEAD, newCommit);
        for (File branch : BRANCHES.listFiles()) {
            Branch thisBranch = Utils.readObject(branch, Branch.class);
            if (thisBranch.isHeadBranch()) {
                thisBranch.setTotalCurCommit(newCommit);
                thisBranch.saveAlreadyBranch();
                break;
            }
        }
    }
    /** deal with conflicted files.
     * @param splitpoint represents the splitpoint commit
     * @param thisBranch represensts the merged branch.
     * @return whether there exist conflict files. */
    static boolean conflictedFiles(Commit splitpoint, Branch thisBranch) {
        ArrayList<String> specialFile = new ArrayList<>(); initialize();
        Commit thisCommit = Commit.fromFile(thisBranch.getCurCommit());
        for (String fileName : splitpoint.getTree().keySet()) {
            String blobid = splitpoint.getTree().get(fileName);
            if (currentCommit.getTree().get(fileName) != null
                && !currentCommit.getTree().get(fileName).equals(blobid)
                && thisCommit.getTree().get(fileName) != null
                && !thisCommit.getTree().get(fileName).equals(blobid)
                && !currentCommit.getTree().get(fileName).
                    equals(thisCommit.getTree().get(fileName))) {
                specialFile.add(fileName);
            } else if (currentCommit.getTree().get(fileName) != null
                    && !currentCommit.getTree().get(fileName).equals(blobid)
                    && thisCommit.getTree().get(fileName) == null) {
                specialFile.add(fileName);
            } else if (thisCommit.getTree().get(fileName) != null
                    && !thisCommit.getTree().get(fileName).equals(blobid)
                    && currentCommit.getTree().get(fileName) == null) {
                specialFile.add(fileName);
            }
        }
        for (String fileName : thisCommit.getTree().keySet()) {
            if (splitpoint.getTree().get(fileName) == null
                && currentCommit.getTree().get(fileName) != null
                && !currentCommit.getTree().get(fileName).
                    equals(thisCommit.getTree().get(fileName))) {
                specialFile.add(fileName);
            }
        }
        for (String fileName : specialFile) {
            File thisFile = Utils.join(CWD, fileName);
            String conflictmsg = "<<<<<<< HEAD\n";
            try {
                if (!thisFile.exists()) {
                    thisFile.createNewFile();
                }
                String thiscontent = "";
                if (thisCommit.getTree().containsKey(fileName)) {
                    thiscontent = Utils.readObject(Utils.join(BLOBS, thisCommit.
                            getTree().get(fileName)), Blob.class).getContent();
                }
                String curcontent = "";
                if (currentCommit.getTree().containsKey(fileName)) {
                    curcontent = Utils.readObject(Utils.join(BLOBS,
                            currentCommit.getTree().get(fileName)),
                            Blob.class).getContent();
                }
                conflictmsg += curcontent; conflictmsg += "=======\n";
                conflictmsg += thiscontent; conflictmsg += ">>>>>>>\n";
                Utils.writeContents(thisFile, conflictmsg);
                String sha1 = Utils.sha1(fileName, Utils.sha1(conflictmsg));
                stage.add(fileName, sha1);
            } catch (IOException ioe) {
                exit(0);
            }
        }
        stage.saveStage(); return specialFile.size() != 0;
    }

    /** deal with removing the necessary files.
     * @param splitpoint represents the splitpoint commit
     * @param thisBranch represensts the merged branch. */
    static void removingFiles(Commit splitpoint, Branch thisBranch) {
        ArrayList<String> specialFile = new ArrayList<>();
        Commit thisCommit = Commit.fromFile(thisBranch.getCurCommit());
        currentCommit = Commit.headfromFile();
        stage = StagingArea.fromFile();
        for (String fileName : splitpoint.getTree().keySet()) {
            String blobid = splitpoint.getTree().get(fileName);
            if (currentCommit.getTree().get(fileName) != null
                && currentCommit.getTree().get(fileName).equals(blobid)
                && thisCommit.getTree().get(fileName) == null) {
                specialFile.add(fileName);
            }
        }
        for (String fileName : specialFile) {
            String[] args = new String[2];
            args[0] = "rm"; args[1] = fileName;
            rm(args);
            stage.saveStage();
        }

    }

    /** deal with staging the necessary files.
     * @param splitpoint represents the splitpoint commit
     * @param thisBranch represensts the merged branch. */
    static void stagingFiles(Commit splitpoint, Branch thisBranch) {
        ArrayList<String> specialFile = new ArrayList<>();
        Commit thisCommit = Commit.fromFile(thisBranch.getCurCommit());
        currentCommit = Commit.headfromFile();
        stage = StagingArea.fromFile();
        for (String fileName : splitpoint.getTree().keySet()) {
            String blobid = splitpoint.getTree().get(fileName);
            if (thisCommit.getTree().containsKey(fileName)
                    && !thisCommit.getTree().get(fileName).equals(blobid)
                    && currentCommit.getTree().containsValue(blobid)) {
                specialFile.add(fileName);
            }
        }

        for (String fileName : thisCommit.getTree().keySet()) {
            if (splitpoint.getTree().get(fileName) == null
                && currentCommit.getTree().get(fileName) == null) {
                specialFile.add(fileName);
            }
        }
        for (String file : specialFile) {
            String [] args = new String[4];
            args[0] = "checkout"; args[1] = thisCommit.getSha1ID();
            args[2] = "--"; args[3] = file;
            checkout2(args);
        }

        for (String file : specialFile) {
            stage.add(file, thisCommit.getTree().get(file));
        }
        stage.saveStage();
    }


    /** @param curBranch is the current branch where the HEAD points to.
     *  @param thisBranch the mergedBranch.
     *  @return return the split point*/
    private static Commit findSplit(Branch curBranch, Branch thisBranch) {
        Stack curancestors = (Stack) curBranch.getAllCommits().clone();
        currentCommit = Commit.headfromFile();
        Stack<String> thisancestors =  thisBranch.getAllCommits();
        Commit splitPoint = null;
        while (!curancestors.empty()) {
            String curancestor = (String) curancestors.pop();
            if (thisancestors.contains(curancestor)) {
                String split = curancestor;
                splitPoint = Commit.fromFile(split);
                break;
            }
        }
        return splitPoint;
    }

    /** find untracked files.
     * @param thisBranch represents the merged branch. */
    public static void findUntrackedForMerge(Branch thisBranch) {
        TreeSet<String> untracked = new TreeSet<>();
        File[] filess = cWDFiles();
        File file = Utils.join(COMMITS, thisBranch.getCurCommit());
        Commit thisCommit = Utils.readObject(file, Commit.class);
        for (File fil : filess) {
            String sha = Utils.sha1(fil.getName(),
                    Utils.sha1(Utils.readContentsAsString(fil)));
            if (!currentCommit.getTree().containsKey(fil.getName())) {
                untracked.add(fil.getName());
            } else if (!currentCommit.getTree().
                    get(fil.getName()).equals(sha)) {
                untracked.add(fil.getName());
            }
        }
        if (!untracked.isEmpty()) {
            for (String untrackedFile : untracked) {
                if (thisCommit.getTree().containsKey(untrackedFile)) {
                    System.out.println("There is an untracked file in the way; "
                            + "delete it, or add and commit it first.");
                    exit(0);
                }
            }
        }

    }

    /** helper function for [merge] [branch].
     * @param thisBranchName is the merged branch name.
     * @return RESULT [currentBranch, thisBranch]*/
    static Branch[] checkoutBranch2(String thisBranchName) {
        thisBranchName = thisBranchName.replace("/", "-");
        Branch[] result = new Branch[2];
        Branch thisBranch = null;
        boolean found = false;
        Branch currentBranch = null;
        String currentName = "";
        for (File branch : BRANCHES.listFiles()) {
            Branch savedBranch = Utils.readObject(branch, Branch.class);
            if (savedBranch.isHeadBranch()) {
                currentBranch = savedBranch;
                currentName = currentBranch.getName();
            }
            if (savedBranch.getName().equals(thisBranchName)) {
                found = true; thisBranch = savedBranch;
            }
        }
        if (!found) {
            System.out.println("A branch with that name does not exist.");
            exit(0);
        }
        if (currentName.equals(thisBranchName)) {
            System.out.println("Cannot merge a branch with itself.");
            exit(0);
        }
        result[0] = currentBranch;
        result[1] = thisBranch;
        return result;
    }

    /** Check whether there are uncommited changes. */
    private static void checkStaged() {
        if (!stage.getAdd().isEmpty() || !stage.getRemoval().isEmpty()) {
            System.out.println("You have uncommitted changes.");
            exit(0);
        }
    }


    /**add remote, where ARGS contains [add-remote][remoteName][name of .
     * remote directory]/.gitlet]. */
    static void addRemote(String[] args) {
        if (args.length != 3) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
        for (File remote : REMOTES.listFiles()) {
            if (remote.getName().equals(args[1])) {
                System.out.println("A remote with that name already exists.");
                exit(0);
            }
        }
        Remote remote = new Remote(args[1], args[2]);
        remote.saveRemote();
    }
    /**remove remote, where ARGS contains [rm-remote][remoteName]. */
    static void rmRemote(String[] args) {
        checkArgsTwo(args);
        boolean found = false;
        for (File remote : REMOTES.listFiles()) {
            if (remote.getName().equals(args[1])) {
                found = true;
            }
        }
        if (!found) {
            System.out.println("A remote with that name does not exist.");
            exit(0);
        }
        for (File branch : BRANCHES.listFiles()) {
            if (branch.getName().contains("-")
                    && branch.getName().contains(args[1])) {
                branch.delete();
            }
        }

        File reRemote = Utils.join(REMOTES, args[1]);
        reRemote.delete();
    }
    /**push, where ARGS contains [push][remoteName][remote branch name]. */
    static void push(String[] args) {
        if (args.length != 3) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
        File remoteFile = Utils.join(REMOTES, args[1]);
        if (!remoteFile.exists()) {
            System.out.println("A remote with that name does not exist.");
            exit(0);
        }

        Remote remote = Utils.readObject(remoteFile, Remote.class);
        if (!remote.getRepo().exists()) {
            System.out.println("Remote directory not found.");
            exit(0);
        }
        currentCommit = Commit.headfromFile();
        Branch remoteBranch = remote.getThisBranch(args[2]);
        if (remoteBranch == null) {
            remote.createBranch(args[2]);
            remoteBranch = remote.getThisBranch(args[2]);
        }
        String remotecommitid = remoteBranch.getCurCommit();

        Queue<String> localAllCommits = new LinkedList<>();
        Commit temp = currentCommit;
        while (temp.getParent() != null) {
            localAllCommits.add(temp.getSha1ID());
            String nextCommitid = temp.getParent();
            temp = Utils.readObject(Utils.join(COMMITS, nextCommitid),
                    Commit.class);
        }
        localAllCommits.add(temp.getSha1ID());
        if (!localAllCommits.contains(remotecommitid)) {
            System.out.println("Please pull down "
                    + "remote changes before pushing.");
            exit(0);
        }

        Stack<Commit> appendedOnes = new Stack<>();
        while (!localAllCommits.isEmpty()) {
            File f = Utils.join(COMMITS, localAllCommits.remove());
            Commit latest = Utils.readObject(f, Commit.class);
            if (!latest.getSha1ID().equals(remotecommitid)) {
                appendedOnes.push(latest);
            } else {
                break;
            }
        }
        copyLocalBlobs(remote, appendedOnes);
        Commit parent = remote.getThisBranchHead(args[2]);
        while (!appendedOnes.isEmpty()) {
            Commit thisOne = appendedOnes.pop();
            Commit appended = new Commit(parent, thisOne.getmsg(),
                    thisOne.getDate());
            remote.pushCommit(remoteBranch, appended, thisOne);
            parent = appended;
        }
        remote.setHead(parent);
    }


    /**Helper method for get and filter out all the blobs in remote.
     * @param remote is the current remote,
     * @param filteredcommits is the filtered. */
    static void copyLocalBlobs(Remote remote,
                                       Stack<Commit> filteredcommits) {
        for (Commit commit : filteredcommits) {
            HashMap<String, String> thisTree = commit.getTree();
            for (String blobid : thisTree.values()) {
                if (remote.getBlob(blobid) == null) {
                    Blob local = Utils.readObject(Utils.
                            join(BLOBS, blobid), Blob.class);
                    Blob newOne = new Blob(local.getContent());
                    newOne.setSha1ID(local);
                    newOne.setContent(local.getContent());
                    remote.saveBlob(newOne);
                }

            }
        }
    }

    /**fetch, where ARGS contains [fetch] [remote name] [remote branch name]. */
    static void fetch(String[] args) {
        if (args.length != 3) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
        File remoteFile = Utils.join(REMOTES, args[1]);
        Remote remote = Utils.readObject(remoteFile, Remote.class);
        if (!remote.getRepo().exists()) {
            System.out.println("Remote directory not found.");
            exit(0);
        }
        Branch thisRemoteBranch = remote.getThisBranch(args[2]);
        if (thisRemoteBranch == null) {
            System.out.println("That remote does not have that branch.");
            exit(0);
        }
        Stack<Commit> filteredcommits = filterCommits(remote, thisRemoteBranch);
        ArrayList<Blob> filteredBlobs = filterBlobs(remote, filteredcommits);
        for (Blob filterBlob : filteredBlobs) {
            Blob newBlob = new Blob(filterBlob.getFileName());
            newBlob.setContent(filterBlob.getContent());
            newBlob.copyremoteBlob(filterBlob);
        }
        if (filteredcommits.isEmpty()) {
            exit(0);
        }
        Commit firstParent = null;
        Commit firstRemote = filteredcommits.pop();
        File parent1 = Utils.join(COMMITS, firstRemote.getParent());
        if (parent1.exists()) {
            firstParent = Utils.readObject(parent1, Commit.class);
        }
        Commit newCommit = new Commit(firstParent, firstRemote.getmsg(),
                firstRemote.getDate());
        newCommit.copyRemoteCommit(firstRemote);
        firstParent = newCommit;

        for (Commit remoteCommit : filteredcommits) {
            newCommit = new Commit(firstParent, remoteCommit.getmsg(),
                    remoteCommit.getDate());
            newCommit.copyRemoteCommit(remoteCommit);
            firstParent = newCommit;
        }
        File fetchedbranch = Utils.join(BRANCHES, args[1] + "-" + args[2]);
        Branch fetchedb = null;
        if (!fetchedbranch.exists()) {
            try {
                fetchedb = new Branch(args[1] + "-" + args[2], firstParent);
                fetchedb.saveBranch();
            } catch (IOException ioe) {
                exit(1);
            }
        } else {
            fetchedb = Utils.readObject(fetchedbranch, Branch.class);
            fetchedb.setTotalCurCommit(firstParent);
            fetchedb.saveAlreadyBranch();
        }
    }

    /**Helper method for get and filter out all the commits in remote.
     * @param thisBranch is the chosen branch
     * @param remote is the remote
     * @return the list of commits filtered. */
    static Stack<Commit> filterCommits(Remote remote, Branch thisBranch) {
        Stack<Commit> result = new Stack<>();
        Queue<Commit> remoteCommits = new LinkedList<>();
        Commit remotecurCommit = remote.getThisBranchHead(thisBranch.getName());
        while (remotecurCommit.getParent() != null) {
            remoteCommits.add(remotecurCommit);
            remotecurCommit = remote.getCommit(remotecurCommit.getParent());
        }
        remoteCommits.add(remotecurCommit);

        while (!remoteCommits.isEmpty()) {
            String thisremoteid = remoteCommits.remove().getSha1ID();
            if (!Utils.join(COMMITS, thisremoteid).exists()) {
                Commit added = remote.getCommit(thisremoteid);
                result.push(added);
            } else {
                break;
            }
        }
        return result;
    }

    /**Helper method for get and filter out all the blobs in remote.
     * @param remote is the current remote,
     * @param filteredcommits is the filtered
     * @return the list of filtered blobs. */
    static ArrayList<Blob> filterBlobs(Remote remote,
                                       Stack<Commit> filteredcommits) {
        ArrayList<Blob> result = new ArrayList<>();
        for (Commit commit : filteredcommits) {
            HashMap<String, String> thisTree = commit.getTree();
            for (String blobid : thisTree.values()) {
                File localfile = Utils.join(BLOBS, blobid);
                if (!localfile.exists()) {
                    result.add(remote.getBlob(blobid));
                }
            }
        }
        return result;
    }


    /**pull, where ARGS contains [pull] [remote name] [remote branch name]. */
    static void pull(String[] args) {
        if (args.length != 3) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
        args[0] = "fetch";
        fetch(args);
        String[] mm = new String[2];
        mm[0] = "merge";
        mm[1] = args[1] + "-" + args[2];
        merge(mm);
    }


    /**Helper function for checking if ARGS have length 1.*/
    public static void checkArgsOne(String [] args) {
        if (args.length != 1) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
    }
    /**Helper function for checking if ARGS have length 2.*/
    public static void checkArgsTwo(String [] args) {
        if (args.length != 2) {
            System.out.println("Incorrect operands.");
            exit(0);
        }
    }
    /**Represent the date. */
    private static Date date;
    /**the current commit.*/
    private static Commit currentCommit;
    /**the staging area. */
    private static StagingArea stage;

}
