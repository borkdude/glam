export PATH
export GLAM_BABASHKA
export GLAM_GLOBAL_PATH
export GLAM_LOCAL_PATH

GLAM_BABASHKA=false
GLAM_GLOBAL_PATH=""
GLAM_LOCAL_PATH=""
GLAM_SYS_PATH=$PATH

glam_global_path() {
if [ -f "$HOME/.glam/path" ]
then
    GLAM_GLOBAL_PATH=$(cat "$HOME"/.glam/path)
    PATH="$GLAM_GLOBAL_PATH:$PATH"
fi
}

glam_local_path() {
    if [ -f ".glam/path" ]
    then
        GLAM_LOCAL_PATH=$(cat "$HOME"/.glam/path)
        PATH="$GLAM_GLOBAL_PATH:$PATH"
    fi
}

glam_global_path
glam_local_path

# glam_detect_bb() {
#     case $GLAM_GLOBAL_PATH in
#         *"$HOME/.glam/repository/org.babashka/babashka/0.2.2"*)
#             export GLAM_BABASHKA
#             GLAM_BABASHKA=true
#             ;;
#         esac
# }

# glam_detect_bb

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
    extra_path=$(clojure -M:glam "$@")
    glam_global_path
    glam_local_path
    exit_code=$?
    path="$(glam_join_paths "$extra_path" "$GLAM_LOCAL_PATH" "$GLAM_GLOBAL_PATH")"
    PATH=$(glam_join_paths "$path" "$GLAM_SYS_PATH")
    return $exit_code
}
