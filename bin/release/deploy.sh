#bin

bin=$(dirname "$0")/..
bin=$(cd "$bin">/dev/null || exit; pwd)
APP_HOME=$(cd "$bin"/..>/dev/null || exit; pwd)

cd "$APP_HOME" || exit;

echo "Deploy the project ..."
echo "Changing version ..."
echo

HOST=master

SNAPSHOT_VERSION=$(head -n 1 "$APP_HOME/VERSION")
VERSION=${SNAPSHOT_VERSION//"-SNAPSHOT"/""}
echo "$VERSION" > "$APP_HOME"/VERSION
find "$APP_HOME" -name 'pom.xml' -exec sed -i "s/$SNAPSHOT_VERSION/$VERSION/" {} \;

SNIPPET=$(grep SNAPSHOT pom.xml)
if [ -n "$SNIPPET" ]; then
  echo "Found SNAPSHOT artifacts in your pom.xml:"
  echo ">>>>>>>>"
  echo "$SNIPPET"
  echo "<<<<<<<<"
  echo "Please upgrade to release versions of the artifacts"
  exit 0
fi

# use the product version of logback
cat "$APP_HOME"/exotic-standalone/src/main/resources/logback-prod.xml > "$APP_HOME"/exotic-standalone/src/main/resources/logback.xml

mvn clean
mvn -PREST-war

exitCode=$?
[ $exitCode -eq 0 ] && echo "Build successfully" || exit 1

REMOTE_BASE_DIR=~/platonic.fun/repo/ai/platon/exotic
ssh $HOST mkdir -p $REMOTE_BASE_DIR

scp -r "$APP_HOME"/exotic-services/target/exotic-services-"$VERSION".jar "$HOST:$REMOTE_BASE_DIR/"
scp -r "$APP_HOME"/exotic-standalone/target/exotic-standalone-"$VERSION".jar "$HOST:$REMOTE_BASE_DIR/"
exitCode=$?
[ $exitCode -eq 0 ] && echo "Copy to remote destination successfully" || exit 1

echo "List directory before creating symbolic link: "
ssh $HOST ls -l $REMOTE_BASE_DIR
ssh $HOST unlink $REMOTE_BASE_DIR/exotic-standalone.jar
ssh $HOST ln -s $REMOTE_BASE_DIR/exotic-standalone-"$VERSION".jar $REMOTE_BASE_DIR/exotic-standalone.jar
echo "List directory after creating symbolic link: "
ssh $HOST ls -l $REMOTE_BASE_DIR
