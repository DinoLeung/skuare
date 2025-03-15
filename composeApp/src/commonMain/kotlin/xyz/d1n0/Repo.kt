package xyz.d1n0

import xyz.d1n0.model.Watch

interface Repo {
    fun setWatch(watch: Watch)
    fun getWatch(): Watch?
}

class RepoImpl: Repo {
    private var _watch: Watch? = null

    override fun setWatch(watch: Watch) {
        _watch = watch
    }

    override fun getWatch(): Watch? = _watch
}