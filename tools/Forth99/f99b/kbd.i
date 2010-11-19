
16          Constant    kbdbufsize

kbdbufsize  RamVar      kbdbuf

10          RamVar      kbdstate

kbdstate     Constant kbdtimer

kbdstate 1 + Constant kbddelay

kbdstate 2 + Constant kbdrate

kbdstate 3 + Constant kbdshift
kbdstate 4 + Constant kbdscan
kbdstate 5 + Constant kbdlast
kbdstate 6 + Constant kbdflag 

kbdstate 7 + Constant kbdtail
kbdstate 8 + Constant kbdhead

cell    RamVar    timeout
cell    RamVar    randnoise

:   read-row  ( col -- bits )
    'KBD c!   'KBD c@
;

:   toupper     ( ch -- ch )
    dup $61 >=  over $7B <=  or  -$20 and  -
;

:   buffer-key  ( -- )
    \ restart timer
    0 kbdtimer c!
    
    \ add to buffer
    kbdtail c@  dup  kbdbuf +  kbdscan c@  swap  c!

    \ advance pointer, unless we hit the head    
    1+  kbdbufsize  1- and  kbdhead c@  over
    - if
        kbdtail c!
    else
        drop
    then 
;

:   repeat-key  ( ch -- )
    \ remember the key
    dup kbdscan c!

    kbdlast c@  over =  if
        \ same key: see if enough time has elapsed since last key
        \ [char] ( demit kbdflag c@ .d kbdtimer c@ .d [char] ) demit 
        
        kbdflag c@        \ repeating? 
        if
            kbdtimer c@ kbdrate c@  <  if  drop exit  then
        else
            \ see if time to repeat
            kbdtimer c@ kbddelay c@  <  if  drop exit  then
            
            true kbdflag c!
        then
    else
        \ new key: reset repeat flag
        0 kbdflag c! 
    then

    \ remember the key
    kbdlast c!

    buffer-key
;

:   lookup-key ( tableoffs -- )

    graddr >r
         
        kbdshift c@  3 rshift  grom_kbdlist +      \ get shifted table ptr
        
        g@      \ get table entry
        +       \ and char offset
        
        gc@     \ read char
        
    r> gwaddr
;
    
:   handle-key  ( raw -- )
    
    \ check alpha and uppercase if needed
    $80 'KBDA c!  'KBDA c@  if  toupper  then  $00 'KBDA c!
    
    \ dup .d 10 demit
    
    \ Fctn-Shift-S  -->  Ctrl-H
    dup 211 = if  
        \ kbdshift c@ .d 10 demit
        $30 kbdshift c@ = if  drop $08  then 
    then

    
    ?dup if  repeat-key  then 
;

\   Actions when any key is detected
:   key-actions
    timeout @ randnoise +!
    kbdscan c@ randnoise +!
     
    0 timeout !
    
    true vid-show
;

:   kbd-scan
    \ clear alpha lock line and read break-able bits
    $0 'KBDA c!
    
    0 read-row
    
    dup $72 = if                    \ ctrl+fctn+shift+space (abort)?
        abort
    then
    
    $70 and  kbdshift c!           \ save shift bits
    0 kbdscan c!
    
    6 0 do 
        i   read-row
        
        i 0= if $07 and then        \ ignore shifts in first row
         
        ?dup if
            i  3 lshift            \ table row offset
            
            swap >bit swap +    \ column
                        
            lookup-key handle-key leave
        then
    loop
    
    kbdscan c@ 0= if
        \ no key!
        kbd-no-key exit 
    then
    
    key-actions
    
;

:   kbd-no-key
    0 kbdscan c!  0 kbdtimer c!  0 kbdflag c!  0 kbdlast c!
;

:   kbd-init
    30 kbddelay c!   \ 1/2 s before repeat
    3 kbdrate c!     \ 1/20 s delay between repeat
    0 kbdtail c!
    0 kbdhead c!
    kbd-no-key
;

true <EXPORT

:   key?    ( -- f )
    kbdhead c@  kbdtail c@  = not  
;

: (key)     ( -- key | 0 )
    key? if
        kbdhead c@  
        dup  1+ kbdbufsize 1- and kbdhead c!    \ advance
        kbdbuf + c@ 
    else
        0
    then
;

: key    ( -- key | 0 )
    begin
        ints-on (idle) ints-off
        key?
    until
    (key)
;


EXPORT>
