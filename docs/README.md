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

### Working together

You might at some point end up working on feature `X` together with another – or more – teammate(s). Dealing with this situation unprepared *will* lead you down a rabbit hole of painful and seemingly endless merge conflicts. 

In an attempt to avoid as many as these merge conflicts, here's what you should **avoid**:
- Do not, under any circumstances, copy-paste code snippets between students/branches to make your code compile. You should rather ask your teammate to push their changes online, and incorporate them into your work (see below how we suggest you achieve this);

Here's how we suggest you work on feature `X` with two (or more) teammates:
1. Create a branch that is common between both of you. If feature `X` relies on branch `Y` (that is awaiting reviews, i.e. not merged into `master`), then create your common branch originating from branch `Y`. If it is not related to any unmerged branches, then default to a copy of `master`.
2. create a personal branch for each student that is originating from the common branch. Additionally (arguably optionally), you may decide to never push changes directly to the common branch but instead use PRs from either of your personal branches to the common branch.
3. When you have a self-contained piece of code that works, add it to the common branch (either through PRs or `git merge`). This way, your partner will be able to use it without the need of copy-pastas.
4. When you need new code that is on the common branch but not yet on your personal branch, simply merge the common branch inside your personal branch.
5. Once you are all happy and the feature is fully finished (and thus operational on the common branch), simply create a PR from the common branch to branch `Y` if `Y` has not been merged yet or `master` if `Y` has already been merged.

Here's what it could look like with student Pierluca and student Nicolas working together on the feature `X` in a common branch that is using features from `Y`:

```
      +----------------------------+
      |           Master           |
      +-------------+--------------+
                    |
                    |
      +-------------+--------------+
      |          branch-y          |
      +-------------+--------------+
                    |
                    |
      +-------------+--------------+
      |       dev-common-pn        |
      +----------------------------+
           ^ ^                ^
           | |                | 
           | |                |
      +----+-+---+       +----+----+
      | Pierluca |       | Nicolas |
      +----------+       +---------+
```


---

### Other useful commands

- `git cherry-pick` : apply the changes introduced by some existing commits
- `git rebase` : reapply commits on top of another base tip

