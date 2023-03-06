package org.unifiedpush.distributor.nextpush.api.provider

class NoProviderException(message: String) : Exception(message)
interface ApiProviderFactory {
    fun getProviderAndExecute(block: (ApiProvider, then: () -> Unit) -> Unit)
}
