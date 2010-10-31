
\   Write VWADDR 
\
:   vwaddr ( addr -- )
    \ dup VDPWA c! swpb VDPWA c!
    VDPWA !
;

:   gwaddr ( addr -- )
    \ dup swpb GPLWA c! GPLWA c!
    GPLWA !
;

:   graddr ( -- addr )
    \ GPLRA c@ 8 lsh  GPLRA c@  or 1-
    GPLRA @ 1- 
;

:   g@  ( addr -- )
    gwaddr  GPLRD c@  8 lsh  GPLRD c@  or 
;

:   gc@  ( addr -- )
    gwaddr  GPLRD c@
;

:   gvmove ( gaddr vaddr len -- )
    rot gwaddr
    swap  $4000 or  vwaddr
    \ 0 do  GPLRD c@ VDPWD c! loop
    GPLRD VDPWD rot  0 0 (cmove)
;

:   >bit ( mask -- lowest bit# )
    16 0 do
        dup  1 and  if
            drop i unloop exit
        then
        1 ursh
    loop
    
    drop true
;

