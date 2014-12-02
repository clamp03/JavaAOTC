date=`date`
echo ---------------------------------------------------------------------------
echo $date SPECjvm98 Result 

echo ---------------------------------------------------------------------------
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms8m SpecApplication -s10 _201_compress
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms8m SpecApplication -s10 _201_compress
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms8m SpecApplication -s10 _201_compress

/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms3m SpecApplication -s100 _202_jess
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms3m SpecApplication -s100 _202_jess
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms3m SpecApplication -s100 _202_jess

/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms12m SpecApplication -s100 _209_db
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms12m SpecApplication -s100 _209_db
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms12m SpecApplication -s100 _209_db

/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms12m SpecApplication -s100 _213_javac
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms12m SpecApplication -s100 _213_javac
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms12m SpecApplication -s100 _213_javac

/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms2m SpecApplication -s10 _222_mpegaudio
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms2m SpecApplication -s10 _222_mpegaudio
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms2m SpecApplication -s10 _222_mpegaudio

/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms10m SpecApplication -s10 _227_mtrt
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms10m SpecApplication -s10 _227_mtrt
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms10m SpecApplication -s10 _227_mtrt

/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms2m SpecApplication -s100 _228_jack
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms2m SpecApplication -s100 _228_jack
/ocap/OCAP_LINUX/CDC/build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms2m SpecApplication -s100 _228_jack

/bin/sync
#/sbin/poweroff
