package mikataneko.interfaces

import com.microsoft.aad.msal4j.ITokenCacheAccessAspect
import com.microsoft.aad.msal4j.ITokenCacheAccessContext

abstract class TokenCacheAspect : ITokenCacheAccessAspect {
    override fun beforeCacheAccess(iTokenCacheAccessContext: ITokenCacheAccessContext?) {
        val data = loadCacheData()
        iTokenCacheAccessContext?.tokenCache()?.deserialize(data)
    }

    override fun afterCacheAccess(iTokenCacheAccessContext: ITokenCacheAccessContext?) {
        val data = iTokenCacheAccessContext?.tokenCache()?.serialize()
        data?.let { saveCacheData(it) }
    }

    abstract fun loadCacheData(): String
    abstract fun saveCacheData(data: String)
}
