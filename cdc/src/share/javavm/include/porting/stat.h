#ifndef _STAT_H
#define _STAT_H
// Clamp for Updated GCC
/* Structure describing file characteristics.  */
struct _stat
  {
	dev_t st_dev;
    long int st_pad1[3];
#ifndef __USE_FILE_OFFSET64
    ino_t st_ino;		/* File serial number.		*/
#else
    ino64_t st_ino;		/* File serial number.		*/
#endif
    mode_t st_mode;		/* File mode.  */
    nlink_t st_nlink;		/* Link count.  */
    uid_t st_uid;		/* User ID of the file's owner.	*/
    gid_t st_gid;		/* Group ID of the file's group.*/
    dev_t st_rdev;	/* Device number, if device.  */
#ifndef USE_FILE_OFFSET64
    long int st_pad2[2];
    off_t st_size;		/* Size of file, in bytes.  */
    /* SVR4 added this extra long to allow for expansion of off_t.  */
    long int st_pad3;
#else
    long int st_pad2[3];
    off64_t st_size;		/* Size of file, in bytes.  */
#endif
    /*
     * Actually this should be timestruc_t st_atime, st_mtime and
     * st_ctime but we don't have it under Linux.
     */
    time_t st_atime;		/* Time of last access.  */
    long int reserved0;
    time_t st_mtime;		/* Time of last modification.  */
    long int reserved1;
    time_t st_ctime;		/* Time of last status change.  */
    long int reserved2;
    blksize_t st_blksize;	/* Optimal block size for I/O.  */
#ifndef USE_FILE_OFFSET64
    blkcnt_t st_blocks;	/* Number of 512-byte blocks allocated.  */
#else
    long int st_pad4;
    blkcnt64_t st_blocks;	/* Number of 512-byte blocks allocated.  */
#endif
    long int st_pad5[14];
  };

//#ifdef USE_LARGEFILE64
struct _stat64
  {
    dev_t st_dev;
    long int st_pad1[3];
    ino64_t st_ino;		/* File serial number.		*/
    mode_t st_mode;		/* File mode.  */
    nlink_t st_nlink;		/* Link count.  */
    uid_t st_uid;		/* User ID of the file's owner.	*/
    gid_t st_gid;		/* Group ID of the file's group.*/
    dev_t st_rdev;	/* Device number, if device.  */
    long int st_pad2[3];
    off64_t st_size;		/* Size of file, in bytes.  */
    /*
     * Actually this should be timestruc_t st_atime, st_mtime and
     * st_ctime but we don't have it under Linux.
     */
    time_t st_atime;		/* Time of last access.  */
    long int reserved0;
    time_t st_mtime;		/* Time of last modification.  */
    long int reserved1;
    time_t st_ctime;		/* Time of last status change.  */
    long int reserved2;
    blksize_t st_blksize;	/* Optimal block size for I/O.  */
    long int st_pad3;
    blkcnt64_t st_blocks;	/* Number of 512-byte blocks allocated.  */
    long int st_pad4[14];
  };
//#endif
#endif
