// Generated by jextract

package net.codecrete.usb.linux.gen.ioctl;

import java.lang.invoke.MethodHandle;

import static java.lang.foreign.ValueLayout.*;
public class ioctl  {

    /* package-private */ ioctl() {}
    public static OfByte C_CHAR = Constants$root.C_CHAR$LAYOUT;
    public static OfShort C_SHORT = Constants$root.C_SHORT$LAYOUT;
    public static OfInt C_INT = Constants$root.C_INT$LAYOUT;
    public static OfLong C_LONG = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfLong C_LONG_LONG = Constants$root.C_LONG_LONG$LAYOUT;
    public static OfFloat C_FLOAT = Constants$root.C_FLOAT$LAYOUT;
    public static OfDouble C_DOUBLE = Constants$root.C_DOUBLE$LAYOUT;
    public static OfAddress C_POINTER = Constants$root.C_POINTER$LAYOUT;
    public static int _SYS_IOCTL_H() {
        return (int)1L;
    }
    public static int _FEATURES_H() {
        return (int)1L;
    }
    public static int _DEFAULT_SOURCE() {
        return (int)1L;
    }
    public static int __GLIBC_USE_ISOC2X() {
        return (int)0L;
    }
    public static int __USE_ISOC11() {
        return (int)1L;
    }
    public static int __USE_ISOC99() {
        return (int)1L;
    }
    public static int __USE_ISOC95() {
        return (int)1L;
    }
    public static int __USE_POSIX_IMPLICITLY() {
        return (int)1L;
    }
    public static int _POSIX_SOURCE() {
        return (int)1L;
    }
    public static int __USE_POSIX() {
        return (int)1L;
    }
    public static int __USE_POSIX2() {
        return (int)1L;
    }
    public static int __USE_POSIX199309() {
        return (int)1L;
    }
    public static int __USE_POSIX199506() {
        return (int)1L;
    }
    public static int __USE_XOPEN2K() {
        return (int)1L;
    }
    public static int __USE_XOPEN2K8() {
        return (int)1L;
    }
    public static int _ATFILE_SOURCE() {
        return (int)1L;
    }
    public static int __WORDSIZE() {
        return (int)64L;
    }
    public static int __WORDSIZE_TIME64_COMPAT32() {
        return (int)1L;
    }
    public static int __SYSCALL_WORDSIZE() {
        return (int)64L;
    }
    public static int __USE_MISC() {
        return (int)1L;
    }
    public static int __USE_ATFILE() {
        return (int)1L;
    }
    public static int __USE_FORTIFY_LEVEL() {
        return (int)0L;
    }
    public static int __GLIBC_USE_DEPRECATED_GETS() {
        return (int)0L;
    }
    public static int __GLIBC_USE_DEPRECATED_SCANF() {
        return (int)0L;
    }
    public static int _STDC_PREDEF_H() {
        return (int)1L;
    }
    public static int __STDC_IEC_559__() {
        return (int)1L;
    }
    public static int __STDC_IEC_559_COMPLEX__() {
        return (int)1L;
    }
    public static int __GNU_LIBRARY__() {
        return (int)6L;
    }
    public static int __GLIBC__() {
        return (int)2L;
    }
    public static int __GLIBC_MINOR__() {
        return (int)35L;
    }
    public static int _SYS_CDEFS_H() {
        return (int)1L;
    }
    public static int __glibc_c99_flexarr_available() {
        return (int)1L;
    }
    public static int __LDOUBLE_REDIRECTS_TO_FLOAT128_ABI() {
        return (int)0L;
    }
    public static int __HAVE_GENERIC_SELECTION() {
        return (int)1L;
    }
    public static int _IOC_NRBITS() {
        return (int)8L;
    }
    public static int _IOC_TYPEBITS() {
        return (int)8L;
    }
    public static int _IOC_SIZEBITS() {
        return (int)14L;
    }
    public static int _IOC_DIRBITS() {
        return (int)2L;
    }
    public static int _IOC_NRSHIFT() {
        return (int)0L;
    }
    public static int TCGETS() {
        return (int)21505L;
    }
    public static int TCSETS() {
        return (int)21506L;
    }
    public static int TCSETSW() {
        return (int)21507L;
    }
    public static int TCSETSF() {
        return (int)21508L;
    }
    public static int TCGETA() {
        return (int)21509L;
    }
    public static int TCSETA() {
        return (int)21510L;
    }
    public static int TCSETAW() {
        return (int)21511L;
    }
    public static int TCSETAF() {
        return (int)21512L;
    }
    public static int TCSBRK() {
        return (int)21513L;
    }
    public static int TCXONC() {
        return (int)21514L;
    }
    public static int TCFLSH() {
        return (int)21515L;
    }
    public static int TIOCEXCL() {
        return (int)21516L;
    }
    public static int TIOCNXCL() {
        return (int)21517L;
    }
    public static int TIOCSCTTY() {
        return (int)21518L;
    }
    public static int TIOCGPGRP() {
        return (int)21519L;
    }
    public static int TIOCSPGRP() {
        return (int)21520L;
    }
    public static int TIOCOUTQ() {
        return (int)21521L;
    }
    public static int TIOCSTI() {
        return (int)21522L;
    }
    public static int TIOCGWINSZ() {
        return (int)21523L;
    }
    public static int TIOCSWINSZ() {
        return (int)21524L;
    }
    public static int TIOCMGET() {
        return (int)21525L;
    }
    public static int TIOCMBIS() {
        return (int)21526L;
    }
    public static int TIOCMBIC() {
        return (int)21527L;
    }
    public static int TIOCMSET() {
        return (int)21528L;
    }
    public static int TIOCGSOFTCAR() {
        return (int)21529L;
    }
    public static int TIOCSSOFTCAR() {
        return (int)21530L;
    }
    public static int FIONREAD() {
        return (int)21531L;
    }
    public static int TIOCLINUX() {
        return (int)21532L;
    }
    public static int TIOCCONS() {
        return (int)21533L;
    }
    public static int TIOCGSERIAL() {
        return (int)21534L;
    }
    public static int TIOCSSERIAL() {
        return (int)21535L;
    }
    public static int TIOCPKT() {
        return (int)21536L;
    }
    public static int FIONBIO() {
        return (int)21537L;
    }
    public static int TIOCNOTTY() {
        return (int)21538L;
    }
    public static int TIOCSETD() {
        return (int)21539L;
    }
    public static int TIOCGETD() {
        return (int)21540L;
    }
    public static int TCSBRKP() {
        return (int)21541L;
    }
    public static int TIOCSBRK() {
        return (int)21543L;
    }
    public static int TIOCCBRK() {
        return (int)21544L;
    }
    public static int TIOCGSID() {
        return (int)21545L;
    }
    public static int TIOCGRS485() {
        return (int)21550L;
    }
    public static int TIOCSRS485() {
        return (int)21551L;
    }
    public static int TCGETX() {
        return (int)21554L;
    }
    public static int TCSETX() {
        return (int)21555L;
    }
    public static int TCSETXF() {
        return (int)21556L;
    }
    public static int TCSETXW() {
        return (int)21557L;
    }
    public static int TIOCVHANGUP() {
        return (int)21559L;
    }
    public static int FIONCLEX() {
        return (int)21584L;
    }
    public static int FIOCLEX() {
        return (int)21585L;
    }
    public static int FIOASYNC() {
        return (int)21586L;
    }
    public static int TIOCSERCONFIG() {
        return (int)21587L;
    }
    public static int TIOCSERGWILD() {
        return (int)21588L;
    }
    public static int TIOCSERSWILD() {
        return (int)21589L;
    }
    public static int TIOCGLCKTRMIOS() {
        return (int)21590L;
    }
    public static int TIOCSLCKTRMIOS() {
        return (int)21591L;
    }
    public static int TIOCSERGSTRUCT() {
        return (int)21592L;
    }
    public static int TIOCSERGETLSR() {
        return (int)21593L;
    }
    public static int TIOCSERGETMULTI() {
        return (int)21594L;
    }
    public static int TIOCSERSETMULTI() {
        return (int)21595L;
    }
    public static int TIOCMIWAIT() {
        return (int)21596L;
    }
    public static int TIOCGICOUNT() {
        return (int)21597L;
    }
    public static int FIOQSIZE() {
        return (int)21600L;
    }
    public static int TIOCPKT_DATA() {
        return (int)0L;
    }
    public static int TIOCPKT_FLUSHREAD() {
        return (int)1L;
    }
    public static int TIOCPKT_FLUSHWRITE() {
        return (int)2L;
    }
    public static int TIOCPKT_STOP() {
        return (int)4L;
    }
    public static int TIOCPKT_START() {
        return (int)8L;
    }
    public static int TIOCPKT_NOSTOP() {
        return (int)16L;
    }
    public static int TIOCPKT_DOSTOP() {
        return (int)32L;
    }
    public static int TIOCPKT_IOCTL() {
        return (int)64L;
    }
    public static int TIOCSER_TEMT() {
        return (int)1L;
    }
    public static int SIOCADDRT() {
        return (int)35083L;
    }
    public static int SIOCDELRT() {
        return (int)35084L;
    }
    public static int SIOCRTMSG() {
        return (int)35085L;
    }
    public static int SIOCGIFNAME() {
        return (int)35088L;
    }
    public static int SIOCSIFLINK() {
        return (int)35089L;
    }
    public static int SIOCGIFCONF() {
        return (int)35090L;
    }
    public static int SIOCGIFFLAGS() {
        return (int)35091L;
    }
    public static int SIOCSIFFLAGS() {
        return (int)35092L;
    }
    public static int SIOCGIFADDR() {
        return (int)35093L;
    }
    public static int SIOCSIFADDR() {
        return (int)35094L;
    }
    public static int SIOCGIFDSTADDR() {
        return (int)35095L;
    }
    public static int SIOCSIFDSTADDR() {
        return (int)35096L;
    }
    public static int SIOCGIFBRDADDR() {
        return (int)35097L;
    }
    public static int SIOCSIFBRDADDR() {
        return (int)35098L;
    }
    public static int SIOCGIFNETMASK() {
        return (int)35099L;
    }
    public static int SIOCSIFNETMASK() {
        return (int)35100L;
    }
    public static int SIOCGIFMETRIC() {
        return (int)35101L;
    }
    public static int SIOCSIFMETRIC() {
        return (int)35102L;
    }
    public static int SIOCGIFMEM() {
        return (int)35103L;
    }
    public static int SIOCSIFMEM() {
        return (int)35104L;
    }
    public static int SIOCGIFMTU() {
        return (int)35105L;
    }
    public static int SIOCSIFMTU() {
        return (int)35106L;
    }
    public static int SIOCSIFNAME() {
        return (int)35107L;
    }
    public static int SIOCSIFHWADDR() {
        return (int)35108L;
    }
    public static int SIOCGIFENCAP() {
        return (int)35109L;
    }
    public static int SIOCSIFENCAP() {
        return (int)35110L;
    }
    public static int SIOCGIFHWADDR() {
        return (int)35111L;
    }
    public static int SIOCGIFSLAVE() {
        return (int)35113L;
    }
    public static int SIOCSIFSLAVE() {
        return (int)35120L;
    }
    public static int SIOCADDMULTI() {
        return (int)35121L;
    }
    public static int SIOCDELMULTI() {
        return (int)35122L;
    }
    public static int SIOCGIFINDEX() {
        return (int)35123L;
    }
    public static int SIOCSIFPFLAGS() {
        return (int)35124L;
    }
    public static int SIOCGIFPFLAGS() {
        return (int)35125L;
    }
    public static int SIOCDIFADDR() {
        return (int)35126L;
    }
    public static int SIOCSIFHWBROADCAST() {
        return (int)35127L;
    }
    public static int SIOCGIFCOUNT() {
        return (int)35128L;
    }
    public static int SIOCGIFBR() {
        return (int)35136L;
    }
    public static int SIOCSIFBR() {
        return (int)35137L;
    }
    public static int SIOCGIFTXQLEN() {
        return (int)35138L;
    }
    public static int SIOCSIFTXQLEN() {
        return (int)35139L;
    }
    public static int SIOCDARP() {
        return (int)35155L;
    }
    public static int SIOCGARP() {
        return (int)35156L;
    }
    public static int SIOCSARP() {
        return (int)35157L;
    }
    public static int SIOCDRARP() {
        return (int)35168L;
    }
    public static int SIOCGRARP() {
        return (int)35169L;
    }
    public static int SIOCSRARP() {
        return (int)35170L;
    }
    public static int SIOCGIFMAP() {
        return (int)35184L;
    }
    public static int SIOCSIFMAP() {
        return (int)35185L;
    }
    public static int SIOCADDDLCI() {
        return (int)35200L;
    }
    public static int SIOCDELDLCI() {
        return (int)35201L;
    }
    public static int SIOCDEVPRIVATE() {
        return (int)35312L;
    }
    public static int SIOCPROTOPRIVATE() {
        return (int)35296L;
    }
    public static int NCC() {
        return (int)8L;
    }
    public static int TIOCM_LE() {
        return (int)1L;
    }
    public static int TIOCM_DTR() {
        return (int)2L;
    }
    public static int TIOCM_RTS() {
        return (int)4L;
    }
    public static int TIOCM_ST() {
        return (int)8L;
    }
    public static int TIOCM_SR() {
        return (int)16L;
    }
    public static int TIOCM_CTS() {
        return (int)32L;
    }
    public static int TIOCM_CAR() {
        return (int)64L;
    }
    public static int TIOCM_RNG() {
        return (int)128L;
    }
    public static int TIOCM_DSR() {
        return (int)256L;
    }
    public static int N_TTY() {
        return (int)0L;
    }
    public static int N_SLIP() {
        return (int)1L;
    }
    public static int N_MOUSE() {
        return (int)2L;
    }
    public static int N_PPP() {
        return (int)3L;
    }
    public static int N_STRIP() {
        return (int)4L;
    }
    public static int N_AX25() {
        return (int)5L;
    }
    public static int N_X25() {
        return (int)6L;
    }
    public static int N_6PACK() {
        return (int)7L;
    }
    public static int N_MASC() {
        return (int)8L;
    }
    public static int N_R3964() {
        return (int)9L;
    }
    public static int N_PROFIBUS_FDL() {
        return (int)10L;
    }
    public static int N_IRDA() {
        return (int)11L;
    }
    public static int N_SMSBLOCK() {
        return (int)12L;
    }
    public static int N_HDLC() {
        return (int)13L;
    }
    public static int N_SYNC_PPP() {
        return (int)14L;
    }
    public static int N_HCI() {
        return (int)15L;
    }
    public static int CERASE() {
        return (int)127L;
    }
    public static int CMIN() {
        return (int)1L;
    }
    public static int CQUIT() {
        return (int)28L;
    }
    public static int CTIME() {
        return (int)0L;
    }
    public static MethodHandle ioctl$MH() {
        return RuntimeHelper.requireNonNull(constants$0.ioctl$MH,"ioctl");
    }
    public static int ioctl ( int __fd,  long __request, Object... x2) {
        var mh$ = ioctl$MH();
        try {
            return (int)mh$.invokeExact(__fd, __request, x2);
        } catch (Throwable ex$) {
            throw new AssertionError("should not reach here", ex$);
        }
    }
    public static long _POSIX_C_SOURCE() {
        return 200809L;
    }
    public static int __TIMESIZE() {
        return (int)64L;
    }
    public static long __STDC_IEC_60559_BFP__() {
        return 201404L;
    }
    public static long __STDC_IEC_60559_COMPLEX__() {
        return 201404L;
    }
    public static long __STDC_ISO_10646__() {
        return 201706L;
    }
    public static int _IOC_NRMASK() {
        return (int)255L;
    }
    public static int _IOC_TYPEMASK() {
        return (int)255L;
    }
    public static int _IOC_SIZEMASK() {
        return (int)16383L;
    }
    public static int _IOC_DIRMASK() {
        return (int)3L;
    }
    public static int _IOC_TYPESHIFT() {
        return (int)8L;
    }
    public static int _IOC_SIZESHIFT() {
        return (int)16L;
    }
    public static int _IOC_DIRSHIFT() {
        return (int)30L;
    }
    public static int _IOC_NONE() {
        return (int)0L;
    }
    public static int _IOC_WRITE() {
        return (int)1L;
    }
    public static int _IOC_READ() {
        return (int)2L;
    }
    public static int IOC_IN() {
        return (int)1073741824L;
    }
    public static int IOC_OUT() {
        return (int)2147483648L;
    }
    public static int IOC_INOUT() {
        return (int)3221225472L;
    }
    public static int IOCSIZE_MASK() {
        return (int)1073676288L;
    }
    public static int IOCSIZE_SHIFT() {
        return (int)16L;
    }
    public static int TIOCINQ() {
        return (int)21531L;
    }
    public static long TIOCGPTN() {
        return 2147767344L;
    }
    public static long TIOCSPTLCK() {
        return 1074025521L;
    }
    public static long TIOCGDEV() {
        return 2147767346L;
    }
    public static long TIOCSIG() {
        return 1074025526L;
    }
    public static long TIOCGPKT() {
        return 2147767352L;
    }
    public static long TIOCGPTLCK() {
        return 2147767353L;
    }
    public static long TIOCGEXCL() {
        return 2147767360L;
    }
    public static int TIOCGPTPEER() {
        return (int)21569L;
    }
    public static int SIOGIFINDEX() {
        return (int)35123L;
    }
    public static int TIOCM_CD() {
        return (int)64L;
    }
    public static int TIOCM_RI() {
        return (int)128L;
    }
    public static int CEOF() {
        return (int)4L;
    }
    public static int CEOL() {
        return (int)0L;
    }
    public static int CINTR() {
        return (int)3L;
    }
    public static int CSTATUS() {
        return (int)0L;
    }
    public static int CKILL() {
        return (int)21L;
    }
    public static int CSUSP() {
        return (int)26L;
    }
    public static int CDSUSP() {
        return (int)25L;
    }
    public static int CSTART() {
        return (int)17L;
    }
    public static int CSTOP() {
        return (int)19L;
    }
    public static int CLNEXT() {
        return (int)22L;
    }
    public static int CDISCARD() {
        return (int)15L;
    }
    public static int CWERASE() {
        return (int)23L;
    }
    public static int CREPRINT() {
        return (int)18L;
    }
    public static int CEOT() {
        return (int)4L;
    }
    public static int CBRK() {
        return (int)0L;
    }
    public static int CRPRNT() {
        return (int)18L;
    }
    public static int CFLUSH() {
        return (int)15L;
    }
}


