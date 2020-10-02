export PATH
export GLAM_BABASHKA
export GLAM_GLOBAL_PATH

GLAM_BABASHKA=false
GLAM_GLOBAL_PATH=""
GLAM_SYS_PATH=$PATH

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
        *"$HOME/.glam/repository/org.babashka/babashka/0.2.2"*)
            export GLAM_BABASHKA
            GLAM_BABASHKA=true
            ;;
        esac
}

glam_detect_bb

glam_join_paths() {
    for I in "$@"
    do
        if [ "$I" != "" ]
        then
            if [ "$out" = "" ]
            then
                out=$I
            else
                out=${out:+$out}:$I
            fi
        fi
    done
    echo "$out"
}

glam() {
    export PATH
    export GLAM_GLOBAL_PATH
    glam_detect_bb
    glam_global_path
    extra_path=""
    if [ "$GLAM_BABASHKA" = "true" ]
    then
        extra_path=$(bb -cp "$(clojure -Spath -A:glam)" -m glam.main -- "$@")
    else
        extra_path=$(clojure -M:glam "$@")
    fi
    GLAM_GLOBAL_PATH="$(glam_join_paths "$extra_path" "$GLAM_GLOBAL_PATH")"
    PATH=$(glam_join_paths "$GLAM_GLOBAL_PATH" "$GLAM_SYS_PATH")
    echo "$GLAM_GLOBAL_PATH"
}
