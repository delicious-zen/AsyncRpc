package com.delicious.asyncRpc

import io.vertx.core.Future
import io.vertx.core.Promise
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Method
import java.lang.reflect.Proxy

class AsyncRpc(
    private val rpcRequestSender: RpcRequestSender
) {

    private val proxyCache: HashMap<String, Any> = HashMap()

    fun getClient(target: Any): Any {
        val key = getRpcClassStr(target)
        if (!proxyCache.containsKey(key)) {
            proxyCache[key] = Proxy.newProxyInstance(
                target::class.java.classLoader,
                target.javaClass.interfaces,
                DynamicProxyHandler(target, rpcRequestSender)
            )
        }
        return proxyCache[key]!!
    }

    private class DynamicProxyHandler(
        private var target: Any,
        private val rpcRequestSender: RpcRequestSender
    ) : InvocationHandler {
        override fun invoke(proxy: Any, method: Method, args: Array< Any>): Future<Any>? {
            val res = Promise.promise<Any>()
            val rpcRequest = RpcRequest(
                serviceClass = getRpcClassStr(target),
                methodId = getMethodId(method),
                args = args
            )
            rpcRequestSender.send(res, rpcRequest)
            return res.future()
        }
    }
}
