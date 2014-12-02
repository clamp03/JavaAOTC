date=`date`
echo ---------------------------------------------------------------------------
echo $date SPECjvm98 Result 

echo ---------------------------------------------------------------------------
../build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms8m SpecApplication -s1 _201_compress

../build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms3m SpecApplication -s1 _202_jess

../build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms12m SpecApplication -s1 _209_db

../build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms12m SpecApplication -s1 _213_javac

../build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms2m SpecApplication -s1 _222_mpegaudio

../build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms10m SpecApplication -s1 _227_mtrt

../build/linux-mips/bin/cvm -Xbootclasspath=../build/linux-mips/lib/personal.jar:./lib/rt-jdk13.jar -Djava.class.path=. -Djava.home=../build/linux-mips -Xms2m SpecApplication -s1 _228_jack

