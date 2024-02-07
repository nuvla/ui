## Release Process

**Before** creating the release:

 - Decide what semantic version to use for this release and change the
   version, if necessary, in `code/project.clj` and all the `pom.xml`
   files.  (It should still have the "-SNAPSHOT" suffix.)

 - Update the `CHANGELOG.md` file.

 - Push all changes to GitHub, including the updates to
   `CHANGELOG.md`.

Again, be sure to set the version **before** tagging the release.

Check that everything builds correctly with:

    mvn clean install

To tag the code with the release version and to update the master
branch to the next snapshot version, run the command:

    ./release.sh true

If you want to test what will happen with the release, leave off the
"true" argument and the changes will only be made locally.

When the tag is pushed to GitHub, CI will build the repository, create the
Docker image, and push it to Docker Hub. Check the GitHub Actions build and
ensure that the new container version appears in the Docker Hub.
