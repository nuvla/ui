#!/bin/bash -xe

mkdir -p ~/.gnupg/private-keys-v1.d
chmod 700 ~/.gnupg/private-keys-v1.d

gpg_pubkey=".github/sixsq.gpg_pubkey.bin"
nb_signed_file="code/resources/public/ui/downloads/nuvlabox-self-registration.py.gpg"
ne_signed_file="code/resources/public/ui/downloads/nuvlaedge-self-registration.py.gpg"

gpg --version
gpg --dry-run --batch --status-fd=1 --no-sig-cache --no-default-keyring --keyring "${gpg_pubkey}" --verify "${nb_signed_file}"
gpg --dry-run --batch --status-fd=1 --no-sig-cache --no-default-keyring --keyring "${gpg_pubkey}" --verify "${ne_signed_file}"
