package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;

import static gitlet.Main.REMOTES;
import static java.lang.System.exit;

/**Represent a remote object.
 * @author Anji*/
public class Remote implements Serializable {
    /** GitLet repo directory for remote. */
    private File repo;
    /** GitLet blobs directory for remote. */
    private File blobs;
    /** GitLet branches directory. */
    private File branches;
    /** GitLet current commit information. */
    private File head;
    /** GitLet staging area file. */
    private File stage;
    /** GitLet commit directory. */
    private File commits;

    /**Constructor for this remote. The remote should have.
     * everything the local has.
     * @param name is the name of the remote
     * @param dir is the directory name. */
    Remote(String name, String dir) {
        _name = name;
        directory = dir;
        repo = new File(directory);
        blobs = Utils.join(repo, "Blobs");
        branches = Utils.join(repo, "Branches");
        stage = Utils.join(repo, "StagingArea");
        commits = Utils.join(repo, "Commits");
        head = Utils.join(repo, "HEAD");
    }

    /**Save this remote. */
    public void saveRemote() {
        try {
            File thisremote = Utils.join(REMOTES, _name);
            thisremote.createNewFile();
            Utils.writeObject(thisremote, this);
        } catch (IOException ioe) {
            exit(0);
        }
    }

    /**@return the path to repo in this remote. */
    public File getRepo() {
        return repo;
    }
    /**@return head of the remote. */
    public Commit getHead() {
        Commit result = Utils.readObject(head, Commit.class);
        return result;
    }
    /** set the head of the remote.
     * @param commit is the head. */
    public void setHead(Commit commit) {
        Utils.writeObject(head, commit);
    }

    /**@return head branch name. */
    public Branch getHeadBranch() {
        for (File b : branches.listFiles()) {
            Branch bb = Utils.readObject(b, Branch.class);
            if (bb.isHeadBranch()) {
                return bb;
            }
        }
        return null;
    }
    /**@param name is the branch name.
     * @return Branch thisBranch. */
    public Branch getThisBranch(String name) {
        File result = Utils.join(branches, name);
        if (result.exists()) {
            return Utils.readObject(result, Branch.class);
        }
        return null;
    }
    /**@param name is the branch name.
     * @return head commit of the branch. */
    public Commit getThisBranchHead(String name) {
        Branch thisBranch = getThisBranch(name);
        File thiscommit = Utils.join(commits, thisBranch.getCurCommit());
        Commit hehe = Utils.readObject(thiscommit, Commit.class);
        return hehe;
    }

    /**@return head commit of the remote. */
    public Commit getHeadCommit() {
        return Utils.readObject(head, Commit.class);
    }

    /**@param fileName is the current file name.
     * @return a specific blob object in the current commit. */
    public String getBlobID(String fileName) {
        Commit thisCommit = getHeadCommit();
        if (thisCommit.getTree().get(fileName) == null) {
            return null;
        } else {
            return thisCommit.getTree().get(fileName);
        }
    }

    /**@param blobid is the sha1id.
     * @return a specific blob object. */
    public Blob getBlob(String blobid) {
        File thisblob = Utils.join(blobs, blobid);
        if (!thisblob.exists()) {
            return null;
        }
        return Utils.readObject(thisblob, Blob.class);
    }

    /**Save blob.
     * @param blob is the saved one. */
    public void saveBlob(Blob blob) {
        try {
            File thisBlob = Utils.join(blobs, blob.getsha1());
            thisBlob.createNewFile();
            Utils.writeObject(thisBlob, blob);
        } catch (IOException ioe) {
            exit(0);
        }
    }




    /**@return name of the remote. */
    public String getName() {
        return _name;
    }
    /**@return the staging area of the remote. */
    public StagingArea getStage() {
        StagingArea staged = Utils.readObject(stage, StagingArea.class);
        return staged;
    }
    /**create a new branch.
     * @param name is the branch name. */
    public void createBranch(String name) {
        try {
            Branch branch = new Branch(name, getHeadCommit());
            File newBranch = Utils.join(branches, name);
            newBranch.createNewFile();
            Utils.writeObject(newBranch, branch);
        } catch (IOException ioe) {
            exit(0);
        }

    }

    /**@param commitid is the sha1id.
     * @return the commit. */
    public Commit getCommit(String commitid) {
        File thisCommit = Utils.join(commits, commitid);
        return Utils.readObject(thisCommit, Commit.class);
    }
    /**Saves a commit and update the branch to a file for future use.
     * @param thisBranch is the branch
     * @param append is the commit being pushed.
     * @param local is the local commit being copied. */
    public void pushCommit(Branch thisBranch, Commit append, Commit local) {
        try {
            append.setSha1ID(local.getSha1ID());
            File newCommit = Utils.join(commits, append.getSha1ID());
            newCommit.createNewFile();
            append.setTree(local);

            Utils.writeObject(newCommit, append);

            thisBranch.setTotalCurCommit(append);
            File updateBranch = Utils.join(branches, thisBranch.getName());
            Utils.writeObject(updateBranch, thisBranch);
        } catch (IOException ioe) {
            exit(0);
        }

    }

    /**Get all the blobs.
     * @return the lst of blobs. */
    public ArrayList<Blob> getAllBlobs() {
        ArrayList<Blob> result = new ArrayList<>();
        for (File file : blobs.listFiles()) {
            result.add(Utils.readObject(file, Blob.class));

        }
        return result;
    }


    /**Name of this remote. */
    private String _name;
    /**The directory the remote is in. */
    private String directory;
}
