export PATH
export GLAM_BABASHKA
export GLAM_GLOBAL_PATH

GLAM_BABASHKA=false
GLAM_GLOBAL_PATH=""

glam_global_path() {
if [ -f "$HOME/.glam/path" ]
then
    GLAM_GLOBAL_PATH=$(cat "$HOME"/.glam/path)
    PATH="$GLAM_GLOBAL_PATH:$PATH"
fi
}

glam_global_path

glam_detect_bb() {
    case $GLAM_GLOBAL_PATH in
        *"$HOME/.glam/repository/org.babashka/babashka/0.2.2-SNAPSHOT"*)
            export GLAM_BABASHKA
            GLAM_BABASHKA=true
            ;;
        esac
}

glam_detect_bb

glam() {
    export PATH
    glam_detect_bb
    glam_global_path
    extra_path=""
    if [ "$GLAM_BABASHKA" = "true" ]
    then
        extra_path=$(bb -cp "$(clojure -Spath -A:glam)" -m glam.main -- "$@")
    else
        extra_path=$(clojure -M:glam "$@")
    fi
    PATH=$extra_path:$PATH
}
