# Gitlet Design Document
author: Abdumijit A. Dolkun

## 1. Classes and Data Structures
### Main.java
The class is to take in the input as an argument and filter its validity, and proceed if it's valid.

### Repo.java
The class is to define all the commands that the gitlet can complete.


### Blobs.java
The class is responsible for keeping track of the contents of a file.

#### Fields

1. static final String FILE_NAME: The name of the file that the blob stores the content for.
2. static String _content: The contents of the file stored.


### Commit.java
The class is the combinations of log timestamp, log messages, mapping of file names to blob references, parent 
reference, and a second parent reference for merge.

#### Fields

1. static File COMMIT_FILE: A pointer that point to the committed files.
2. static String _message: The contents of the files stored.
3. static String _timestamps: Committed timestamp includes the exact time, time zone, day of the year.
4. static String _log: The name of the log for the current commit.
5. static File HEAD: A pointer that points to the most recent commit.
6. static File MASTER: A pointer that points to the current committing repository.


### Stage.java
The class is to keep track of the files that was changed in the repository since the last commit.

#### Fields
1. static File ADD: A pointer that points to the files that added contents.
2. static File RM: A pointer that points to the files that removed contents.


### Serialize.java
The class is use to create serialize methods for conversion between byte files and string files.

#### Fields
1. static File FILE_NAME: A pointer to the file that will be passed in for conversion.



## 2. Algorithms
### Main.java
* *Main()*: takes in the input as an argument and works as followed:

This is where we take in the input as an argument and determine if it's valid by:
1. if the input size is 0, we print *Please enter a command.*
2. if the command is not listed below, print *Command not valid.*
3. if the operand is not the right length, print *Operand not valid.*
4. if the command is not taken place in the gitlet repository, print *Not in the Gitlet repository.*
If the argument format is correct and includes any of the below commands with the correct operand, then the 
corresponding command executes.
* add 
* commit
* branch
* reset
* merge
* checkout
* status
* find
* global-log
* rm

### Repo.java
This is where we define and implement the use of all the possible commands that are listed in Main above.
* *Repo(String)*: The class constructor.


### Blob.java
This is where we save the content of the file to determine its version in the repository as a Hashmap.
Each bolb will have a pointer pointing to it as the content of each file in the commit section. 
When the contents of the file is not changed, no new bolb will be generated as a new Hash.
* *Blob(HashMap<String, String>)*: The class constructor.

### Commit.java
The is where the combinations of log timestamp, log messages, mapping of file names to blob references, parent
reference, and a second parent reference for merge takes place.  
Each time a unique Hashmap will be generated when we successfully commit files, the committed branch pointer and the 
HEAD pointer will be updated to the most recent commit along with the log information.
* *Commit(String, ... String, HashMap<String, String)*: The Commit class constructor that initialize the variables.

### Stage.java
This is where to keep track of the files that was changed in the repository since the last commit.
* Check to see if it's necessary to add the file to this stage
* If possible, hold the changes here until further action of committing.


### Serialize.java
This is where we store the files to our local repository, and do the conversion between String and Byte file contents.
* *StoreFile(Object)*: A method used to save a file to the committed repository.
* *toString(Object)*: A method used to serialize a byte file into a byte file.
* *toByte(String)*: A method used to reconvert a byte file into a string file.



## 3. Persistence
We would want to be able to encrypt another input file without
providing a configuration file. That is, the settings we provided
in the first run should persist across multiple executions of the
program.

In order to persist the settings of gitlet, we will need to save
the state of the commits after each call to gitlet.Main. To do
this,

1. Write the Commit HashMaps to disk. We can serialize them into
   bytes that we can eventually write to a specially named file on
   disk. This can be done with writeObject method from the Utils
   class. Which I will add later.

2. Write all the Blob objects to disk. We can serialize the Blob
   objects and write them to files on disk (for example, “Blob1”
   file, “Blob2” file, etc.). This can be done with the writeObject
   method from the Utils class. We will make sure that our Rotor class
   implements the Serializable interface.

In order to retrieve our state, before executing any code, we need to
search for the saved files in the working directory (folder in which
our program exists) and load the objects that we saved in them. Since
we set on a file naming convention (“commit1”, etc.) our program
always knows which files it should look for. We can use the readObject
method from the Utils class to read the data of files as and
deserialize the objects we previously wrote to these files.

## 4. Design Diagram

![GitLet Design Diagram](gitlet-design.png)
