export PATH
export GLAM_BABASHKA

GLAM_BABASHKA=false
global_path=false

if [ -f "$HOME/.glam/path" ]
then
    PATH="$(cat "$HOME"/.glam/path 2>/dev/null):$PATH"
    global_path=true
fi

glam_detect_bb() {
if [ $global_path ] && [ -f "$HOME/.glam/repository/org.babashka/babashka/0.2.2-SNAPSHOT/bb" ]
then
    export GLAM_BABASHKA
    GLAM_BABASHKA=TRUE
fi
}

glam_detect_bb

glam() {
    export PATH
    glam_detect_bb
    extra_path=""
    if [ $GLAM_BABASHKA ]
    then
        extra_path=$(bb -cp "$(clojure -Spath -A:glam)" -m glam.main -- "$@")
    else
        extra_path=$(clojure -M:glam -- "$@")
    fi
    PATH=$extra_path:$PATH
}
