package org.unifiedpush.distributor.nextpush.api.provider

interface ApiProviderFactory {
    fun getProviderAndExecute(block: (ApiProvider) -> Unit)
    fun destroyProvider()
}
