package gitlet;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Stack;

import static gitlet.Main.BRANCHES;

/** Representing a branch.
 * @author Anji */
public class Branch implements Serializable {
    /**Constructor of Branch class.
     * @param name is the branch name,
     * @param curCommit is the commit it points to. */
    Branch(String name, Commit curCommit) {
        _name = name;
        _curCommit = curCommit.getSha1ID();
        setTotalCurCommit(curCommit);
    }
    /** @return boolean value of head Branch. */
    boolean isHeadBranch() {
        return isHead;
    }

    /** Set the branch to be head branch.
     * @param isH boolean value for whether it's head. */
    void setHead(boolean isH) {
        isHead = isH;
    }

        /** @return the _NAME of the branch. */
    String getName() {
        return _name;
    }

    /** @return _curCommit the commit that this branch points to. */
    String getCurCommit() {
        return _curCommit;
    }

    /** set _curCommit the commit that this branch points to.
     * @param commit is the current commit. */
    void setCurCommit(Commit commit) {
        _curCommit = commit.getSha1ID();
    }

    /** save a new branch. */
    void saveBranch() throws IOException {
        File newBranch = Utils.join(BRANCHES, _name);
        newBranch.createNewFile();
        Utils.writeObject(newBranch, this);
    }

    /** save the already existed branch. */
    void saveAlreadyBranch() {
        File newBranch = Utils.join(BRANCHES, _name);
        assert newBranch.exists();
        Utils.writeObject(newBranch, this);
    }

    /** update the commit history of this branch.
     * @param commit reprensents the current commit. */
    void setTotalCurCommit(Commit commit) {
        assert commit != null;
        setCurCommit(commit);
        _allCommits.clear();
        Stack<String> ancestors = depthFirstAdd(commit);
        while (!ancestors.empty()) {
            _allCommits.push(ancestors.pop());
        }
    }

    /**helper method for updating commit history.
     * @param commit represents the current commit.
     * @return return the commits history. */
    Stack<String> depthFirstAdd(Commit commit) {
        Stack<String> ancestors = new Stack<>();
        Queue<String> temp = new LinkedList<>();
        temp.add(commit.getSha1ID());
        while (true) {
            int size = temp.size();
            if (size == 0) {
                break;
            }
            while (size > 0) {
                String x = temp.remove();
                ancestors.push(x);
                Commit xx = Commit.fromFile(x);
                if (xx.getParent() != null) {
                    temp.add(xx.getParent());
                }
                if (xx.getSecondParent() != null) {
                    temp.add(xx.getSecondParent());
                }
                size -= 1;
            }
        }
        return ancestors;
    }

    /** @return _allCommits: commit history. */
    public Stack<String> getAllCommits() {
        return _allCommits;
    }

    /** current latest commit id for this branch. */
    private String _curCommit;
    /** name of the branch. */
    private String _name;
    /**represent the commit history that the current branch points to.*/
    private Stack<String> _allCommits = new Stack<>();

    /** whether the HEAD pointer points to this branch or not. */
    private boolean isHead;

}
