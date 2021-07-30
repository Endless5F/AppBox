package com.android.core.ext

import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext

// 创建一个Job，并用这个Job来管理CoroutineScope的所有子协程
val job = Job()
val coroutineContext: CoroutineContext = Dispatchers.Main + job
// 方法三，自行通过 CoroutineContext 创建一个 CoroutineScope 对象
//                                    👇 需要一个类型为 CoroutineContext 的参数
val coroutineScope = CoroutineScope(coroutineContext)

// 开始执行协程
fun startCoroutine(block: suspend CoroutineScope.() -> Unit) {
    coroutineScope.launch(Dispatchers.Main) {
        // 切换到 IO 线程，并在执行完成后切回 UI 线程
        withContext(Dispatchers.IO, block)
    }
}

// 模仿 Jetpack Compose 创建协程域
inline fun createCoroutineScope(
    getContext: () -> CoroutineContext = { EmptyCoroutineContext }
): CoroutineScope {
    return CoroutineScope(job + getContext())
}

fun endAllCoroutine() {
    // 结束所有子协程
    job.cancel()
}
