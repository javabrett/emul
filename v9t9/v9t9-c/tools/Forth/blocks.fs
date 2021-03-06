
\ \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

\   BLOCKS

[IFUNDEF] BLK 
\ a-addr is the address of a cell containing zero or the number of the 
\ mass-storage block being interpreted. If BLK contains zero, the input
\ source is not a block and can be identified by SOURCE-ID, if SOURCE-ID
\ is available. An ambiguous condition exists if a program directly alters
\ the contents of BLK. 
User BLK
[THEN]

[IFUNDEF] C/L
&64 constant C/L
[THEN]

[IFUNDEF] CHARS/BLOCK
&1024 constant CHARS/BLOCK
[THEN]

chars/block c/l /   constant L/BLK
 

[IFUNDEF] FIRST
(first) constant FIRST
[THEN]

\ private
[IFUNDEF] PREV
\ Previously assigned block struct ptr
User PREV
[THEN]

[IFUNDEF] LIMIT
(limit) constant LIMIT
[THEN] 

[IFUNDEF] USE
\ Next allocated block struct ptr
User USE
[THEN] 

[IFUNDEF] SCR
\ Last listed block
User SCR
[THEN] 

\ Block layout:
\
\   block#  if in use, or'ed by 0x8000 if modified
\   <block memory>

$404  constant  (b/blk)

\ read or write:  r/w == TRUE means read, FALSE means write
Code RWBLK       ( addr  block# r/w -- err )
    PUSH            \ put TOS on stack
    xop *SP , SYS^ #
    #rwblock data
    mov *SP+ , R0   \ save result and pop
    c *SP+ , *SP+   \ pop two more
    mov R0 , TOS
    NEXT
end-code

: R/W ( addr block# r/w -- )
    RWBLK -8 ?ERROR
;

\ Distance to the next buffer relative to the previous.
: +BUF ( baddr -- next-addr distance )
    (b/blk) +  dup limit >= if
        drop first
    then
    dup prev @ -
;

\ a-addr is the address of the first character of the block buffer assigned 
\ to block u. The contents of the block are unspecified. An ambiguous condition 
\ exists if u is not an available block number.
\
\ If block u is already in a block buffer, a-addr is the address of that block buffer.
\
\ If block u is not already in memory and there is an unassigned buffer, a-addr
\ is the address of that block buffer.
\
\ If block u is not already in memory and there are no unassigned block buffers, 
\ unassign a block buffer. If the block in that buffer has been UPDATEd, 
\ transfer the block to mass storage. a-addr is the address of that block buffer.
\
\ At the conclusion of the operation, the block buffer pointed to by a-addr
\ is the current block buffer and is assigned to u.

\ This freely reallocates a new buffer, flushing any along the way. 
: buffer    ( u -- a-addr )
    use @  dup >r
    begin +buf until  
    use !
    r@ @  0< if
        r@ cell+  r@ @  $7fff and  0 r/w \ ." r/w" . . .
    then
    r@ !  r@ prev !  r> cell+
;

\ a-addr is the address of the first character of the block buffer assigned 
\ to mass-storage block u. An ambiguous condition exists if u is not an available block number.

\ If block u is already in a block buffer, a-addr is the address of that block buffer.

\ If block u is not already in memory and there is an unassigned block buffer, 
\ transfer block u from mass storage to an unassigned block buffer. a-addr is 
\ the address of that block buffer.

\ If block u is not already in memory and there are no unassigned block buffers,
\ unassign a block buffer. If the block in that buffer has been UPDATEd, transfer
\ the block to mass storage and transfer block u from mass storage into that buffer
\ a-addr is the address of that block buffer.

\ At the conclusion of the operation, the block buffer pointed to by a-addr is the 
\ current block buffer and is assigned to u. 
: block ( u -- a-addr )
\ offset @ +
    >r  prev @  dup @  r@ -
    dup +
    if 
        begin
            +buf 0= if
                drop r@ buffer dup r@ 1 r/w 2- 
            then
            dup @
            r@ - dup + 0= 
        until
        dup prev !
    then
    r> drop cell+
;

\   Tell if the current block buffer is dirty
: dirty?    ( a-addr -- t|f )
    prev @ @ &15 rshift
;

\   Mark the current block buffer as unmodified. An ambiguous 
\   condition exists if there is no current block buffer.
\
\   We do this by marking it unloaded
: revert 
     0 prev @ ! 
;

\   Mark the current block buffer as modified. An ambiguous 
\   condition exists if there is no current block buffer.
\
\   UPDATE does not immediately cause I/O. 
: update 
    $8000 prev @ |!
;

\  Transfer the contents of each UPDATEd block buffer to 
\  mass storage. Mark all buffers as unmodified.  
: save-buffers
    \ Basically, force out all the buffers by bogusly
    \ asking for lots of buffers for block $7fff.
    limit first - (b/blk) / 1+ 0 do
        $7fff buffer drop
    loop
;

\   Perform the function of SAVE-BUFFERS, then unassign all block buffers.  
: flush
    save-buffers
;

: EMPTY-BUFFERS
    first limit over - erase \ flush 
    first use !  first prev !  
;

\ Save the current input-source specification. Store u in 
\ BLK (thus making block u the input source and setting the input 
\ buffer to encompass its contents), set >IN to zero, and interpret. 
\ When the parse area is exhausted, restore the prior input source 
\ specification. Other stack effects are due to the words LOADed.
\
\ An ambiguous condition exists if u is zero or is not a valid block number. 
: load  ( i*x u -- j*x )
    ?dup 0= if -9 ?error then
    <input
    0 >in !
    blk !
    blk @ block >tib !  chars/block #tib !
    interpret
    input>
;

:   ?loading
    blk @ 0= -&10 ?error 
;

: -->
    ?loading
    refill drop
    \ 0 >in !  1 blk +!
;

: thru ( i*x u1 u2 -- j*x )
    1+ swap do i load loop
;

\   get the address of the given line from the block-formatted memory
: (line)    ( line# baddr -- addr u )
    >r c/l * r>  + c/l 
;

\   print the number and the contents of the given line from the block-formatted memory
: .line ( line# baddr label# )
    0 u.r  [char] : emit  space 
    (line) 
    win@ drop s>b drop 6 - min
    type cr
;

\   list all the lines from the given block
: list  ( blk -- )
    dup scr !
    block   ( baddr )
    l/blk 0 do
        i over i .line
        (pause?) if unloop exit then
    loop drop
;

: index ( u1 u2 -- )
    1+ swap do
        0  i block i .line
        (pause?) if unloop exit then
    loop
;



