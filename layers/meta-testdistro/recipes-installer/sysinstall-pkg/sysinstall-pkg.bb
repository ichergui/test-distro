DESCRIPTION = "Package a rootfs image for the installer"
LICENSE = "MIT"
LIC_FILES_CHKSUM = "file://${COMMON_LICENSE_DIR}/MIT;md5=0835ade698e0bcf8506ecda2f7b4f302"

SYSINSTALL_PKG_IMAGE ??= "core-image-sato"

S = "${WORKDIR}"
B = "${WORKDIR}/build"

do_configure[noexec] = "1"
do_compile[noexec] = "1"

do_install() {
    install -d ${D}/installer
    install -m 0644 ${DEPLOY_DIR_IMAGE}/${SYSINSTALL_PKG_IMAGE}-${MACHINE}.tar.gz ${D}/installer/image.tar.gz
}
do_install[depends] += "${SYSINSTALL_PKG_IMAGE}:do_image_complete"

FILES_${PN} = "/installer"

PACKAGE_ARCH = "${MACHINE_ARCH}"

