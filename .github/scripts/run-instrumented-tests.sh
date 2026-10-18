#!/usr/bin/env bash

status=1
for attempt in 1 2 3; do
  echo "Instrumented test attempt ${attempt}/3"
  firebase emulators:exec \
    --config .firebase-ci.json \
    --project demo-nami \
    --only auth,firestore,storage,functions \
    "./gradlew connectedDebugAndroidTest"
  status=$?
  if [ "${status}" -eq 0 ]; then
    mkdir -p app/build/reports/performance
    find app/build/outputs/androidTest-results -type f -name 'logcat-*' \
      -exec grep -h 'NAMI_PERFORMANCE_JSON:' {} + \
      | sed 's/^.*NAMI_PERFORMANCE_JSON: //' \
      | tail -n 1 \
      > app/build/reports/performance/t38-performance.json
    test -s app/build/reports/performance/t38-performance.json
    exit 0
  fi
  echo "Instrumented tests failed on attempt ${attempt}; restarting Firebase emulators before retry."
  sleep 5
done

exit "${status}"
