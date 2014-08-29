#!/bin/bash

android_path="$PWD"
if [ -z "${ANDROID_HOME}" ]; then
  export ANDROID_HOME=$HOME/.android-sdk
fi
if [ -z "${EMULATOR_VERSION}" ]; then
  export EMULATOR_VERSION=19
fi
android_adb=$ANDROID_HOME/platform-tools/adb
export PATH=$ANDROID_HOME/platform-tools:$PATH

export rvm_trust_rvmrcs_flag=1
gateway_path="$PWD/../$JOB_NAME-gateway"
gateway_pid="/tmp/$JOB_NAME-gateway-server"
gateway_port=3000

cd_gateway() {
  cd $gateway_path
  source $HOME/.rvm/scripts/rvm
  source .rvmrc_ci
}

init_gateway_repo() {
  if [ -d $gateway_path ]; then
    (cd $gateway_path && git clean -dxf && git checkout . && git pull)
  else
    (git clone git@github.braintreeps.com:braintree/gateway.git $gateway_path)
    (cd $gateway_path && git checkout $1)
  fi
}

init_gateway() {
  cd_gateway
  ./ci.sh prepare
}

start_gateway() {
  stop_gateway

  bundle exec thin --port $gateway_port --pid "$gateway_pid" --daemonize start
  sleep 30
}

stop_gateway() {
  if [ -f $gateway_pid ];  then
    bundle exec thin --pid "$gateway_pid" stop
  fi
}

cd_android() {
  cd $android_path
}

init_android() {
  cd_android
  # Build twice, the first build will resolve dependencies via sdk-manager-plugin and then fail
  # https://github.com/JakeWharton/sdk-manager-plugin/issues/10
  ./gradlew --info --no-color clean assembleDebug
  ./gradlew --info --no-color clean lint
  lint_return_code=$?
  if [ $lint_return_code -ne 0 ]; then
    exit 1
  fi

  echo "Starting ADB server"
  $android_adb start-server
  echo "ADB server started"

  echo "Creating emulator"
  echo no | $ANDROID_HOME/tools/android create avd --force -n android$EMULATOR_VERSION -t android-$EMULATOR_VERSION --abi armeabi-v7a
  echo "Starting emulator"
  $ANDROID_HOME/tools/emulator -avd android$EMULATOR_VERSION -no-boot-anim -wipe-data -no-audio -no-window &
}

wait_for_emulator() {
  echo "Waiting for emulator to start"
  $android_adb wait-for-device

  # This is a hack - wait-for-device just checks power on.
  # By polling until the package manager is ready, we can make sure a device is actually booted
  # before attempting to run tests.
  echo "Waiting for device package manager to load"
  while [[ `$android_adb shell pm path android` == 'Error'* ]]; do
    sleep 2
  done
  echo "Emulator fully armed and operational, starting tests"
}

stop_android() {
  $android_adb emu kill
  $android_adb kill-server
  kill -9 `cat /tmp/httpsd.pid`
  kill -9 $screenshot_listener_pid
}

if [ $# -eq 0 ]; then
  init_android

  init_gateway_repo $GATEWAY_BRANCH
  init_gateway
  start_gateway

  wait_for_emulator

  cd_android
  ruby script/httpsd.rb /tmp/httpsd.pid
  ruby screenshot_listener.rb &
  screenshot_listener_pid=$!

  $android_path/gradlew --info --no-color runAllTests connectedAndroidTest
  test_return_code=$?

  stop_gateway
  stop_android

  exit $test_return_code;
else
  case "$1" in
    travis)
      cd_android

      init_android
      wait_for_emulator

      ./gradlew --info --no-color connectedAndroidTest
      test_return_code=$?

      stop_android

      exit $test_return_code;
      ;;
    *)
      echo "Invalid argument: $1"
      exit 1
      ;;
  esac
fi

