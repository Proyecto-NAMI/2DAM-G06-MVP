#!/usr/bin/env bash

status=1
for attempt in 1 2 3; do
  echo "Instrumented test attempt ${attempt}/3"
  firebase emulators:exec \
    --config .firebase-ci.json \
    --project demo-nami \
    --only auth,firestore,functions,storage \
    "./gradlew connectedDebugAndroidTest"
  status=$?
  if [ "${status}" -eq 0 ]; then
    exit 0
  fi
  echo "Instrumented tests failed on attempt ${attempt}; restarting Firebase emulators before retry."
  sleep 5
done

exit "${status}"
