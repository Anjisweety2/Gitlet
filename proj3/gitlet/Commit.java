package gitlet;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;

import static gitlet.Main.*;

/**Class representing a commit.
 * @author Anji*/
public class Commit implements Serializable {
    /**Constructor for Commit class.
     * @param firstParent is the first parent of this commit.
     * @param msg is commit msg.
     * @param date is the commit date. */
    Commit(Commit firstParent, String msg, String date) {
        _msg = msg;
        _date = date;
        _parent = firstParent;
        if (_parent != null) {
            parent = _parent.getSha1ID();
        } else {
            parent = null;
        }
        sha1ID = Utils.sha1(_msg + _date + parent);
        cloneTree();
    }

    /**@return the sha1ID of the current commit. */
    String getSha1ID() {
        return sha1ID;
    }

    /**@return the date */
    String getDate() {
        return _date;
    }

    /**@return commit message. */
    String getmsg() {
        return _msg;
    }

    /**@param id is the commitid.
     * @return the first seven digit of the commit sha1id. */
    String sevenDigit(String id) {
        return id.substring(0, 7);
    }

    /**@return parent of the commit. */
    String getParent() {
        return parent;
    }

    /**@return the second parent of the commit. */
    String getSecondParent() {
        return _secondParent;
    }

    /**set the second parent.
     * @param secondParent is the second parent of the commit after merging. */
    void setSecondParent(String secondParent) {
        _secondParent = secondParent;
    }

    /**@return the tree representing the tracked files of this commit. */
    HashMap<String, String> getTree() {
        return tree;
    }

    /**update the tree of this commit by cloning its parent's commit. */
    void cloneTree() {
        if (_parent != null && _parent.getTree() != null) {
            for (String file : _parent.getTree().keySet()) {
                tree.put(file, _parent.getTree().get(file));
            }
        }
    }


    /**
     * Reads in and deserializes a commit from a file with name NAME in COMMITS.
     * @param commitID Name of commit to load
     * @return Commit com read from file
     */
    public static Commit fromFile(String commitID) {
        File thisCommit = Utils.join(COMMITS, commitID);
        if (!thisCommit.exists()) {
            return null;
        }
        Commit com = Utils.readObject(thisCommit, Commit.class);
        return com;
    }
    /**@return the head file. */
    public static Commit headfromFile() {
        return Utils.readObject(HEAD, Commit.class);
    }

    /**Saves a commit to a file for future use.*/
    public void saveCommit() {
        try {
            StagingArea stage = Utils.readObject(STAGE, StagingArea.class);
            for (String fileName : stage.getRemoval()) {
                tree.remove(fileName);
            }
            for (String fileName : stage.getAdd().keySet()) {
                tree.put(fileName, stage.getAdd().get(fileName));
            }

            if (!tree.isEmpty()) {
                Object[] fileSha1 = tree.values().toArray();
                sha1ID = Utils.sha1(Utils.sha1(fileSha1), sha1ID);
            }

            File thisCommit = Utils.join(COMMITS, sha1ID);
            thisCommit.createNewFile();
            Utils.writeObject(thisCommit, this);
            stage.clear();
            Utils.writeObject(STAGE, stage);

        } catch (IOException ioe) {
            throw new Error(ioe);
        }

    }
    /**Set sha1ID.
     * @param id is sha1id. */
    public void setSha1ID(String id) {
        sha1ID = id;
    }

    /**Set sha1ID.
     * @param commit is copied commit. */
    public void setTree(Commit commit) {
        tree = commit.getTree();
    }

    /**@param commit is the remote commit being copied. */
    public void copyRemoteCommit(Commit commit) {
        try {
            sha1ID = commit.getSha1ID();
            File thisCommit = Utils.join(COMMITS, sha1ID);
            thisCommit.createNewFile();
            tree = commit.getTree();
            Utils.writeObject(thisCommit, this);
        } catch (IOException ioe) {
            throw new Error(ioe);
        }

    }

    /**parent of the commit, not serialized, for convenience. */
    private transient Commit _parent;
    /**parent of the commit.*/
    private String parent;
    /**second parent of the commit.*/
    private String _secondParent;
    /**sha1ID of the commit. */
    private String sha1ID;
    /**date of the commit. */
    private String _date;
    /**commit message. */
    private String _msg;
    /** fileName - blob id. */
    private HashMap<String, String> tree = new HashMap<>();
}
