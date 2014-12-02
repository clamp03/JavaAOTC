cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
cp AOTCWriter.java.full    AOTCWriter.java

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
make clean
cp profile/aotcprof_witheembc aotcprof
cp GNUmakefile_eembc_fullcni GNUmakefile
./doBuild.sh
cp bin lib obj generated new_binary/ttt/eembc -rf
make clean
cp profile/aotcprof_withspec aotcprof
cp GNUmakefile_spec_fullcni GNUmakefile
./doBuild.sh
./cplib
./doBuild.sh
cp bin lib obj generated new_binary/ttt/spec -rf
cd new_binary
cp ttt/ fullcni -rf


cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
cp AOTCWriter.java.full    AOTCWriter.java
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
make clean
cp profile/aotcprof_witheembc aotcprof
cp GNUmakefile_eembc_fulljni GNUmakefile
./doBuild.sh
cp bin lib obj generated new_binary/ttt/eembc -rf
make clean
cp profile/aotcprof_withspec aotcprof
cp GNUmakefile_spec_fulljni GNUmakefile
./doBuild.sh
./cplib
./doBuild.sh
cp bin lib obj generated new_binary/ttt/spec -rf
cd new_binary
cp ttt/ fulljni -rf

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
cp AOTCWriter.java.full    AOTCWriter.java
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
make clean
cp profile/aotcprof_eembc aotcprof
cp GNUmakefile_eembc_partialcni GNUmakefile
./doBuild.sh
cp bin lib obj generated new_binary/ttt/eembc -rf
make clean
cp profile/aotcprof_spec aotcprof
cp GNUmakefile_spec_partialcni GNUmakefile
./doBuild.sh
./cplib
./doBuild.sh
cp bin lib obj generated new_binary/ttt/spec -rf
cd new_binary
cp ttt/ partialcni -rf

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
cp AOTCWriter.java.full    AOTCWriter.java
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
make clean
cp profile/aotcprof_eembc aotcprof
cp GNUmakefile_eembc_partialjni GNUmakefile
./doBuild.sh
cp bin lib obj generated new_binary/ttt/eembc -rf
make clean
cp profile/aotcprof_spec aotcprof
cp GNUmakefile_spec_partialjni GNUmakefile
./doBuild.sh
./cplib
./doBuild.sh
cp bin lib obj generated new_binary/ttt/spec -rf
cd new_binary
cp ttt/ partialjni -rf

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
cp AOTCWriter.java.full    AOTCWriter.java
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
make clean
cp profile/aotcprof_eembc aotcprof
cp GNUmakefile_eembc_partialwrapper GNUmakefile
./doBuild.sh
cp bin lib obj generated new_binary/ttt/eembc -rf
make clean
cp profile/aotcprof_spec aotcprof
cp GNUmakefile_spec_partialwrapper GNUmakefile
./doBuild.sh
./cplib
./doBuild.sh
cp bin lib obj generated new_binary/ttt/spec -rf
cd new_binary
cp ttt/ partialwrapper -rf
