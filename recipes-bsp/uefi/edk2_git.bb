SUMMARY = "Modern, feature-rich, cross-platform firmware development environment for the UEFI and PI specifications"

LICENSE = "BSD"
LIC_FILES_CHKSUM = "file://BaseTools/License.txt;md5=a041d47c90fd51b4514d09a5127210e6 \
                   "

DEPENDS += "util-linux-native iasl-native"

inherit deploy

PV = "0.0+${SRCPV}"

SRCREV_FORMAT = "edk2-atf"

SRCREV_edk2 = "c0cb1e1a72bccb5c83d7a36a8e52a38002b18671"
SRCREV_atf = "d0c104e1e1ad0102f0f4c70997b7ee6e6fbbe273"
SRCREV_openplatformpkg = "028971707ed3fe327421c9cad7510ade574dc653"
SRCREV_uefitools = "8c8bfc6263e81242ea51f1f9e304078dda70e149"

SRC_URI = "git://github.com/tianocore/edk2.git;name=edk2 \
           git://github.com/ARM-software/arm-trusted-firmware.git;name=atf;destsuffix=git/atf \
           git://git.linaro.org/uefi/OpenPlatformPkg.git;name=openplatformpkg;destsuffix=git/OpenPlatformPkg \
          "

SRC_URI_append = " git://git.linaro.org/uefi/uefi-tools.git;name=uefitools;destsuffix=git/uefi-tools \
                 "

S = "${WORKDIR}/git"

export EDK2_DIR = "${S}"

export CROSS_COMPILE_64 = "${TARGET_PREFIX}"
export CROSS_COMPILE_32 = "${TARGET_PREFIX}"

# Override variables from BaseTools/Source/C/Makefiles/header.makefile
# to build BaseTools with host toolchain
export CC = "${BUILD_CC}"
export CXX = "${BUILD_CXX}"
export AS = "${BUILD_CC}"
export AR = "${BUILD_AR}"
export LD = "${BUILD_LD}"
export LINKER = "${CC}"

# This is a bootloader, so unset OE LDFLAGS.
# OE assumes ld==gcc and passes -Wl,foo
LDFLAGS = ""

export UEFIMACHINE ?= "${MACHINE_ARCH}"
OPTEE_OS_ARG ?= ""

do_compile() {
    # Add in path to native sysroot to find uuid/uuid.h
    sed -i -e 's:-I \.\.:-I \.\. -I ${STAGING_INCDIR_NATIVE} :' ${S}/BaseTools/Source/C/Makefiles/header.makefile
    # ... and the library itself
    sed -i -e 's: -luuid: -luuid -L ${STAGING_LIBDIR_NATIVE}:g' ${S}/BaseTools/Source/C/*/GNUmakefile

    ${EDK2_DIR}/uefi-tools/uefi-build.sh -T GCC49 -b RELEASE -a ${EDK2_DIR}/atf ${OPTEE_OS_ARG} ${UEFIMACHINE}
}

do_install() {
    # Placeholder to be overriden in machine specific recipe
    if [ -e ${EDK2_DIR}/atf/build/${UEFIMACHINE}/release/fip.bin ] ; then
        install -d ${D}${libdir}/edk2/
        install -D -p -m0644 ${EDK2_DIR}/atf/build/${UEFIMACHINE}/release/*.bin ${D}${libdir}/edk2/
    fi
}

do_deploy() {
    # Placeholder to be overriden in machine specific recipe
    if [ -e ${EDK2_DIR}/atf/build/${UEFIMACHINE}/release/fip.bin ] ; then
        install -D -p -m0644 ${EDK2_DIR}/atf/build/${UEFIMACHINE}/release/*.bin ${DEPLOYDIR}
    fi
}

PACKAGE_ARCH = "${MACHINE_ARCH}"
FILES_${PN} += "/boot"

addtask deploy before do_build after do_compile
