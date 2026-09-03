package gitlet;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import static gitlet.Main.BLOBS;
import static gitlet.Main.CWD;

/** Class representing a Blob.
 * @author Anji */
public class Blob implements Serializable {
    /**Constructor for Blob class.
     * @param fileName of the blob. */
    Blob(String fileName) {
        _fileName = fileName;
        file = Utils.join(CWD, fileName);
        if (file.exists()) {
            content = Utils.readContentsAsString(file);
        }

    }

    /**
     * Reads in and deserializes a blob from a file with name NAME in BLOBS.
     *
     * @param sha1 sha1 id of a blob
     * @return content com read from file
     */
    public static Blob fromFile(String sha1) {
        File blob = Utils.join(BLOBS, sha1);
        if (!blob.exists()) {
            return null;
        }
        Blob thisBlob = Utils.readObject(blob, Blob.class);
        return thisBlob;
    }
    /**@return content of the blob. */
    public String getContent() {
        return content;
    }

    /**set content.
     * @param thiscontent is the content. */
    public void setContent(String thiscontent) {
        content = thiscontent;
    }

    /**save the blob. */
    void saveBlob() {
        try {
            _sha1ID = Utils.sha1(_fileName, Utils.sha1(content));
            File thisBlob = Utils.join(BLOBS, _sha1ID);
            thisBlob.createNewFile();
            Utils.writeObject(thisBlob, this);
        } catch (IOException ioe) {
            Utils.error("ioe in blob" + _sha1ID);
        }

    }

    /**@param blob is the copied blob. */
    void copyremoteBlob(Blob blob) {
        try {
            _sha1ID = blob.getsha1();
            File thisBlob = Utils.join(BLOBS, _sha1ID);
            thisBlob.createNewFile();
            Utils.writeObject(thisBlob, this);
        } catch (IOException ioe) {
            Utils.error("ioe in blob" + _sha1ID);
        }
    }

    /**Set the sha1id same as b.
     * @param b is blob. */
    void setSha1ID(Blob b) {
        _sha1ID = b.getsha1();
    }

    /**Set the name same as b.
     * @param b is blob. */
    void setName(Blob b) {
        _fileName = b.getFileName();
    }

    /**@return fileName. */
    public String getFileName() {
        return _fileName;
    }

    /** @return _sha1ID of this blob. */
    String getsha1() {
        return _sha1ID;
    }

    /**fileName + content. */
    private String _sha1ID;

    /** the file in CWD. */
    private File file;

    /**the content in the file. */
    private String content;
    /**file name. */
    private String _fileName;
}
