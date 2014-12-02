export JDK_HOME=/opt/X-TOOL/tool/x86-linux/java/jdk1.4.2
export PATH=$JDK_HOME/bin:$PATH 
#touch btclasses.zip
make CVM_TARGET_TOOLS_PREFIX=${MVPREFIX} J2ME_CLASSLIB=foundation CVM_AOT=false CVM_JIT=false CVM_DEBUG=false 2>&1 |tee log

#cp -rf ./* /opt/broadcom/clamp/j2me
