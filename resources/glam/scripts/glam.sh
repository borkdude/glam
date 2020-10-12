export PATH
export GLAM_GLOBAL_PATH
export GLAM_LOCAL_PATH

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
    # shellcheck disable=SC2153
    if [ "$GLAM_CMD" != "" ]
    then
        glam_cmd="$GLAM_CMD"
    elif [ -x "$(which glam-bin)" ]
    then
         glam_cmd="glam-bin"
    else
        >&2 echo "glam-bin not found, have you installed it and added it to the path?"
        return 1
    fi
    extra_path=$(eval "$glam_cmd" "$@")
    glam_global_path
    glam_local_path
    exit_code=$?
    path="$(glam_join_paths "$extra_path" "$GLAM_LOCAL_PATH" "$GLAM_GLOBAL_PATH")"
    PATH=$(glam_join_paths "$path" "$GLAM_SYS_PATH")
    return $exit_code
}
