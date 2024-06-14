#!/usr/bin/awk -f
#
# © DEDIS lab 2021
#
# This AWK script parses a markdown file and replaces every code block of form
#
# ```json5
# // path/to/file.json
#
#   *code here will be replaced by the content of file.json*
#
# ```
#
# with the content of the file as first comment of the block. 
#
# This script should be used as follow and run in the same folder as the
# file.md:
#
#   ./embedme < file.md > file2.md && mv file2.md file.md

BEGIN { 
    # separate by space to easily get the filename from the comment of form
    # "//" SPACE FILENAME
    FS=" ";
    
    # used to say if the previous line was an opening code block (i.e.
    # "```json5")
    last_line=0;

    # used to say if we are currently removing the old content of a block
    del=0;
}

{
    # if this is a closing block line, unsets the del variable to stop deleting
    # lines if it was the case
    if ($0 ~ /^```$/) {
        del=0;
        print $0;
    
    # if we are currently deleting lines, then we should just not print the line
    } else if (del == 1) {
        # nothing to do, line is deleted

    # if this is the start of an opening block, we indicate that with the
    # last_line variable
    } else if ($0 ~ /```json5/) {
        last_line=1;
        print $0;

    # if the previous line was an opening block, and we get a comment, then
    # let's fill the block with the file's content and remove the old lines from
    # the block. $1 will contain "//" and $2 will contain the filename.
    } else if ($0 ~ /\/\/ .+\.json/ && last_line == 1) {

        # check if the file exists.
        if (system("test -f " $2)==0) {
            print $0;
            print "";
            while(( getline line<$2) > 0 ) {
                print line;
            }
            close($2);
            print "";
            del=1;
        } else {
            print $0;
            print "// error: embedme: file '"$2"' not found";
        }

    # the "normal" case. We unset last_line in case it was set
    } else {
        last_line=0;
        print $0;
    }
}

END {
}
