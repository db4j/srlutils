# copyright nqzero 2017 - see License.txt for terms

srlutils.sort


this note is written long after the creation of these files, so some of this is speculation
i've tried my best to retrace my footsteps
(using ght log -p -- historyfile)

much of the code is templated
- write for the double case
- render to all primitives and objects
- paste into destination file

srlutils/doc/templatize.sh
  this tool takes the input code (for double) and duplicates it once each for float, int, etc
  reads either standard input or xclip

Sorti.java - Indirect.java    
Sortj.java - java.util.Arrays
Sorto.java - DirectOrder.java
Sortp.java - DirectFast.java



MacroDevel
make all changes to this file for doubles, render to the other types using genPrimitives
and paste into Direct.java. some fixup is required, boolean compares and generics

then copy/paste everything into the other templated classes





regex='\_/* XXX_'
script=../../../doc/templatize.sh

ii=templated/Direct.java
sed -n MacroDevel.java -e "$regex,$regex p" | $script |
    sed -i -n $ii -e "1,$regex p; $ r /dev/stdin"
echo } >> $ii


the resulting Direct.java will require fixup
known changes:
- boolean logic in compare
- TT extends Comparable


then render Direct.java to the other derived classes:

    


(
cd templated
for ii in DirectFast.java DirectOrder.java Indirect.java; do
    sed -n Direct.java -e "1,$regex d; p" |
        sed -n -i $ii -e "1,$regex p; $ r /dev/stdin"
done
)


Config:
  limit: size of an assertion sort
  robust: use a random pivot, otherwise a fixed pivot
  rand: random source, for repeatability 
which: a static type (Yes, No, Index:indirect)
  used to select between different implementations from MacroDevel
  No: direct access
  Yes: direct access, but preserve order
  Index: indirect access, ie thru the order array

Indirect and DirectOrder are similar
  both keep an order array, but indirect accesses elements thru this mapping
Direct and DirectFast are similar
  fast uses a fixed pivot (slow for sorted input) and and a fixed config object

all but DirectFast are robust to sorted input



fixme -- many of the comments are ambiguous about ranges: [k1,k2) vs [k1,k2]
  especially in the context of a Part

performance for DirectFast seems to be 50% slower than the builtin java sort
  i haven't investigated what factors influence this or if this has always be true






