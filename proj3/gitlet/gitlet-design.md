# Gitlet Design Document

**Name**: Anji Dong

# Gitlet

# Classes and data structures
## Blob

This class represents content of a file (txt).
###Fields
  1. String id: the unique sha1 id for the file
  2. File content
  3. String name: name of the file as reference


## StagingArea

This class represents the staging area for BRANCH.
###Fields
   1. LinkedList added: add all the blobs that are being tracked after commit.
   2. Branch branch: the branch that the staging area commits to.
   3. LinkedList removed: add all the file names that are being untracked.
   4. List file: list of files’ names being added


## Commit

This class represents each commit in the repository.
###Fields

1. Commit parent: the first parent commit
2. String message: the commit message
3. String time: the date and time when committing
4. HashSet copied: the sha1 code of the files of the parent commit


## Branch

This class
###Fields


## Repository

This class stores all the commit history.
###Fields

1. Hashmap: it maps the sha1 id of the commit to the Commit


## Main

This is the main class that executes the command.


# Algorithms
## Blob

1. getID(): get the sha1 id
2. saveContent(): update the content in staging area to the blob in repository
3. getName(): get the name of the file


## StagingArea

1. add(Blob blob): add the blob to staging area using linked list
2. toBeRemove(String fileName): mark the file to be untracked
3. clear(): clear added and removed after committing.
4. exist(String name, List file): see if name exists file’s name in staging area.
5. remove(String fileName): remove the file from added
6. fromFile(): return the StagingArea object that represents the current state of the
staging area.


## Commit
1. getsha1id(): return its sha1id.
2. setSecondParent(): set the second parent of the commit
3. getParent(): return the first parent.
4. saveCommit(): save the commit to COMMITS folder.
5. fromFile(commitid): return the Commit object based on commitid.

## Branch
1. isHead(): return whether this branch is the head branch.
2. setHead(): set the current branch to be the head branch.
3. getcurCommit(): get the commit that the branch points to.
4. getcommitHistory(): return the commit history starting the commit the branch points to
5. fromFile(): return the content of the branch.
6. getName(): return its name.


## Main
1. add(args): create a blob and add it to the staging area if it’s different from current commit. If it’s the same from current commit, and exists a previous version in staging area, remove the blob in staging area.
2. rm(args)
3. log(args)
4. globalLog(args)
5. find(args)
6. status(args)
7. checkout(args)
8. branch(args)
9. merge(args)
10. globallog(args)

# Persistence
1. make sure that init only runs for once. (if there exists .gitlet, then init cannot be executed)
2. save all the files that are being removed to staging area. Add, remove always go to staging area, instead of branch or repository class.
3. In order to read file, always check which directory that we are in, make sure that we are in the right branch by calling currentBranch()
4. Use restricteddelete when deleting files in CWD.
5. to ensure that NullPointerException does not exist, always check that the hashmap
calling get(fileName) != null. Afterwards, get its blobid to compare with other file sha1id.

