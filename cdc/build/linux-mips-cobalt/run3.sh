cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
cp AOTCWriter.java.noOpt AOTCWriter.java

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/share
cp defs.mk.O0 defs.mk
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
./build.sh
cd new_binary
cp ttt/ noOpt_O0 -rf

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/share
cp defs.mk.O1 defs.mk
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
./build.sh
cd new_binary
cp ttt/ noOpt_O1 -rf

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/share
cp defs.mk.O2 defs.mk
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
./build.sh
cd new_binary
cp ttt/ noOpt_O2 -rf

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/src/share/javavm/jcc/aotc
cp AOTCWriter.java.full    AOTCWriter.java

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/share
cp defs.mk.O0 defs.mk
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
./build.sh
cd new_binary
cp ttt/ fullOpt_O0 -rf

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/share
cp defs.mk.O1 defs.mk
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
./build.sh
cd new_binary
cp ttt/ fullOpt_O1 -rf

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/share
cp defs.mk.O2 defs.mk
cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/linux-mips-cobalt
./build.sh
cd new_binary
cp ttt/ fullOpt_O2 -rf

cd ~/graduation/cleanCode/PhoneMeAdvanced/cdc/build/share
cp defs.mk.O2 defs.mk
