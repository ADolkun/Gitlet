package gitlet;

import java.io.File;
import java.io.Serializable;

public class Blob implements Serializable {
    /** SHA1 of the current blob. */
    private String shaID;
    /** Contents of the current blob. */
    private String blobContents;

    /** Blob class constructor that generates the SHA1 and the contents
     * of a file.
     * @param file blob file.
     * */
    public Blob(File file) {
        shaID = Utils.sha1(Utils.readContents(file));
        blobContents = Utils.readContentsAsString(file);
    }

    /** Gets the SHA1 of the blob file.
     * @return the serialized SHA1 ID of the file content.
     * */
    public String getShaID() {
        return this.shaID;
    }

    /** Gets the content of the blob file.
     * @return the content of the blob file.
     * */
    public String getBlobContents() {
        return this.blobContents;
    }
}
