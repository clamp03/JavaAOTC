#MVPREFIX=/opt/toolchains/crosstools_hf-linux-2.6.12.0_gcc-3.4.6-20_uclibc-0.9.28-20050817-20070131/bin/mipsel-linux-
MVPREFIX=/opt/X-TOOL/tool/x86-linux/toolchains/bdj_bd390/crosstools_sf-linux-2.6.18.0_gcc-4.2-9ts_uclibc-nptl-0.9.29-20070423_20080702/bin/mipsel-linux-
export JDK_HOME=/opt/X-TOOL/tool/x86-linux/java/jdk1.4.2
export PATH=$JDK_HOME/bin:$PATH 
#touch btclasses.zip
make CVM_TARGET_TOOLS_PREFIX=${MVPREFIX} J2ME_CLASSLIB=foundation CVM_AOT=false CVM_DEBUG=false 2>&1 |tee log
#mv generated/javavm/runtime/romjavaAotcAssembly.S ../../src/mips/javavm/runtime/

#cp -rf ./* /opt/broadcom/clamp/j2me
