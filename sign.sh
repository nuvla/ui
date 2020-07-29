#!/bin/sh

file_to_sign="code/resources/public/ui/downloads/nuvlabox-self-registration.py"

gpg --local-user 751A93DBA8E709E8A303007810A8124ECA7D9EB9 --sign "${file_to_sign}"
