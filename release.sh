# Same as -> export newVersion=x.x.x
# Stops at the first command failing, rather than tagging a version that could not be set
set -eu

# The release commit is undone at the end, which would discard any uncommitted change along with it
if ! git diff --quiet || ! git diff --cached --quiet; then
    echo "Commit or stash the changes first" >&2
    exit 1
fi

echo "Enter version number (x.x.x): "
read -r newVersion
case "${newVersion}" in
    "" | *[!0-9.]* | .* | *. | *..*)
        echo "Invalid version number: ${newVersion}" >&2
        exit 1
        ;;
esac

export GPG_TTY=$(tty)
mvn versions:set -DnewVersion="${newVersion}" -DgenerateBackupPoms=false
# mvn clean deploy -Prelease -> Done by Github actions
# Only the version change goes in the release, not whatever else lies untracked in the working tree
git add pom.xml
git commit -m "Release ${newVersion}"
git tag "${newVersion}"
git push origin "${newVersion}"
git reset HEAD~1
git checkout pom.xml
