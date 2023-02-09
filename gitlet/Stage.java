package gitlet;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.TreeMap;

public class Stage implements Serializable {
    /** TreeMap to store the added files with the fileName as the key and
     * the serialized blob as the value. */
    private TreeMap<String, String> _addTree;

    /** ArrayList to store the file names of
     * all the files need to be removed. */
    private ArrayList<String> _removeList;

    /** Stage constructor that initialize _addTree and _removeList. */
    public Stage() {
        _addTree = new TreeMap<>();
        _removeList = new ArrayList<>();
    }

    /** Adds the files to the blobMap
     * Creates a file with blobSHAID as the name and blob as the content.
     * @param fileName of the staged file
     * */
    public void toAddMap(String fileName) {
        Blob blob = new Blob(Utils.join(Repo.CWD, fileName));
        _addTree.put(fileName, blob.getShaID());
        Utils.writeObject(Utils.join(Repo.BLOB_FOLDER, blob.getShaID()), blob);
    }

    /** Adds the file that needs to be removed to the list.
     * @param fileName of the removed file
     * */
    public void toRemoveList(String fileName) {
        _removeList.add(fileName);
    }

    /** Clears the _addTree and the _removeList after each Commit. */
    public void clear() {
        _addTree = new TreeMap<>();
        _removeList = new ArrayList<>();
    }

    /** Gets the _addTree from the stage.
     * @return _addTree
     * */
    public TreeMap<String, String> getAddTree() {
        return _addTree;
    }

    /** Gets the _removeList from the stage.
     * @return _removeList
     * */
    public ArrayList<String> getRemoveList() {
        return _removeList;
    }
}
