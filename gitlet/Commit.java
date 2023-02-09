package gitlet;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Set;
import java.util.TreeMap;

public class Commit implements Serializable {

    /** Commit message of the current Commit. */
    private String _message;

    /** Timestamp of the current Commit. */
    private String _timestamp;

    /** Older parent Commit of the current Commit. */
    private Commit _mergeParent;

    /** Newer parent Commit of the current Commit. */
    private Commit _parent;

    /** Staged blobTree of the current Commit. */
    private TreeMap<String, String> _blobMap;

    /** Commit ID of the current Commit. */
    private String _commitID;

    /** Commit class constructor that takes in the Commit message,
     * staged blobTree, and the parent Commit.
     * Generates the SHA1 Commit ID of the current Commit.
     * Checks the parent stage blobTree to create a pointer in the
     * current blobTree for any old blobs.
     * @param msg commit message
     * @param blobMap stage treemap
     * @param mergeParent merge parent commit
     * @param parent parent commit
     * @param currRepo repo
     * */
    public Commit(String msg, TreeMap<String, String> blobMap,
                  Commit mergeParent, Commit parent, Repo currRepo) {
        _message = msg;
        _mergeParent = mergeParent;
        _parent = parent;
        _blobMap = blobMap;
        ArrayList<String> removeList = currRepo.getStage().getRemoveList();

        if (parent == null) {
            this._timestamp = "Thu Jan 01 00:00:00 1970 -0800";
        } else {
            this._timestamp = LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("E LLL dd HH:mm:ss u"
                            + " -0800"));

            Set<String> pBlobKeys = parent.getBlobMap().keySet();
            Set<String> currBlobKeys = this.getBlobMap().keySet();
            for (String bKey : pBlobKeys) {
                if (!currBlobKeys.contains(bKey)
                        && !removeList.contains(bKey)) {
                    _blobMap.put(bKey, parent.getBlobMap().get(bKey));
                }
            }
        }
        _commitID = Utils.sha1(Utils.serialize(this));
    }

    /** Gets the commit message of the current Commit.
     * @return Commit message of the current commit.
     * */
    public String getMessage() {
        return this._message;
    }

    /** Gets the timestamp of the current Commit.
     * @return Timestamp of the current commit.
     * */
    public String getTimestamp() {
        return this._timestamp;
    }

    /** Gets the newer parent Commit of the current Commit.
     * @return Papa of the current commit.
     * */
    public Commit getParent() {
        return this._parent;
    }

    /** Gets the oder parent Commit of the current Commit.
     * @return Grandpa of the current commit.
     * */
    public Commit getMergeParent() {
        return this._mergeParent;
    }

    /** Gets the SHA1 ID of the current Commit.
     * @return Commit ID of the current commit.
     * */
    public String getCommitID() {
        return this._commitID;
    }

    /** Gets the staged files for the current Commit.
     * @return Stage treemap of the commit.
     * */
    public TreeMap<String, String> getBlobMap() {
        return this._blobMap;
    }

    /** Adds a map to the current commit blobMap.
     * @param fileName file name
     * @param blob the blob file
     * */
    public void addBlob(String fileName, Blob blob) {
        this._blobMap.put(fileName, blob.getShaID());
        Utils.writeObject(Utils.join(Repo.BLOB_FOLDER, blob.getShaID()), blob);
    }

    /** Removes a map from the current commit blobMap.
     * @param fileName File name
     * */
    public void removeBlob(String fileName) {
        if (_blobMap.containsKey(fileName)) {
            this._blobMap.remove(fileName);
        }
    }

    /** Returns all the blobMap keys of the commit.
     * @return A set of all keys
     * */
    public Set<String> getBlobKeys() {
        return this._blobMap.keySet();
    }

}
