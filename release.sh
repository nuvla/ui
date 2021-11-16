#!/bin/bash -xe

TAG_VERSION=NONE

VERSION=NONE

PUSH_CHANGES=${1:-false}

BRANCH=master

if [ "${PUSH_CHANGES}" == "true" ]; then
    TARGET=deploy
else
    TARGET=install
fi

do_push() {
    if [ "${PUSH_CHANGES}" == "true" ]; then
        echo "INFO: PUSHING changes."
        git push
    else
        echo "INFO: not pushing changes."
    fi
}

do_push_tag() {
    if [ "${PUSH_CHANGES}" == "true" ]; then
        echo "INFO: PUSHING tag ${TAG_VERSION}."
        git push origin ${TAG_VERSION}
    else
        echo "INFO: not pushing tag."
    fi
}

create_tag() {
    if [ "${PUSH_CHANGES}" == "true" ]; then
        echo "INFO: CREATING tag ${TAG_VERSION}."
        git tag ${TAG_VERSION}
    else
        echo "INFO: not creating tag."
    fi
}

# update pom.xml files for tag and next development version
tag_release() {
  # make the release tag
  git add $(find . -type f -and \( -name project.clj -or -name pom.xml \) | tr '\r\n' ' ')
  git commit -m "release ${TAG_VERSION}"
  do_push
  create_tag
  do_push_tag
}

# update pom.xml files for tag and next development version
update_to_snapshot() {
  # update to next development version
  git add $(find . -type f -and \( -name project.clj -or -name pom.xml \) | tr '\r\n' ' ')
  git commit -m "next development version"
  do_push
}

do_tag() {
    echo "TAGGING ${TAG_VERSION}"
    tag_release
    echo
}

do_update() {
    echo "UPDATING TO SNAPSHOT ${NEXT_RELEASE}"
    update_to_snapshot
    echo
}

update_pom_versions() {
    v=$1
    if [ "${v}" == "" ]; then
        echo "missing version for pom version update"
        exit 1
    fi

    mvn -Djvmargs="-Xmx1024M" \
        -f pom.xml \
        -B \
        -DskipTests \
        versions:set -DnewVersion=${v} -DgenerateBackupPoms=false
}

update_project_versions() {
    v=$1
    if [ "${v}" == "" ]; then
        echo "missing version for project.clj version update"
        exit 1
    fi
    echo 'Updating project.clj versions to ' ${v}
    find . -name project.clj -exec sed -i.bck "s/^(defproject sixsq.nuvla.ui\/code .*/(defproject sixsq.nuvla.ui\/code \"${v}\"/" {} \;
    find . -name project.clj -exec sed -i.bck "s/^(def version .*/(def version \"${v}\")/" {} \;
}


#
# calculate the versions
#

mvn -Djvmargs="-Xmx1024M" \
    -f pom.xml \
    -B \
    -DskipTests \
    validate

source versions.sh

export TAG_VERSION
export NEXT_VERSION

echo ${TAG_VERSION}
echo ${NEXT_VERSION}

#
# update to release version
#

update_pom_versions ${TAG_VERSION}
update_project_versions ${TAG_VERSION}

#
# tag release
#

do_tag

#
# update to next snapshot version
#

update_pom_versions ${NEXT_VERSION}
update_project_versions ${NEXT_VERSION}

#
# update master to snapshot
#
do_update
