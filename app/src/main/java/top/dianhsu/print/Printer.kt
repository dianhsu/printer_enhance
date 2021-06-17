package top.dianhsu.print

import android.util.Log
import de.gmuth.ipp.core.IppString
import de.gmuth.ipp.cups.CupsClient
import java.lang.Exception
import java.net.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class Printer {
    /**
     * @param name 打印机的名称
     * @param uri 打印机的资源地址
     */
    data class PrinterInfo(val name: IppString, val uri: URI)

    /**
     * 扫描局域网下面的所有的IP地址的631端口，检查是否开放
     * @param ipList 局域网内所有的IP地址
     */
    class MyThread(private var ipList: ArrayList<String>) : Thread() {

        private val _printerPort = 631
        var validIP: ArrayList<String> = ArrayList()

        /**
         * 获取处理器的核心数量
         */
        private fun getProcessorNum(): Int {
            return (Runtime.getRuntime().availableProcessors())
        }

        // 线程池
        private var pool: ExecutorService = Executors.newFixedThreadPool(getProcessorNum() * 10)
        override fun run() {
            super.run()
            validIP.clear()

            val start: Long = System.currentTimeMillis()

            ipList.forEach { ip ->
                val runnable = Runnable {
                    val mSocket = Socket()
                    val socketAddress: SocketAddress = InetSocketAddress(ip, _printerPort)
                    with(mSocket) {
                        try {
                            //println("test ip $ip")
                            connect(socketAddress, 300)
                            close()
                            println(ip)
                            validIP.add(ip)
                        } catch (e: SocketTimeoutException) {

                        } catch (e: ConnectException) {

                        }
                    }
                }
                pool.execute(runnable)
            }
            pool.shutdown()
            while (true) {
                if (pool.isTerminated) {
                    val end = System.currentTimeMillis()
                    println(end - start)
                    break
                }
            }
        }
    }

    /**
     * 获取所有的网卡，不包含本地回环
     */
    private fun getInterfaces(): ArrayList<NetworkInterface> {
        val interfaceList: ArrayList<NetworkInterface> = ArrayList()
        val netInterfaces = NetworkInterface.getNetworkInterfaces()
        while (netInterfaces.hasMoreElements()) {
            val ni = netInterfaces.nextElement()
            if (!ni.isLoopback) {
                interfaceList.add(ni)
            }
        }
        return interfaceList
    }

    /**
     * IP地址转换成字符串
     * @note IPv4
     */
    private fun ipToString(address: Long): String {
        return (address ushr 24 and 0xFF).toString() + "." +
                (address ushr 16 and 0xFF) + "." +
                (address ushr 8 and 0xFF) + "." +
                (address and 0xFF)
    }

    /**
     * 获取本机局域网内所有的IP地址
     * 如果多个网卡相连的IP段相同，需要根据路由选择其中一个网卡的连接作为测试的对象。避免后续出现多个相同网卡的情况
     */
    private fun getHost(): ArrayList<String> {
        val addresses = getInterfaces()
        val hosts = ArrayList<String>()
        val ipSet = HashSet<String>()
        addresses.forEach { ni ->
            ni.interfaceAddresses.forEach { ia ->
                if (!ia.address.toString().contains(':')) {
                    val netmask: Long = (0xff_ff_ff_ff shl (32 - ia.networkPrefixLength))
                    val ipList = ia.address.hostAddress.split('.')
                    val ip =
                        (ipList[0].toLong() shl 24) or (ipList[1].toLong() shl 16) or (ipList[2].toLong() shl 8) or ipList[3].toLong()
                    val startIp = ip and netmask


                    for (i in 1L until ((1 shl 32 - ia.networkPrefixLength)) - 1) {
                        val h = startIp or i
                        if (h == ip) {
                            continue
                        }
                        val curIp = ipToString(startIp or i)
                        if (!ipSet.contains(curIp)) {
                            hosts.add(curIp)
                        }
                    }
                }
            }
        }

        return hosts
    }

    fun findPrinters(): ArrayList<PrinterInfo> {
        val td = MyThread(getHost())
        td.start()
        td.join()
        val ret: ArrayList<PrinterInfo> = ArrayList()
        //Log.d("Printer", td.validIP.toString())
        td.validIP.forEach { ip ->
            try {
                val cupsClient = CupsClient(ip)
                cupsClient.getPrinters().forEach {
                    ret.add(PrinterInfo(it.name, it.printerUri))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return ret
    }
}