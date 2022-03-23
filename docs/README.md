# Proof-of-Personhood

[Protocol specification](protocol.md)

## Git(Hub)

### Import the project

`cd` where you want to have your project located and then clone the project using the following commands

```bash
git clone git@github.com:dedis/popstellar.git [folderProjectName]
```

We then want to navigate to the `be2-scala` subfolder

```bash
cd <folderProjectName>/be2-scala
```

You can then open the project using your favorite IDE (e.g. `idea .` for IntelliJ and `code .` for VSCode)

---

### Create a new branch

By default, the new branch you create will be a copy of `master`

```bash
git checkout -b <branchName>
```

If you rather want to make a copy of branch `x` (e.g. a branch that is currently being reviewed, i.e. not merged), use this instead

```bash
git checkout <x>
git checkout -b <branchName>
```

---

### Push changes on the remote server

When pushing changes, you first want to **commit** the changes you've applied using

```bash
git add <fileName>					# add file <fileName> to the commit
git commit -m"<message>"    # add a title to the commit (switch <message> for the actual message)

## Note: You may also add files separately using
git add <fileName>

## Note: You may also add all modified (!= new) files to the commit and set a title using
git commit -am"<message>"

## Note: You may also set a title and description for a specific commit using your favorite IDE (obviously doom emacs ^^). Then save and quit
git add <fileName>
git commit
```

<div align="center">
  <img alt="Example of git commit command" src="images/example-git-commit.png" width="700" />
</div>

:information_source: Do not create a commit with 2.000 lines of codes! Short commit with meaningful titles/description are way better and way easier to review

:information_source: There are faster ways to add multiple files in the commit such as `git commit <-u|.>`. You can check the corresponding man page for more information 

You then finally want to pull/push the changes

```bash
git pull			# safety check to see if no one applies changes to your branch in your absence
git push			# push the commited changes
```

Note that if you just created the branch, you'll need to tell git once where to push the changes using the following command instead of `git push`

```bash
git push -u origin <branchName>
```

:warning: You cannot directly push changes on `master`! For that, you need to go through the process of creating a pull request (see below)

---

### Merge a branch into another

Navigate (using `git checkout <branchName>`) to the branch you want the merge **to be applied to** (i.e. the branch that we merge another into!). Then perform the following command to merge the content of branch `x` into the current checked out branch

```bash
git merge <x>
```

---

### Create a pull request

Creating a pull request (PR) can only be done with on the [GitHub website](https://github.com/dedis/popstellar/pulls) by navigating on "Pull requests". Click on the big green button titled "New pull request" and choose the receiving branch (generally `master`) as well as the branch you want to merge into the latter (e.g. `work-be2-scala-raulinn-test`). Click on "Create pull request".

You can then set a title and description to your PR as well as set a label (e.g. `be2-scala`), an assignee (the person responsible for the code), a project (if any) and ask for a specific reviewer using the right side bar. Click on "Create pull request"


---

### Other useful commands

- `git cherry-pick` : apply the changes introduced by some existing commits
- `git rebase` : reapply commits on top of another base tip

