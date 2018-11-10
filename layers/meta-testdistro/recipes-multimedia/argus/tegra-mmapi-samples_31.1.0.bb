DESCRIPTION = "NVIDIA Tegra Multimedia API headers and examples"
HOMEPAGE = "http://developer.nvidia.com"
LICENSE = "Proprietary & BSD"

SRC_URI = "https://developer.download.nvidia.com/embedded/L4T/r31_Release_v1.0/BSP/Tegra_Multimedia_API_R${PV}_aarch64.tbz2 \
           file://remove-xxd-reference.patch \
           file://jpeg-fixups.patch \
           file://cross-build-fixups.patch \
           file://vector-fixup.patch \
"
SRC_URI[md5sum] = "65f1812bf594d8fe8346c2325cf930f6"
SRC_URI[sha256sum] = "cdf379e2a578d93b5c7b7ac6c7b89c545892eded23c5dbcfe48f490ea4a52478"
COMPATIBLE_MACHINE = "(tegra186|tegra194)"
PACKAGE_ARCH = "${SOC_FAMILY_PKGARCH}"

DEPENDS = "libdrm tegra-mmapi virtual/egl virtual/libgles1 virtual/libgles2 jpeg expat gstreamer1.0 glib-2.0 v4l-utils tensorrt cudnn opencv coreutils-native"

LIC_FILES_CHKSUM = "file://LICENSE;md5=2cc00be68c1227a7c42ff3620ef75d05 \
		    file://argus/LICENSE.TXT;md5=271791ce6ff6f928d44a848145021687"

PACKAGECONFIG ??= "${@bb.utils.contains('DISTRO_FEATURES', 'x11', 'x11', '', d)}"
PACKAGECONFIG[x11] = "-DWITH_X11=ON,,virtual/libx11 gtk+3"

inherit cmake pkgconfig cuda

S = "${WORKDIR}/tegra_multimedia_api"
B = "${S}"

OECMAKE_SOURCEPATH = "${S}/argus"
EXTRA_OECMAKE = "-DMULTIPROCESS=ON \
                 -DCMAKE_INCLUDE_PATH=${S}/include/libjpeg-8b-tegra \
                 -DJPEG_NAMES=libnvjpeg.so"

do_configure() {
    rm -f ${S}/include/nvbuf_utils.h
    #sed -i -e's,\(samples/11\),#\1,' ${S}/Makefile
    find samples -name 'Makefile' -exec sed -i -e's,^LDFLAGS,NVLDFLAGS,' -e's,\$(LDFLAGS),$(LDFLAGS) $(NVLDFLAGS),' {} \;
    cd ${OECMAKE_SOURCEPATH}
    cmake_do_configure
}

do_compile() {
    VERBOSE=1 cmake --build '${B}/argus' -- ${EXTRA_OECMAKE_BUILD}
    export CPP=`echo ${CXX} | sed -e's, .*,,'`
    CXX_EXTRA=`echo ${CXX} | sed -e's,^[^ ]*,,'`
    export CUDA_PATH=${STAGING_DIR_NATIVE}/usr/local/cuda-${CUDA_VERSION}
    PATH=$CUDA_PATH/bin:$PATH
    export CPPFLAGS="${CXX_EXTRA} ${CXXFLAGS} -I${STAGING_DIR_TARGET}/usr/local/cuda-${CUDA_VERSION}/include"
    CPPFLAGS="$CPPFLAGS `pkg-config --cflags libdrm`"
    export LDFLAGS="-L${STAGING_DIR_TARGET}/usr/local/cuda-${CUDA_VERSION}/lib ${LDFLAGS}"
    export CFLAGS="${CFLAGS} `pkg-config --cflags opencv`"
    CCBIN=`which $CPP`
    oe_runmake -j1 all TEGRA_ARMABI=${TARGET_ARCH} TARGET_ROOTFS=${STAGING_DIR_TARGET} NVCC=nvcc NVCCFLAGS="--shared -ccbin=${CCBIN}" GENCODE_FLAGS="${CUDA_NVCC_ARCH_FLAGS}"
}

do_install() {
    DESTDIR="${D}" cmake --build '${B}/argus' --target ${OECMAKE_TARGET_INSTALL}
    install -d ${D}/opt/tegra-mmapi
    cp -R --preserve=mode,timestamps ${S}/data ${D}/opt/tegra-mmapi/
    install -d ${D}/opt/tegra-mmapi/bin
    install -m 0755 ${S}/samples/00_video_decode/video_decode ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/01_video_encode/video_encode ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/02_video_dec_cuda/video_dec_cuda ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/03_video_cuda_enc/video_cuda_enc ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/04_video_dec_trt/video_dec_trt ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/05_jpeg_encode/jpeg_encode ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/06_jpeg_decode/jpeg_decode ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/07_video_convert/video_convert ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/08_video_dec_drm/video_dec_drm ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/09_camera_jpeg_capture/camera_jpeg_capture ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/10_camera_recording/camera_recording ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/12_camera_v4l2_cuda/camera_v4l2_cuda ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/13_multi_camera/multi_camera ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/backend/backend ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/frontend/frontend ${D}/opt/tegra-mmapi/bin/
    install -m 0755 ${S}/samples/v4l2cuda/capture-cuda ${D}/opt/tegra-mmapi/bin/
}

FILES_${PN} += "/opt/tegra-mmapi"
RDEPENDS_${PN} += "tegra-libraries-libv4l-plugins"

DEFAULT_PREFERENCE = "-1"
