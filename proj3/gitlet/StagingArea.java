package gitlet;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;

import static gitlet.Main.STAGE;

/**Represent the staging area.
 * @author Anji*/
public class StagingArea implements Serializable {
    /**Constructor for StagingArea class. */
    StagingArea() {
        addition = new HashMap<>();
        removal = new HashSet<>();
    }
    /**staged for addition.
     * @param fileName is the fileName being added.
     * @param b is the actual blob being created that contains the content.*/
    void add(String fileName, Blob b) {
        addition.put(fileName, b.getsha1());
    }

    /**staged for addition.
     * @param fileName is the fileName being added.
     * @param blobid is the blob id being created that contains the content.*/
    void add(String fileName, String blobid) {
        addition.put(fileName, blobid);
    }

    /**staged for removal(untrack) for next commit.
     * @param fileName is the file being removed. */
    void untrack(String fileName) {
        removal.add(fileName);
    }

    /** clear the whole staging area. */
    void clear() {
        addition.clear();
        removal.clear();
    }

    /**Save the stage area.*/
    void saveStage() {
        Utils.writeObject(STAGE, this);

    }
    /**@return the stagingArea object from STAGE folder. */
    static StagingArea fromFile() {
        StagingArea stage = Utils.readObject(STAGE, StagingArea.class);
        return stage;
    }

    /**reprensent the files being added.
     * @return added <fileName><blobid>*/
    HashMap<String, String> getAdd() {
        return addition;
    }
    /**reprensent the files being removed.
     * @return removed <fileName><blobid>*/
    HashSet<String> getRemoval() {
        return removal;
    }

    /**reprensent the files being removed.*/
    private HashSet<String> removal;
    /**reprensent the files being added.*/
    private HashMap<String, String> addition;





}
