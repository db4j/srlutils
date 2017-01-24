#!/bin/bash
# copyright nqzero 2017 - see License.txt for terms

clip=$1

if [[ $clip ]]; then
    text=$(xclip -o -sel c)
    cmd="xclip -i -sel c"
else
    text=$(</dev/stdin)
    cmd=cat
fi


(
echo "$text"
for ii in '  float' boolean '   byte' '   char' '  short' '   long' '    int'; do
    jj=$(echo $ii | sed   \
-e "s/double/Double/"   \
-e "s/float/Float/"   \
-e "s/boolean/Boolean/"   \
-e "s/byte/Byte/"   \
-e "s/char/Character/"   \
-e "s/short/Short/"   \
-e "s/long/Long/"   \
-e "s/int/Integer/"   \
-e "s/TT/TT/"   \
)
    echo "$text" | sed -e "s/ Double/ $jj/g" -e "s/Double /$jj /g" -e "s/ double/$ii/g"
done


echo "$text" | sed -e "s/ doubles/ Objects/g" -e "s/ double/     TT/g"


) | eval "$cmd"




