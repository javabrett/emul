;   kbd.inc
; 
;   (c) 1996-2014 Ed Swartz
; 
;   All rights reserved. This program and the accompanying materials
;   are made available under the terms of the Eclipse Public License v1.0
;   which accompanies this distribution, and is available at
;   http://www.eclipse.org/legal/epl-v10.html
; 

;--------------------------------------------------------------------------

kinit
    ;   reset clears all memory

    mov    #>1e03,@kbdlimit  ; 1/2 s before repeat, 1/20 s delay between repeat
	
	rt

;   Scan the keyboard.
;
;   We store the scancode, if any, in kbdscan, 
;   and the shift state in kbdshift (even uncombined).
;   The translated keycode (ASCII) is stored in a
;   buffer at kbdbuf.  Repeats and delays are controlled
;   by kbdlimit and kbddelay.
;
;	This is called during an interrupt!
;
;	(I.E., don't use the stack!)
;
;	WP = INTWS

scankbd	mov	11,10

	clr		1		 	
	movb	1, @KBDA	; clear alpha lock, set row 0

	clr		3			; row * >100
	movb	3, @KBD 

	movb	@KBD,4	 	; get row 0
	;inv		4			; 0=off 1=on
	
	clr     5
	cb      #>72, 4     ; ctrl+fctn+shift+space (abort)?
	jne     sknobreak
	
	seto    5           ; remember for later
	
sknobreak:	
	mov		4,2			; copy to R2=shifts
	andi	2,>7000		; save 0=off 1=on (shifts)
	movb	2,@kbdshft	; save shift
	andi	4,>0700		; mask =, space, enter
	jmp		skloop0

skloop:
	clr		4		 	
	movb	@KBD, 4		; read 8 bits	
skloop0:
	jne		skgotsome	; any bits set?
	ai		3,>100
	ci		3,>600		; stop at joystick
	jeq		skblank

	movb	3, @KBD	
	jmp		skloop

skgotsome:
	srl		3,5		 	; entry into table
	swpb	4		 	; move to low byte so we can
skwhich:
	srl		4,1			; roll down
	joc		skdone	 	; this bit?
	inc		3			; next
	jmp		skwhich

skblank:
	movb	1,@kbdscan	; no key whatsoever

	movb	@kbdshft,3	; shifts?
	jeq		sknone

	jmp		sknone0

skdone:	
	bl		@kbdhandle
sknone0:
    a       @timeout,@randnoise
    clr     @timeout
	bl     	@vscreenon

sknone:
	movb	1,@kbdlast	   	; update last char
	a		1,@randnoise

	b		*10

;-------------------------------------------------------------------------

kbdhandle
;    movb    @kbdmode, 9     ; kbd_trans set?
;	jlt		khgetascii
;
;    inc     3
;	swpb	3
;	movb	3,@kbdscan	 	; save scancode
;	movb	3,1		 		; R3=R1=scancode
;	jmp		khtestbuffer
;	
;khgetascii:
    mov		@GPLRA, 8      ; save GROM addr in R8
	dec		8

	srl		2,11		 	; get shift state
	
	li		9, GPLRD
	mov		#grom_kbdlist, @GPLWA
	movb	*9, 1
	swpb	1
	movb	*9, 1
	swpb	1
	a		1, 2			; point to keyboard tables	

	mov		2,@GPLWA		; point to grom kbd list

	movb	*9, 2		   ; get table entry
	swpb	2
	movb	*9, 2	   
	swpb	2

	a		3,2		 		; get offset

	mov		2,@GPLWA		; point to char
	
	movb	*9,1	   	 	; R1=key code, 0-255

	mov		8,@GPLWA    	; restore GROM addr

    movb	#>80, @KBDA
    movb	@KBDA, 2
	jeq		khnoalpha
		
	ci		1,>6100		; alpha lock on; 
	jl		khnoalpha	; test 'a'-'z'
	ci		1,>7b00
	jhe		khnoalpha
	ai		1,->2000	; uppercase

khnoalpha:
	inc     3
	swpb	3			; put scancode in hi byte

khtestbuffer:
	sla		4,1		 	; kbd_poll set?
	joc		khnone

	; HACK!  Fctn-Shift-S is treated as Ctrl-H
	cb		#211, 1
	jne		$0+
	cb		#>30, @kbdshft
	jne		$0
	
	li		1,>0800
	
$0:
	socb	1,1
	jne		khbuffer   	; got something

	jmp		khnone

;-------------------------------------------

;   Buffer a key if it's new or the same as the last
;   one and a significant time has elapsed.
khbuffer:	
	cb		3,@kbdscan 	       ; scancode the same?
	jne		khnew

	movb	@kbdflag, 2      	; get flags
	ab      2,2
	jnc		khb4repeat          ; repeating yet?

	cb		@kbdtimer,@kbddelay ; time for new repeat?
	jl		khnone
	jmp		khstuff

khb4repeat:
	cb		@kbdtimer,@kbdlimit ; repeated long enough yet?
	jl		khnone		     	; no
	
    socb    #>80,@kbdflag       ; set repeat flag
	
	jmp		khstuff

khnew:
	szcb	#>80,@kbdflag	    ; clear repeat flag
	movb	3,@kbdscan			; save new scancode

khstuff:
	sb		@kbdtimer,@kbdtimer ; restart timer

    mov     5,5                 ; check abort flag
    jeq     khstuffit
    
    clr     @kbdhead
    clr     @kbdflag
    
    blwp	@>FFFC				; NMI

khstuffit:
	movb	@kbdtail,2	       	; get current pos in ring
    srl     2,8
	movb	1,@kbdbuf(2)	    ; buffer it
	inc		2		     		; inc...
	andi	2,kbdbufsize-1	    ; roll over if necc
	swpb	2
	cb		2,@kbdhead	     	; overflow if equal!
	jeq		khnone		     	; eeeer... don't update ptrs

	movb	2,@kbdtail	     	; update

khnone:

khout:
	rt




;--------------------------------------------------

;	If key available, return EQ=0.
;
;	Should be called with INTERRUPTS DISABLED.
;

kbdavail 
;    movb    @kbdmode, 0
;    sla     0, 2                ; is kbd_poll set?
;	jnc		katestbuff
;	
;	movb	@kbdscan,0         	; kbdscan=0 means none
;	rt
;
;katestbuff:
	cb		@kbdhead,@kbdtail	; EQ=1 means none
	rt


xt_keypressed
	cb		@kbdhead,@kbdtail	; EQ=1 means none
	clr		*12
	jeq		_k0
	seto	*12
_k0:	
	rt

;	Read the last char from the keyboard buffer.
;	Assumes that "kbdavail" returned positively.
;
;	Returns all r0=ASCII-esque keycode.
;
;	If R0=0, then no char is available.
;
;	This should be called WITH INTERRUPTS DISABLED, as the interrupt
;	routine WILL modify all these variables.

kbdread	   
;	movb	@kbdmode,1
;	sla		1,2					; kbd_poll set?
;	jnc		krbuff
;
;	movb	@kbdscan,0			; read last polled char
;	srl		0,8
;	rt
;
;krbuff:							; read from keyboard buffer
	clr		0
	clr		1
	cb		@kbdhead,@kbdtail
	jeq		krbempty
	movb	@kbdhead,1			; get head ptr
	ab		#1,@kbdhead		; and inc...
	szcb	#-kbdbufsize,@kbdhead	; mask...
	swpb	1					; and make offset
	movb	@kbdbuf(1),0		; and retrieve!
	srl		0,8
krbempty:
	rt

xt_readkey
	PUSH	SP, 11
	bl		@kbdread
	POP		SP, 11
	
	mov		0, *12
	rt
	