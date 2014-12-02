make clean
cp profile/aotcprof_witheembc aotcprof
cp GNUmakefile_eembc GNUmakefile
./doBuild.sh
cp bin lib obj generated new_binary/ttt/eembc -rf

make clean
cp profile/aotcprof_withspec aotcprof
cp GNUmakefile_spec GNUmakefile
./doBuild.sh
./cplib
./doBuild.sh
cp bin lib obj generated new_binary/ttt/spec -rf
