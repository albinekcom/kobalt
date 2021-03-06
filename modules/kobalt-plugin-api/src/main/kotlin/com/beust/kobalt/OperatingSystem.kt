package com.beust.kobalt

import java.io.File

abstract class OperatingSystem {

    override fun toString(): String {
        return getName() + " " + getVersion() + " " + System.getProperty("os.arch")
    }

    fun getName(): String {
        return System.getProperty("os.name")
    }

    fun getVersion(): String {
        return System.getProperty("os.version")
    }

    open fun isWindows(): Boolean {
        return false
    }

    open fun isUnix(): Boolean {
        return false
    }

    open fun isMacOsX(): Boolean {
        return false
    }

    open fun isLinux(): Boolean {
        return false
    }

    abstract fun getNativePrefix(): String

    abstract fun getScriptName(scriptPath: String): String

    abstract fun getExecutableName(executablePath: String): String

    abstract fun getSharedLibraryName(libraryName: String): String

    abstract fun getStaticLibraryName(libraryName: String): String

    abstract fun getFamilyName(): String

    /**
     * Locates the given executable in the system path. Returns null if not found.
     */
    fun findInPath(name: String): File? {
        val exeName = getExecutableName(name)
        if (exeName.contains(File.separator)) {
            val candidate = File(exeName)
            if (candidate.isFile) {
                return candidate
            }
            return null
        }
        for (dir in path) {
            val candidate = File(dir, exeName)
            if (candidate.isFile) {
                return candidate
            }
        }

        return null
    }

//    fun findAllInPath(name: String): List<File> {
//        val all = LinkedList<File>()
//
//        for (dir in getPath()) {
//            val candidate = File(dir, name)
//            if (candidate.isFile) {
//                all.add(candidate)
//            }
//        }
//
//        return all
//    }

    val path: List<File>
        get() {
            return (System.getenv(getPathVar()) ?: return emptyList()).split(File.pathSeparator).map { File(it) }
        }

    open fun getPathVar(): String {
        return "PATH"
    }

    class Windows : OperatingSystem() {
        override fun isWindows(): Boolean {
            return true
        }

        override fun getFamilyName(): String {
            return "windows"
        }

        override fun getScriptName(scriptPath: String): String {
            return withSuffix(scriptPath, ".bat")
        }

        override fun getExecutableName(executablePath: String): String {
            return withSuffix(executablePath, ".exe")
        }

        override fun getSharedLibraryName(libraryName: String): String {
            return withSuffix(libraryName, ".dll")
        }

        override fun getStaticLibraryName(libraryName: String): String {
            return withSuffix(libraryName, ".lib")
        }

        override fun getNativePrefix(): String {
            var arch = System.getProperty("os.arch")
            if ("i386" == arch) {
                arch = "x86"
            }
            return "win32-" + arch
        }

        private fun withSuffix(executablePath: String, extension: String): String {
            if (executablePath.toLowerCase().endsWith(extension)) {
                return executablePath
            }
            return removeExtension(executablePath) + extension
        }

        private fun removeExtension(executablePath: String): String {
            val fileNameStart = Math.max(executablePath.lastIndexOf('/'), executablePath.lastIndexOf('\\'))
            val extensionPos = executablePath.lastIndexOf('.')

            if (extensionPos > fileNameStart) {
                return executablePath.substring(0, extensionPos)
            }
            return executablePath
        }


        override fun getPathVar(): String {
            return "Path"
        }
    }

    open class Unix : OperatingSystem() {
        override fun getScriptName(scriptPath: String): String {
            return scriptPath
        }

        override fun getFamilyName(): String {
            return "unknown"
        }

        override fun getExecutableName(executablePath: String): String {
            return executablePath
        }

        override fun getSharedLibraryName(libraryName: String): String {
            return getLibraryName(libraryName, getSharedLibSuffix())
        }

        private fun getLibraryName(libraryName: String, suffix: String): String {
            if (libraryName.endsWith(suffix)) {
                return libraryName
            }
            val pos = libraryName.lastIndexOf('/')
            if (pos >= 0) {
                return libraryName.substring(0, pos + 1) + "lib" + libraryName.substring(pos + 1) + suffix
            } else {
                return "lib" + libraryName + suffix
            }
        }

        protected open fun getSharedLibSuffix(): String {
            return ".so"
        }

        override fun getStaticLibraryName(libraryName: String): String {
            return getLibraryName(libraryName, ".a")
        }

        override fun isUnix(): Boolean {
            return true
        }

        override fun getNativePrefix(): String {
            val arch = getArch()
            var osPrefix = getOsPrefix()
            osPrefix += "-" + arch
            return osPrefix
        }

        protected open fun getArch(): String {
            var arch = System.getProperty("os.arch")
            if ("x86" == arch) {
                arch = "i386"
            }
            if ("x86_64" == arch) {
                arch = "amd64"
            }
            if ("powerpc" == arch) {
                arch = "ppc"
            }
            return arch
        }

        protected open fun getOsPrefix(): String {
            var osPrefix = getName().toLowerCase()
            val space = osPrefix.indexOf(" ")
            if (space != -1) {
                osPrefix = osPrefix.substring(0, space)
            }
            return osPrefix
        }
    }

    class MacOs : Unix() {
        override fun isMacOsX(): Boolean {
            return true
        }

        override fun getFamilyName(): String {
            return "os x"
        }

        override fun getSharedLibSuffix(): String {
            return ".dylib"
        }

        override fun getNativePrefix(): String {
            return "darwin"
        }
    }

    class Linux : Unix() {
        override fun isLinux(): Boolean {
            return true
        }

        override fun getFamilyName(): String {
            return "linux"
        }
    }

    class FreeBSD : Unix()

    class Solaris : Unix() {
        override fun getFamilyName(): String {
            return "solaris"
        }

        override fun getOsPrefix(): String {
            return "sunos"
        }

        override fun getArch(): String {
            val arch = System.getProperty("os.arch")
            if (arch == "i386" || arch == "x86") {
                return "x86"
            }
            return super.getArch()
        }
    }

    companion object {
        val WINDOWS: Windows = Windows()
        val MAC_OS: MacOs = MacOs()
        val SOLARIS: Solaris = Solaris()
        val LINUX: Linux = Linux()
        val FREE_BSD: FreeBSD = FreeBSD()
        val UNIX: Unix = Unix()

        fun current(): OperatingSystem {
            return forName(System.getProperty("os.name"))
        }

        fun forName(os: String): OperatingSystem {
            val osName = os.toLowerCase()
            if (osName.contains("windows")) {
                return WINDOWS
            } else if (osName.contains("mac os x") || osName.contains("darwin") || osName.contains("osx")) {
                return MAC_OS
            } else if (osName.contains("sunos") || osName.contains("solaris")) {
                return SOLARIS
            } else if (osName.contains("linux")) {
                return LINUX
            } else if (osName.contains("freebsd")) {
                return FREE_BSD
            } else {
                // Not strictly true
                return UNIX
            }
        }
    }
}

