MVPREFIX=/opt/toolchains/crosstools_hf-linux-2.6.12.0_gcc-3.4.6-20_uclibc-0.9.28-20050817-20070131/bin/mipsel-linux-
export JDK_HOME=/opt/X-TOOL/tool/x86-linux/java/jdk1.4.2
export PATH=$JDK_HOME/bin:$PATH 
#touch btclasses.zip
make CVM_TARGET_TOOLS_PREFIX=${MVPREFIX} J2ME_CLASSLIB=foundation CVM_AOT=false CVM_DEBUG=false 2>&1 |tee log

#cp -rf ./* /opt/broadcom/clamp/j2me
