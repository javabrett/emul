/*******************************************************************************
 * Copyright (c) 2007, 2011 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 * The Eclipse Public License is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 * http://www.eclipse.org/org/documents/edl-v10.php.
 * You may elect to redistribute this code under either of these licenses.
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     Nokia - Symbian support
 *******************************************************************************/

/*
 * Machine and OS dependend definitions.
 * This module implements host OS abstraction layer that helps make
 * agent code portable between Linux, Windows, VxWorks and potentially other OSes.
 *
 * mdep.h must be included first, before any other header files.
 */

#ifndef D_mdep
#define D_mdep

#define __STDC_FORMAT_MACROS 1

#if defined(WIN32) || defined(__CYGWIN__)
/* MS Windows NT/XP */

#ifndef _WIN32_WINNT
#  define _WIN32_WINNT 0x0501
#endif

#if defined(__CYGWIN__)
#  define _WIN32_IE 0x0501
#elif defined(__MINGW32__)
#  define _WIN32_IE 0x0501
#elif defined(_MSC_VER)
#  pragma warning(disable:4054) /* 'type cast' : from function pointer '...' to data pointer 'void *' */
#  pragma warning(disable:4055) /* 'type cast' : from data pointer 'void *' to function pointer '...' */
#  pragma warning(disable:4127) /* conditional expression is constant */
#  pragma warning(disable:4152) /* nonstandard extension, function/data pointer conversion in expression */
#  pragma warning(disable:4100) /* unreferenced formal parameter */
#  pragma warning(disable:4611) /* interaction between '_setjmp' and C++ object destruction is non-portable */
#  pragma warning(disable:4996) /* 'strcpy': This function or variable may be unsafe */
#  ifdef UNICODE
/* TCF code uses UTF-8 multibyte character encoding */
#    undef UNICODE
#  endif
#  ifdef _DEBUG
#    define _CRTDBG_MAP_ALLOC
#    include <stdlib.h>
#    include <crtdbg.h>
#  endif
#  define _WSPIAPI_H_
#endif

/* winsock2.h must be included before sys/types.h */
#include <winsock2.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <sys/utime.h>
#include <stdio.h>
#include <io.h>

#if defined(_MSC_VER)
   typedef signed __int8 int8_t;
   typedef unsigned __int8 uint8_t;
   typedef signed __int16 int16_t;
   typedef unsigned __int16 uint16_t;
   typedef signed __int32 int32_t;
   typedef unsigned __int32 uint32_t;
   typedef signed __int64 int64_t;
   typedef unsigned __int64 uint64_t;
   typedef int ssize_t;
#  define PRId64 "I64d"
#  define PRIX64 "I64X"
#  define SCNx64 "I64x"
#else
#  include <inttypes.h>
#endif

#define FILE_PATH_SIZE MAX_PATH

typedef int socklen_t;

#if defined(__CYGWIN__)

#include <sys/unistd.h>

#else /* not __CYGWIN__ */

#include <direct.h>
#include <errno.h>

#if !defined(HAVE_STRUCT_TIMESPEC) && !defined(_TIMESPEC_DEFINED)
struct timespec {
    time_t  tv_sec;         /* seconds */
    long    tv_nsec;        /* nanoseconds */
};
#define HAVE_STRUCT_TIMESPEC
#define _TIMESPEC_DEFINED
#endif

#define SIGKILL 1

#ifndef ETIMEDOUT
#define ETIMEDOUT 10060 /* Value from winsock.h. */
#endif

#if defined(__MINGW32__)
typedef unsigned int useconds_t;
#elif defined(_MSC_VER)
#define __i386__
#define strcasecmp(x,y) stricmp(x,y)
typedef unsigned long pid_t;
typedef unsigned long useconds_t;
#endif

#define CLOCK_REALTIME 1
typedef int clockid_t;
extern int clock_gettime(clockid_t clock_id, struct timespec * tp);
extern void usleep(useconds_t useconds);

#define off_t __int64
#define lseek _lseeki64
extern int truncate(const char * path, off_t size);
extern int ftruncate(int f, off_t size);

#if defined(_MSC_VER)
#define utimbuf _utimbuf
#define utime   _utime
#define futime  _futime
#define snprintf _snprintf
#endif

extern int getuid(void);
extern int geteuid(void);
extern int getgid(void);
extern int getegid(void);

extern ssize_t pread(int fd, void * buf, size_t size, off_t offset);
extern ssize_t pwrite(int fd, const void * buf, size_t size, off_t offset);

/* UTF-8 support */
struct utf8_stat {
    dev_t      st_dev;
    ino_t      st_ino;
    unsigned short st_mode;
    short      st_nlink;
    short      st_uid;
    short      st_gid;
    dev_t      st_rdev;
    int64_t    st_size;
    int64_t    st_atime;
    int64_t    st_mtime;
    int64_t    st_ctime;
};
#define stat   utf8_stat
#define lstat  utf8_stat
#define fstat  utf8_fstat
#define open   utf8_open
#define chmod  utf8_chmod
#define remove utf8_remove
#define rmdir  utf8_rmdir
#define mkdir  utf8_mkdir
#define rename utf8_rename
extern int utf8_stat(const char * name, struct utf8_stat * buf);
extern int utf8_fstat(int fd, struct utf8_stat * buf);
extern int utf8_open(const char * name, int flags, int perms);
extern int utf8_chmod(const char * name, int mode);
extern int utf8_remove(const char * path);
extern int utf8_rmdir(const char * path);
extern int utf8_mkdir(const char * path, int mode);
extern int utf8_rename(const char * path1, const char * path2);

/*
 * readdir() emulation with UTF-8 support
 */
struct UTF8_DIR {
  long hdl;
  struct _wfinddatai64_t blk;
  wchar_t * path;
};

struct utf8_dirent {
  char d_name[FILE_PATH_SIZE];
  int64_t d_size;
  time_t d_atime;
  time_t d_ctime;
  time_t d_wtime;
};

typedef struct UTF8_DIR UTF8_DIR;

#define DIR UTF8_DIR
#define dirent   utf8_dirent
#define opendir  utf8_opendir
#define closedir utf8_closedir
#define readdir  utf8_readdir

extern DIR * utf8_opendir(const char * path);
extern int utf8_closedir(DIR * dir);
extern struct utf8_dirent * readdir(DIR * dir);

#endif /* __CYGWIN__ */

extern char * canonicalize_file_name(const char * path);

#define O_LARGEFILE 0

#elif defined(_WRS_KERNEL)
/* VxWork kernel module */

#if !defined(INET)
#  define INET
#endif

#include <vxWorks.h>
#include <version.h>
#include <unistd.h>
#include <socket.h>
#include <strings.h>
#include <sys/ioctl.h>
#include <selectLib.h>
#if _WRS_VXWORKS_MAJOR > 6 || _WRS_VXWORKS_MAJOR == 6 && _WRS_VXWORKS_MINOR >= 6
#  include <private/taskLibP.h>
#endif

#define environ taskIdCurrent->ppEnviron

#if _WRS_VXWORKS_MAJOR < 6 || _WRS_VXWORKS_MAJOR == 6 && _WRS_VXWORKS_MINOR < 9
typedef unsigned int uintptr_t;
#endif

typedef unsigned long useconds_t;

#define FILE_PATH_SIZE PATH_MAX

#ifndef MEM_USAGE_FACTOR
#  define MEM_USAGE_FACTOR 2
#endif

#define O_BINARY 0
#define O_LARGEFILE 0
#define lstat stat

extern int truncate(char * path, int64_t size);
extern char * canonicalize_file_name(const char * path);
extern ssize_t pread(int fd, void * buf, size_t size, off_t offset);
extern ssize_t pwrite(int fd, const void * buf, size_t size, off_t offset);

extern void usleep(useconds_t useconds);

extern int getuid(void);
extern int geteuid(void);
extern int getgid(void);
extern int getegid(void);

#elif defined __SYMBIAN32__
/* Symbian / OpenC */

#include <stddef.h>
#include <stdlib.h>
#include <stdio.h>
#include <socket.h>
#include <sys/types.h>
#include <sys/stat.h>
#include <errno.h>
#include <utime.h>
#include <memory.h>
#include <string.h>
#include <limits.h>
#include <inttypes.h>
#include <fcntl.h>
#include <utime.h>
#include <timespec.h>
#include <e32def.h>
#include <unistd.h>

#include <framework/link.h>

#define MAX_PATH _POSIX_PATH_MAX
#define FILE_PATH_SIZE _POSIX_PATH_MAX

#ifndef MEM_USAGE_FACTOR
#  define MEM_USAGE_FACTOR 2
#endif

#define SIGKILL 1

#define ETIMEDOUT 60

extern int truncate(const char * path, int64_t size);

extern ssize_t pread(int fd, void * buf, size_t size, off_t offset);
extern ssize_t pwrite(int fd, const void * buf, size_t size, off_t offset);

extern int loc_clock_gettime(int, struct timespec *);
#define clock_gettime loc_clock_gettime /* override Open C impl */

#else
/* Linux, BSD, MacOS, UNIX */

#include <unistd.h>
#include <memory.h>
#include <sys/types.h>
#include <sys/select.h>
#include <sys/time.h>
#include <sys/ioctl.h>
#include <sys/socket.h>
#include <limits.h>
#include <inttypes.h>

#define O_BINARY 0

#if defined(__FreeBSD__) || defined(__NetBSD__) || defined(__APPLE__)
#  define O_LARGEFILE 0
extern char ** environ;
extern char * canonicalize_file_name(const char * path);
#endif /* BSD */

#if defined(__APPLE__)
#  define CLOCK_REALTIME 1
  typedef int clockid_t;
  extern int clock_gettime(clockid_t clock_id, struct timespec * tp);
#endif

extern int tkill(pid_t pid, int signal);

#define FILE_PATH_SIZE PATH_MAX

#endif

#ifndef PRId64
#  define PRId64 "lld"
#endif
#ifndef PRIX64
#  define PRIX64 "llX"
#endif
#ifndef SCNx64
#  define SCNx64 "llx"
#endif

#ifndef MEM_USAGE_FACTOR
#  define MEM_USAGE_FACTOR 32
#endif

#if !defined(__FreeBSD__) && !defined(__NetBSD__) && !defined(__APPLE__) && !defined(__VXWORKS__)
extern size_t strlcpy(char * dst, const char * src, size_t size);
extern size_t strlcat(char * dst, const char * src, size_t size);
#endif

#if defined(__UCLIBC__)
extern char * canonicalize_file_name(const char * path);
extern int posix_openpt(int flags);
#endif

#if defined(__i386__) || defined(__x86_64__)
#  define big_endian_host() (0)
#else
   extern int big_endian_host(void);
#endif

/* Return Operating System name */
extern const char * get_os_name(void);

/* Get user home directory path */
extern const char * get_user_home(void);

/* Create new UUID - Universally Unique IDentifier */
extern const char * create_uuid(void);

/* Switch to running in the background, rather than under the direct control of a user */
extern void become_daemon(void);

/* Return 1 if running in the background, return 0 othewise */
extern int is_daemon(void);

/* Initialize mdep module */
extern void ini_mdep(void);

#endif
