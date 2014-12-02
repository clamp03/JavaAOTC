cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
cp AOTCWriter.java.abc   AOTCWriter.java
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
./build.sh
cd new_binary
cp ttt/ abc -rf

#cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
#cp AOTCWriter.java.full    AOTCWriter.java
#cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
#./build.sh
#cd new_binary
#cp ttt/ full -rf

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
cp AOTCWriter.java.inlineWithProfile  AOTCWriter.java
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
./build.sh
cd new_binary
cp ttt/ inlineWithProfile -rf

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
cp AOTCWriter.java.null AOTCWriter.java
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
./build.sh
cd new_binary
cp ttt/ null -rf

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
cp AOTCWriter.java.copy  AOTCWriter.java
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
./build.sh
cd new_binary
cp ttt/ copy -rf

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
cp AOTCWriter.java.inline  AOTCWriter.java
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
./build.sh
cd new_binary
cp ttt/ inline -rf

#cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
#cp AOTCWriter.java.noOpt AOTCWriter.java
#cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
#./build.sh
#cd new_binary
#cp ttt/ noOpt -rf


#cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
#cp AOTCWriter.java.full    AOTCWriter.java
#cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
#make clean
#cp profile/aotcprof_witheembc aotcprof
#cp GNUmakefile_eembc_setjmpTry GNUmakefile
#./doBuild.sh
#cp bin lib obj generated new_binary/ttt/eembc -rf
#
#make clean
#cp profile/aotcprof_withspec aotcprof
#cp GNUmakefile_spec_setjmpTry GNUmakefile
#./doBuild.sh
#./cplib
#./doBuild.sh
#cp bin lib obj generated new_binary/ttt/spec -rf
#cd new_binary
#cp ttt/ setjmpTry -rf


#cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
#cp AOTCWriter.java.full    AOTCWriter.java
#cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
#make clean
#cp profile/aotcprof_witheembc aotcprof
#cp GNUmakefile_eembc_setjmpProlog GNUmakefile
#./doBuild.sh
#cp bin lib obj generated new_binary/ttt/eembc -rf
#make clean
#cp profile/aotcprof_withspec aotcprof
#cp GNUmakefile_spec_setjmpProlog GNUmakefile
#./doBuild.sh
#./cplib
#./doBuild.sh
#cp bin lib obj generated new_binary/ttt/spec -rf
#cd new_binary
#cp ttt/ setjmpProlog -rf
