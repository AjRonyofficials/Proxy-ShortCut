package hev.socks5

import hev.htproxy.TProxyService

object TProxy {
    @JvmStatic
    fun Start(configPath: String, fd: Int): Boolean {
        return TProxyService.TProxyStartService(configPath, fd)
    }

    @JvmStatic
    fun Stop(): Boolean {
        return TProxyService.TProxyStopService()
    }

    @JvmStatic
    fun isRunning(): Boolean {
        return TProxyService.TProxyIsRunning()
    }

    @JvmStatic
    fun getStats(): LongArray {
        return TProxyService.TProxyGetStats()
    }
}
